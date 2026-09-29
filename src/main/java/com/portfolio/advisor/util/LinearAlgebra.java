package com.portfolio.advisor.util;

/** Minimal dense linear algebra for the regression model (no external libraries). */
public final class LinearAlgebra {

    private LinearAlgebra() { }

    /** Solves A x = b by Gaussian elimination with partial pivoting. A and b are not modified. */
    public static double[] solve(double[][] a, double[] b) {
        int n = b.length;
        double[][] m = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            if (a[i].length != n) throw new IllegalArgumentException("matrix must be square");
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n] = b[i];
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) pivot = r;
            }
            if (Math.abs(m[pivot][col]) < 1e-12) throw new ArithmeticException("matrix is singular");
            double[] tmp = m[col]; m[col] = m[pivot]; m[pivot] = tmp;
            for (int r = col + 1; r < n; r++) {
                double factor = m[r][col] / m[col][col];
                for (int c = col; c <= n; c++) m[r][c] -= factor * m[col][c];
            }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = m[i][n];
            for (int j = i + 1; j < n; j++) sum -= m[i][j] * x[j];
            x[i] = sum / m[i][i];
        }
        return x;
    }

    public static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
