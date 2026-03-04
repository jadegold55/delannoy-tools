import java.math.BigInteger;
import java.util.*;
import java.io.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class DelannoyMatrixMenu {

    private static BigInteger[][] lastGeneratedMatrix = null;
    private static int lastK = -1;
    private static Integer lastMod = null;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        File numbersDir = new File("numbers");
        if (!numbersDir.exists())
            numbersDir.mkdir();

        BigInteger[][][] precomputed = new BigInteger[11][][];
        for (int k = 0; k <= 10; k++) {
            precomputed[k] = buildMatrix(10, 10, k);
        }

        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Generate an array");
            System.out.println("2. Generate an array with mod");
            System.out.println("3. See a precomputed array (k = 0 to 10)");
            System.out.println("4. Export last generated array to file");
            System.out.println("5. Import array from file");
            System.out.println("6. List saved files");
            System.out.println("7. Export last generated array as PNG carpet");
            System.out.println("8. End");
            System.out.print("Choose an option (1-8): ");

            int choice = getIntInput(sc, 1, 8);
            if (choice == 8)
                break;

            if (choice == 1 || choice == 2) {
                System.out.print("Enter number of rows: ");
                int m = getPositiveInt(sc);
                System.out.print("Enter number of columns: ");
                int n = getPositiveInt(sc);
                System.out.print("Enter value for k: ");
                int k = getPositiveInt(sc);
                lastK = k;

                BigInteger[][] matrix = buildMatrix(m, n, k);

                if (choice == 2) {
                    System.out.print("Enter value for mod (p): ");
                    int p = getPositiveInt(sc);
                    matrix = applyMod(matrix, p);
                    lastMod = p;
                } else {
                    lastMod = null;
                }

                lastGeneratedMatrix = matrix;
                printMatrix(matrix);

            } else if (choice == 3) {
                System.out.print("Enter k value (0-10): ");
                int k = getIntInput(sc, 0, 10);
                printMatrix(precomputed[k]);

            } else if (choice == 4) {
                if (lastGeneratedMatrix == null || lastK == -1) {
                    System.out.println("No matrix has been generated yet.");
                    continue;
                }
                System.out.print("Enter format (txt or csv): ");
                String format = sc.next();
                if (!format.equalsIgnoreCase("txt") && !format.equalsIgnoreCase("csv")) {
                    System.out.println("Invalid format. Only 'txt' and 'csv' are supported.");
                    continue;
                }
                exportMatrixToFile(lastGeneratedMatrix, format, lastGeneratedMatrix.length,
                        lastGeneratedMatrix[0].length, lastK, lastMod);

            } else if (choice == 5) {
                System.out.print("Enter filename to import (with extension): ");
                String filename = sc.next();
                try {
                    BigInteger[][] matrix = importMatrixFromFile("numbers/" + filename);
                    lastGeneratedMatrix = matrix;
                    System.out.println("Matrix successfully imported from " + filename);
                    printMatrix(matrix);
                } catch (Exception e) {
                    System.out.println("Failed to import matrix: " + e.getMessage());
                }

            } else if (choice == 6) {
                File[] files = numbersDir.listFiles();
                if (files == null || files.length == 0) {
                    System.out.println("No files saved yet in 'numbers' folder.");
                } else {
                    System.out.println("Saved files:");
                    for (File file : files) {
                        System.out.println("- " + file.getName());
                    }
                }
            } else if (choice == 7) {
                if (lastGeneratedMatrix == null) {
                    System.out.println("Generate or import an array first.");
                    continue;
                }

                System.out.print("Enter cell size in pixels (e.g., 2): ");
                int cellSize = getPositiveInt(sc);
                if (cellSize <= 0) {
                    System.out.println("Cell size must be greater than 0.");
                    continue;
                }

                String kPart = (lastK >= 0) ? ("k" + lastK) : "imported";
                String modPart = (lastMod != null) ? ("_mod" + lastMod) : "";
                String filename = String.format(
                        "numbers/delannoy_%dx%d_%s%s_carpet_bw.png",
                        lastGeneratedMatrix.length,
                        lastGeneratedMatrix[0].length,
                        kPart,
                        modPart);

                try {
                    exportMatrixToPng(lastGeneratedMatrix, cellSize, filename);
                    System.out.println("PNG carpet exported to " + filename);
                } catch (IOException e) {
                    System.out.println("Error writing PNG: " + e.getMessage());
                }
            }
        }
        sc.close();
    }

    public static int getPositiveInt(Scanner sc) {
        while (true) {
            try {
                int num = sc.nextInt();
                if (num >= 0)
                    return num;
                System.out.print("Please enter a non-negative integer: ");
            } catch (InputMismatchException e) {
                System.out.print("Sorry, that's not a valid input. Try again with an integer: ");
                sc.next();
            }
        }
    }

    public static int getIntInput(Scanner sc, int min, int max) {
        while (true) {
            try {
                int input = sc.nextInt();
                if (input >= min && input <= max)
                    return input;
                System.out
                        .print("Sorry, that's not a valid input. Try an integer between " + min + " and " + max + ": ");
            } catch (InputMismatchException e) {
                System.out.print("Sorry, that's not a valid input. Try again with an integer: ");
                sc.next();
            }
        }
    }

    public static BigInteger binomial(int n, int k) {
        if (k < 0 || k > n)
            return BigInteger.ZERO;
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= k; i++) {
            result = result.multiply(BigInteger.valueOf(n - i + 1));
            result = result.divide(BigInteger.valueOf(i));
        }
        return result;
    }

    public static BigInteger[][] buildMatrix(int m, int n, int k) {
        BigInteger[][] matrix = new BigInteger[m][n];
        for (int j = 0; j < n; j++)
            matrix[0][j] = BigInteger.ONE;
        for (int i = 0; i < m; i++)
            matrix[i][0] = binomial(i + k, k);
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                matrix[i][j] = matrix[i - 1][j].add(matrix[i][j - 1]).add(matrix[i - 1][j - 1]);
            }
        }
        return matrix;
    }

    public static BigInteger[][] applyMod(BigInteger[][] matrix, int mod) {
        int m = matrix.length, n = matrix[0].length;
        BigInteger[][] result = new BigInteger[m][n];
        BigInteger modVal = BigInteger.valueOf(mod);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = matrix[i][j].mod(modVal);
            }
        }
        return result;
    }

    public static void printMatrix(BigInteger[][] matrix) {
        int maxWidth = Arrays.stream(matrix)
                .flatMap(Arrays::stream)
                .mapToInt(val -> val.toString().length())
                .max().orElse(1);

        StringBuilder pad = new StringBuilder();
        for (int s = 0; s < maxWidth + 3; s++)
            pad.append(' ');
        System.out.print(pad.toString());
        for (int j = 0; j < matrix[0].length; j++) {
            System.out.printf("%" + (maxWidth + 2) + "d", j);
        }
        System.out.println();

        for (int i = 0; i < matrix.length; i++) {
            System.out.printf("%" + (maxWidth + 2) + "d ", i);
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%" + (maxWidth + 2) + "s", matrix[i][j]);
            }
            System.out.println();
        }
    }

    public static void exportMatrixToFile(BigInteger[][] matrix, String format, int rows, int cols, int k,
            Integer modValue) {
        String modPart = (modValue != null) ? ("_mod" + modValue) : "";
        String filename = String.format("numbers/delannoy_%dx%d_k%d%s.%s", rows, cols, k, modPart,
                format.toLowerCase());
        File file = new File(filename);

        if (file.exists()) {
            Scanner confirm = new Scanner(System.in);
            while (true) {
                System.out.print("File already exists. Overwrite? (yes/no): ");
                String response = confirm.nextLine().trim().toLowerCase();
                if (response.equals("yes") || response.equals("y"))
                    break;
                else if (response.equals("no") || response.equals("n")) {
                    System.out.println("Export cancelled.");
                    return;
                } else {
                    System.out.println("Invalid input. Please enter 'yes'/'y' or 'no'/'n'.");
                }
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write((format.equals("csv") ? "," : "\t"));
            for (int j = 0; j < cols; j++) {
                writer.write(j + (j < cols - 1 ? (format.equals("csv") ? "," : "\t") : ""));
            }
            writer.write("\n");

            for (int i = 0; i < rows; i++) {
                writer.write(i + (format.equals("csv") ? "," : "\t"));
                for (int j = 0; j < cols; j++) {
                    writer.write(matrix[i][j].toString());
                    if (j < cols - 1)
                        writer.write(format.equals("csv") ? "," : "\t");
                }
                writer.write("\n");
            }

            System.out.println("Matrix exported to " + filename);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    public static BigInteger[][] importMatrixFromFile(String filename) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(new File(filename));
        boolean isCSV = filename.endsWith(".csv");
        String delimiter = isCSV ? "," : "\t";

        String[] colHeaders = fileScanner.nextLine().split(delimiter);
        int cols = colHeaders.length - 1;
        List<BigInteger[]> rows = new ArrayList<>();

        while (fileScanner.hasNextLine()) {
            String[] parts = fileScanner.nextLine().split(delimiter);
            BigInteger[] row = new BigInteger[cols];
            for (int j = 0; j < cols; j++) {
                row[j] = new BigInteger(parts[j + 1]);
            }
            rows.add(row);
        }
        fileScanner.close();
        return rows.toArray(new BigInteger[0][0]);
    }

    public static void exportMatrixToPng(BigInteger[][] matrix, int cellSize, String filename)
            throws IOException {
        int rows = matrix.length;
        int cols = matrix[0].length;

        int width = cols * cellSize;
        int height = rows * cellSize;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                g2d.setColor(matrix[i][j].equals(BigInteger.ZERO) ? Color.WHITE : Color.BLACK);
                g2d.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
            }
        }

        g2d.dispose();
        ImageIO.write(image, "png", new File(filename));
    }
}
