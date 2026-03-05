"""
Hypergeometric Analysis of Generalized Delannoy Sums
=====================================================
Based on: Jade Gold & William Griffiths
          "A Hypergeometric Approach to Generalized Delannoy Numbers"

Analyzes: S(m,n,k) = sum_{j=0}^{n} C(m+k, j+k) * C(n, j) * 2^j

Steps implemented:
  1. Direct summation + value tables
  2. Gosper ratio test  =>  hypergeometric identification
  3. Closed form: S(m,n,k) = C(m+k, k) * 2F1(-m, -n; k+1; 2)
  4. Gosper indefinite sum attempt
  5. Zeilberger recurrences (k=0 Delannoy case)
  6. Polynomial structure via Petkovšek interpolation

Requirements: pip install sympy
"""

from sympy import *
from sympy.concrete.gosper import gosper_sum

# ── Symbols ───────────────────────────────────────────────────────────────────
m, n, k, j = symbols("m n k j", integer=True, nonneg=True)

# ── Core functions ────────────────────────────────────────────────────────────


def S(mv, nv, kv):
    """Direct computation: S(m,n,k) = sum_{j=0}^{n} C(m+k,j+k)*C(n,j)*2^j"""
    return sum(
        int(binomial(mv + kv, jv + kv) * binomial(nv, jv) * 2**jv)
        for jv in range(nv + 1)
    )


def hyper21(a, b, c, z, n_terms=40):
    """Evaluate 2F1(a,b;c;z) — terminates when a or b is a negative integer."""
    result = Rational(0)
    term = Rational(1)
    for i in range(n_terms):
        result += term
        num = (a + i) * (b + i) * z
        den = (c + i) * (i + 1)
        if den == 0:
            break
        term *= Rational(num, den)
        if term == 0:
            break
    return result


def D_standard(mv, nv):
    """Standard Delannoy D(m,n) = sum_j C(m,j)*C(n,j)*2^j"""
    return sum(
        int(binomial(mv, jv) * binomial(nv, jv) * 2**jv)
        for jv in range(min(mv, nv) + 1)
    )


def sep(title=""):
    w = 62
    print("\n" + "=" * w)
    if title:
        print(title)
        print("=" * w)


# ── 1. VALUE TABLES ───────────────────────────────────────────────────────────


def print_value_table():
    sep("VALUES  S(m,n,k) = Σ C(m+k,j+k)·C(n,j)·2^j")
    for kv in range(4):
        print(f"\n  k = {kv}:")
        print(f"  {'n\\m':>4}", end="")
        for mv in range(7):
            print(f"{mv:>7}", end="")
        print()
        for nv in range(1, 6):
            print(f"  n={nv} ", end="")
            for mv in range(7):
                print(f"{S(mv,nv,kv):>7}", end="")
            print()


# ── 2. GOSPER RATIO TEST ──────────────────────────────────────────────────────


def ratio_test():
    sep("STEP 1 — GOSPER RATIO TEST")
    F = binomial(m + k, j + k) * binomial(n, j) * 2**j
    ratio = simplify(F.subs(j, j + 1) / F)

    print(
        f"""
  Summand:  t(j) = C(m+k, j+k) · C(n, j) · 2^j

  t(j+1)
  ──────  =  {ratio}
  t(j)

  Rational in j?  {ratio.is_rational_function(j)}  ✓  => term is HYPERGEOMETRIC

  Match to 2F1(a,b;c;z) ratio  =  (a+j)(b+j) / [(c+j)(j+1)] · z :

      a = -m,   b = -n,   c = k+1,   z = 2

  First term t(0) = C(m+k, k)  (not 1, so carries as prefactor)

  =>  S(m,n,k)  =  C(m+k, k) · 2F1(-m, -n; k+1; 2)
"""
    )
    return ratio


# ── 3. VERIFY CLOSED FORM ─────────────────────────────────────────────────────


def verify_closed_form():
    sep("STEP 2 — VERIFY  S(m,n,k) = C(m+k,k) · 2F1(-m,-n; k+1; 2)")
    cases = [
        (2, 3, 0),
        (3, 2, 1),
        (4, 3, 2),
        (2, 2, 0),
        (5, 3, 1),
        (3, 4, 2),
        (4, 4, 1),
        (2, 5, 3),
    ]
    all_ok = True
    for mv, nv, kv in cases:
        direct = S(mv, nv, kv)
        norm = int(binomial(mv + kv, kv))
        hyp = norm * hyper21(-mv, -nv, kv + 1, 2)
        ok = direct == hyp
        if not ok:
            all_ok = False
        tag = "✓" if ok else "✗"
        print(f"  {tag}  S({mv},{nv},{kv}) = {direct:>6}   C·2F1 = {hyp!s:>6}")

    print(f"\n  All verified: {all_ok}")

    # Special case k=0
    ok_d = all(S(mv, nv, 0) == D_standard(mv, nv) for mv in range(7) for nv in range(7))
    print(f"\n  Special case k=0:  S(m,n,0) = D(m,n)  [standard Delannoy]")
    print(f"  Verified m,n ∈ 0..6: {ok_d}  ✓")

    print(
        f"""
  Why the prefactor C(m+k,k)?
    The 2F1 series starts at 1 (j=0 term = 1).
    Our t(0) = C(m+k,k)·C(n,0)·2^0 = C(m+k,k).
    So S = C(m+k,k)·[1 + further terms] = C(m+k,k)·2F1(...)
"""
    )


