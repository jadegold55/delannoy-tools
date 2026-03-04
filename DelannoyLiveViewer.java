import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;

public class DelannoyLiveViewer {

    private final AtomicInteger renderGeneration = new AtomicInteger(0);

    private final ImagePanel imagePanel = new ImagePanel();
    private final JLabel statusLabel = new JLabel("Ready", SwingConstants.LEFT);

    private final JSlider sizeSlider = new JSlider(20, 300, 120);
    private final JSlider kSlider = new JSlider(0, 20, 1);
    private final JSlider pSlider = new JSlider(2, 20, 3);
    private final JSlider zoomSlider = new JSlider(1, 6, 2);

    private final JLabel sizeValue = new JLabel();
    private final JLabel kValue = new JLabel();
    private final JLabel pValue = new JLabel();
    private final JLabel zoomValue = new JLabel();

    private final JPanel zeroColorSwatch = new JPanel();
    private final JPanel nonZeroColorSwatch = new JPanel();
    private final JLabel zeroColorValue = new JLabel();
    private final JLabel nonZeroColorValue = new JLabel();

    private volatile Color zeroColor = Color.WHITE;
    private volatile Color nonZeroColor = Color.BLACK;

    private Timer debounceTimer;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DelannoyLiveViewer().createAndShow());
    }

    private void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("Delannoy Carpet - Live Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel controls = buildControlsPanel();
        JScrollPane imageScroll = new JScrollPane(imagePanel);
        imageScroll.getVerticalScrollBar().setUnitIncrement(16);
        imageScroll.getHorizontalScrollBar().setUnitIncrement(16);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, imageScroll);
        split.setResizeWeight(0.0);
        split.setDividerLocation(320);

        JPanel root = new JPanel(new BorderLayout());
        root.add(split, BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setSize(1100, 760);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        debounceTimer = new Timer(70, e -> renderAsync());
        debounceTimer.setRepeats(false);

        updateValueLabels();
        renderAsync();
    }

    private JPanel buildControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        sizeSlider.setPaintTicks(true);
        sizeSlider.setMajorTickSpacing(40);
        sizeSlider.setMinorTickSpacing(10);

        kSlider.setPaintTicks(true);
        kSlider.setMajorTickSpacing(5);
        kSlider.setMinorTickSpacing(1);

        pSlider.setPaintTicks(true);
        pSlider.setMajorTickSpacing(3);
        pSlider.setMinorTickSpacing(1);

        zoomSlider.setPaintTicks(true);
        zoomSlider.setMajorTickSpacing(1);
        zoomSlider.setSnapToTicks(true);

        ChangeListener onSlide = e -> {
            updateValueLabels();
            debounceTimer.restart();
        };

        sizeSlider.addChangeListener(onSlide);
        kSlider.addChangeListener(onSlide);
        pSlider.addChangeListener(onSlide);
        zoomSlider.addChangeListener(onSlide);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);

        addColorPicker(panel, gbc, "Color for value = 0", true);
        addColorPicker(panel, gbc, "Color for value != 0", false);
        addControl(panel, gbc, "Size (rows = cols)", sizeSlider, sizeValue);
        addControl(panel, gbc, "k", kSlider, kValue);
        addControl(panel, gbc, "p (mod)", pSlider, pValue);
        addControl(panel, gbc, "Zoom (pixel per cell)", zoomSlider, zoomValue);

        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        panel.setPreferredSize(new Dimension(320, 700));
        return panel;
    }

    private void addColorPicker(JPanel panel, GridBagConstraints gbc, String label, boolean isZeroColor) {
        JLabel title = new JLabel(label);
        title.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        panel.add(title, gbc);
        gbc.gridy++;

        JPanel row = new JPanel(new BorderLayout(8, 0));
        JPanel swatch = isZeroColor ? zeroColorSwatch : nonZeroColorSwatch;
        JLabel value = isZeroColor ? zeroColorValue : nonZeroColorValue;

        swatch.setPreferredSize(new Dimension(36, 24));
        swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        JButton pick = new JButton("Pick");
        pick.addActionListener(e -> chooseColor(panel, isZeroColor));

        row.add(swatch, BorderLayout.WEST);
        row.add(value, BorderLayout.CENTER);
        row.add(pick, BorderLayout.EAST);
        panel.add(row, gbc);
        gbc.gridy++;
    }

    private void addControl(JPanel panel, GridBagConstraints gbc, String label, JSlider slider, JLabel valueLabel) {
        JLabel title = new JLabel(label);
        title.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
        panel.add(title, gbc);
        gbc.gridy++;

        panel.add(slider, gbc);
        gbc.gridy++;

        valueLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(valueLabel, gbc);
        gbc.gridy++;
    }

    private void updateValueLabels() {
        sizeValue.setText(Integer.toString(sizeSlider.getValue()));
        kValue.setText(Integer.toString(kSlider.getValue()));
        pValue.setText(Integer.toString(pSlider.getValue()));
        zoomValue.setText(Integer.toString(zoomSlider.getValue()));

        zeroColorSwatch.setBackground(zeroColor);
        nonZeroColorSwatch.setBackground(nonZeroColor);
        zeroColorValue.setText(colorText(zeroColor));
        nonZeroColorValue.setText(colorText(nonZeroColor));
    }

    private void chooseColor(Component parent, boolean isZeroColor) {
        Color initial = isZeroColor ? zeroColor : nonZeroColor;
        Color chosen = JColorChooser.showDialog(parent, "Choose Color", initial);
        if (chosen == null) {
            return;
        }

        if (isZeroColor) {
            zeroColor = chosen;
        } else {
            nonZeroColor = chosen;
        }

        updateValueLabels();
        debounceTimer.restart();
    }

    private String colorText(Color color) {
        return String.format("RGB(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void renderAsync() {
        int size = sizeSlider.getValue();
        int k = kSlider.getValue();
        int mod = pSlider.getValue();
        int cell = zoomSlider.getValue();
        Color zero = zeroColor;
        Color nonZero = nonZeroColor;
        int thisGeneration = renderGeneration.incrementAndGet();
        long started = System.nanoTime();

        statusLabel.setText(String.format("Rendering... size=%d k=%d p=%d zoom=%d", size, k, mod, cell));

        new SwingWorker<BufferedImage, BufferedImage>() {
            @Override
            protected BufferedImage doInBackground() {
                int width = size * cell;
                int height = size * cell;
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                publish(image);

                int[] previousRow = new int[size];
                int[] currentRow = new int[size];
                BigInteger modBig = BigInteger.valueOf(mod);

                BigInteger firstCol = BigInteger.ONE;

                for (int col = 0; col < size; col++) {
                    previousRow[col] = 1 % mod;
                }

                for (int row = 0; row < size; row++) {
                    if (thisGeneration != renderGeneration.get()) {
                        return null;
                    }

                    if (row == 0) {
                        firstCol = BigInteger.ONE;
                    } else {
                        firstCol = firstCol.multiply(BigInteger.valueOf(row + k)).divide(BigInteger.valueOf(row));
                    }
                    currentRow[0] = firstCol.mod(modBig).intValue();

                    for (int col = 1; col < size; col++) {
                        int value = previousRow[col] + currentRow[col - 1] + previousRow[col - 1];
                        currentRow[col] = value % mod;
                    }

                    for (int col = 0; col < size; col++) {
                        paintCell(image, row, col, currentRow[col], cell, zero, nonZero);
                    }

                    int[] temp = previousRow;
                    previousRow = currentRow;
                    currentRow = temp;

                    if (row % 6 == 0 || row == size - 1) {
                        publish(image);
                    }
                }

                return image;
            }

            @Override
            protected void process(List<BufferedImage> chunks) {
                if (thisGeneration != renderGeneration.get() || chunks.isEmpty()) {
                    return;
                }
                imagePanel.setImage(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                if (thisGeneration != renderGeneration.get()) {
                    return;
                }
                try {
                    BufferedImage result = get();
                    if (result != null) {
                        imagePanel.setImage(result);
                        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
                        statusLabel.setText("Done in " + elapsedMs + " ms");
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Render failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private static void paintCell(BufferedImage image, int row, int col, int value, int cell, Color zeroColor,
            Color nonZeroColor) {
        int rgb = (value == 0 ? zeroColor : nonZeroColor).getRGB();

        int x0 = col * cell;
        int y0 = row * cell;
        for (int y = y0; y < y0 + cell; y++) {
            for (int x = x0; x < x0 + cell; x++) {
                image.setRGB(x, y, rgb);
            }
        }
    }

    private static class ImagePanel extends JPanel {
        private BufferedImage image;

        void setImage(BufferedImage image) {
            this.image = image;
            if (image != null) {
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            }
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, null);
            }
        }
    }
}