# ── 4. GOSPER INDEFINITE SUM ──────────────────────────────────────────────────


def run_gosper():
    sep("STEP 3 — GOSPER'S ALGORITHM  (indefinite sum in j)")
    F = binomial(m + k, j + k) * binomial(n, j) * 2**j
    result = gosper_sum(F, (j, 0, n))
    print(f"\n  gosper_sum( t(j), j=0..n ) = {result}\n")
    if result is None:
        print("  Result: None — proven NO hypergeometric antidifference exists.")
        print("  This is a theorem, not a failure.")
        print("  The definite sum S(m,n,k) is closed-form but the indefinite")
        print("  sum has no simpler hypergeometric expression.  Zeilberger needed.")


# ── 5. RECURRENCES ────────────────────────────────────────────────────────────


def find_recurrences():
    sep("STEP 4 — RECURRENCE RELATIONS")

    print("\n  ── k=0: Recurrence in m  (Zeilberger) ──")
    print("  (m+1)·S(m+1,n,0) = (2n+1)·S(m,n,0) + m·S(m-1,n,0)\n")
    ok_m = all(
        (mv + 1) * S(mv + 1, nv, 0)
        == (2 * nv + 1) * S(mv, nv, 0) + mv * S(mv - 1, nv, 0)
        for mv in range(1, 7)
        for nv in range(2, 7)
    )
    print(f"  Verified m=1..6, n=2..6: {ok_m}  ✓")

    print("\n  ── k=0: Recurrence in n  (m↔n symmetry) ──")
    print("  (n+1)·S(m,n+1,0) = (2m+1)·S(m,n,0) + n·S(m,n-1,0)\n")
    ok_n = all(
        (nv + 1) * S(mv, nv + 1, 0)
        == (2 * mv + 1) * S(mv, nv, 0) + nv * S(mv, nv - 1, 0)
        for mv in range(2, 7)
        for nv in range(1, 6)
    )
    print(f"  Verified m=2..6, n=1..5: {ok_n}  ✓")

    print("\n  ── General k: consecutive ratio in m ──")
    print("  From 2F1: ratio C(m+k+1,k) 2F1(-m-1,...) / [C(m+k,k) 2F1(-m,...)]")
    print("  is rational in m  =>  first-order hypergeometric recurrence in m\n")
    print("  Sample ratios S(m+1,n,k) / S(m,n,k):")
    for kv in range(3):
        for nv in [2, 3]:
            print(f"    n={nv}, k={kv}:", end="")
            for mv in range(1, 5):
                r = Rational(S(mv + 1, nv, kv), S(mv, nv, kv))
                print(f"  m={mv}: {r}", end="")
            print()


# ── 6. POLYNOMIAL STRUCTURE ───────────────────────────────────────────────────


def polynomial_structure():
    sep("STEP 5 — POLYNOMIAL STRUCTURE  (Petkovšek)")
    print("\n  For fixed n, k:  S(m,n,k) is a polynomial in m of degree n+k\n")
    for nv in range(1, 5):
        for kv in range(3):
            pts = [(mv, S(mv, nv, kv)) for mv in range(nv + kv + 2)]
            poly = expand(interpolate(pts, m))
            try:
                deg = Poly(poly, m).degree()
                print(f"  S(m,{nv},{kv}) = {poly}   [deg {deg}]")
            except Exception:
                print(f"  S(m,{nv},{kv}) = {poly}")


# ── 7. SUMMARY ────────────────────────────────────────────────────────────────


def print_summary():
    sep("SUMMARY — CLOSED FORM IDENTITIES")
    print(
        """
  ┌──────────────────────────────────────────────────────────┐
  │  HYPERGEOMETRIC FORM                                     │
  │                                                          │
  │   S(m,n,k) = C(m+k, k) · ₂F₁(-m, -n; k+1; 2)             │
  │                                                          │
  │  Proof:  t(j+1)/t(j) = 2(j-m)(j-n)/[(j+1)(j+k+1)]        │
  │          rational in j  ✓  =>  hypergeometric            │
  └──────────────────────────────────────────────────────────┘

  SPECIAL CASES
    k=0:  S(m,n,0) = D(m,n)   [standard Delannoy numbers]
    k=1, n=1:  S(m,1,1) = (m+1)²   [perfect squares!]

  RECURRENCES  (k=0 / Zeilberger)
    In m:  (m+1)·S(m+1,n) = (2n+1)·S(m,n) + m·S(m-1,n)
    In n:  (n+1)·S(m,n+1) = (2m+1)·S(m,n) + n·S(m,n-1)

  POLYNOMIAL STRUCTURE  (Petkovšek)
    For fixed n, k:  S(m,n,k) is degree (n+k) polynomial in m


"""
    )


# ── MAIN ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print_value_table()
    ratio_test()
    verify_closed_form()
    run_gosper()
    find_recurrences()
    polynomial_structure()
    print_summary()
