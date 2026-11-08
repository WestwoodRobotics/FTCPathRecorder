package com.westwoodrobotics.hungamunga.pathrecorder.fit;

import java.util.ArrayList;
import java.util.List;

public final class CurveFitter {
    private static final int MAX_ITERATIONS = 20;
    private static final double FILTER_FRACTION = 0.25;

    private final double tolerance;

    public CurveFitter(double tolerance) {
        this.tolerance = tolerance;
    }

    public List<FitCurve> fit(List<Vec2> raw) {
        List<FitCurve> out = new ArrayList<>();
        List<Vec2> pts = dedupe(raw);
        if (pts.size() < 2) {
            Vec2 a = raw.get(0);
            out.add(FitCurve.line(a, raw.get(raw.size() - 1)));
            return out;
        }
        Vec2 first = pts.get(0);
        Vec2 last = pts.get(pts.size() - 1);
        if (maxLineDistance(pts, first, last) <= tolerance) {
            out.add(FitCurve.line(first, last));
            return out;
        }
        Vec2[] d = smooth(pts).toArray(new Vec2[0]);
        // fit the filtered points tighter so the raw points stay inside the full tolerance
        double error = tolerance * (1 - FILTER_FRACTION);
        List<Piece> pieces = new ArrayList<>();
        fitCubic(d, 0, d.length - 1, leftTangent(d, 0, d.length - 1), rightTangent(d, 0, d.length - 1), error, pieces);
        merge(d, pieces, error);
        for (Piece piece : pieces) out.add(piece.curve);
        return out;
    }

    static double maxLineDistance(List<Vec2> pts, Vec2 a, Vec2 b) {
        FitCurve line = FitCurve.line(a, b);
        double max = 0;
        for (Vec2 q : pts) max = Math.max(max, line.distance(q));
        return max;
    }

    private List<Vec2> dedupe(List<Vec2> raw) {
        double min = tolerance * 0.02;
        List<Vec2> out = new ArrayList<>();
        for (Vec2 q : raw) {
            if (out.isEmpty() || q.distance(out.get(out.size() - 1)) > min) out.add(q);
        }
        Vec2 end = raw.get(raw.size() - 1);
        if (out.size() > 1 && out.get(out.size() - 1) != end) out.set(out.size() - 1, end);
        return out;
    }

    private List<Vec2> smooth(List<Vec2> pts) {
        double limit = tolerance * FILTER_FRACTION;
        int n = pts.size();
        List<Vec2> out = new ArrayList<>(n);
        out.add(pts.get(0));
        for (int i = 1; i < n - 1; i++) {
            int w = Math.min(2, Math.min(i, n - 1 - i));
            Vec2 sum = new Vec2(0, 0);
            for (int k = -w; k <= w; k++) sum = sum.plus(pts.get(i + k));
            Vec2 avg = sum.times(1.0 / (2 * w + 1));
            Vec2 shift = avg.minus(pts.get(i));
            double m = shift.norm();
            out.add(m > limit ? pts.get(i).plus(shift.times(limit / m)) : avg);
        }
        out.add(pts.get(n - 1));
        return out;
    }

    private void fitCubic(Vec2[] d, int first, int last, Vec2 t1, Vec2 t2, double error, List<Piece> out) {
        if (last - first == 1) {
            double dist = d[last].distance(d[first]) / 3;
            out.add(new Piece(first, last, FitCurve.cubic(d[first], d[first].plus(t1.times(dist)),
                    d[last].plus(t2.times(dist)), d[last])));
            return;
        }
        Fit fit = fitSingle(d, first, last, t1, t2, error);
        if (fit.error <= error) {
            out.add(new Piece(first, last, fit.curve));
            return;
        }
        int split = Math.max(first + 1, Math.min(last - 1, fit.split));
        Vec2 center = centerTangent(d, first, split, last);
        fitCubic(d, first, split, t1, center, error, out);
        fitCubic(d, split, last, center.times(-1), t2, error, out);
    }

    private Fit fitSingle(Vec2[] d, int first, int last, Vec2 t1, Vec2 t2, double error) {
        double[] u = chordLength(d, first, last);
        FitCurve bez = generate(d, first, last, u, t1, t2);
        Fit fit = maxError(d, first, last, bez, u);
        if (fit.error <= error || fit.error > error * 4) return fit;
        for (int k = 0; k < MAX_ITERATIONS; k++) {
            for (int i = first; i <= last; i++) u[i - first] = bez.newton(u[i - first], d[i]);
            bez = generate(d, first, last, u, t1, t2);
            Fit next = maxError(d, first, last, bez, u);
            if (next.error < fit.error) fit = next;
            if (fit.error <= error) return fit;
        }
        return fit;
    }

    private void merge(Vec2[] d, List<Piece> pieces, double error) {
        boolean changed = true;
        while (changed && pieces.size() > 1) {
            changed = false;
            for (int i = 0; i + 1 < pieces.size(); i++) {
                Piece a = pieces.get(i);
                Piece b = pieces.get(i + 1);
                Vec2 t1 = tangentOf(a.curve, true);
                Vec2 t2 = tangentOf(b.curve, false);
                Fit fit = fitSingle(d, a.first, b.last, t1, t2, error);
                if (fit.error <= error) {
                    pieces.set(i, new Piece(a.first, b.last, fit.curve));
                    pieces.remove(i + 1);
                    changed = true;
                    break;
                }
            }
        }
    }

    private static Vec2 tangentOf(FitCurve c, boolean start) {
        List<Vec2> p = c.controlPoints();
        return start ? p.get(1).minus(p.get(0)).normalized() : p.get(2).minus(p.get(3)).normalized();
    }

    private static FitCurve generate(Vec2[] d, int first, int last, double[] u, Vec2 t1, Vec2 t2) {
        Vec2 p0 = d[first];
        Vec2 p3 = d[last];
        double c00 = 0, c01 = 0, c11 = 0, x0 = 0, x1 = 0;
        for (int i = first; i <= last; i++) {
            double s = u[i - first];
            double v = 1 - s;
            double b0 = v * v * v, b1 = 3 * v * v * s, b2 = 3 * v * s * s, b3 = s * s * s;
            Vec2 a0 = t1.times(b1);
            Vec2 a1 = t2.times(b2);
            c00 += a0.dot(a0);
            c01 += a0.dot(a1);
            c11 += a1.dot(a1);
            Vec2 tmp = d[i].minus(p0.times(b0 + b1).plus(p3.times(b2 + b3)));
            x0 += a0.dot(tmp);
            x1 += a1.dot(tmp);
        }
        double det = c00 * c11 - c01 * c01;
        double seg = p0.distance(p3);
        double alphaL = Math.abs(det) < 1e-12 ? 0 : (x0 * c11 - x1 * c01) / det;
        double alphaR = Math.abs(det) < 1e-12 ? 0 : (c00 * x1 - c01 * x0) / det;
        double eps = 1e-6 * seg;
        if (alphaL < eps || alphaR < eps) {
            alphaL = seg / 3;
            alphaR = seg / 3;
        }
        return FitCurve.cubic(p0, p0.plus(t1.times(alphaL)), p3.plus(t2.times(alphaR)), p3);
    }

    private static Fit maxError(Vec2[] d, int first, int last, FitCurve bez, double[] u) {
        double max = 0;
        int split = (first + last) / 2;
        for (int i = first + 1; i < last; i++) {
            double e = bez.point(u[i - first]).distance(d[i]);
            if (e > max) {
                max = e;
                split = i;
            }
        }
        return new Fit(bez, max, split);
    }

    private static double[] chordLength(Vec2[] d, int first, int last) {
        double[] u = new double[last - first + 1];
        for (int i = first + 1; i <= last; i++) u[i - first] = u[i - first - 1] + d[i].distance(d[i - 1]);
        double total = u[u.length - 1];
        for (int i = 1; i < u.length; i++) u[i] = total == 0 ? (double) i / (u.length - 1) : u[i] / total;
        return u;
    }

    private Vec2 leftTangent(Vec2[] d, int first, int last) {
        int k = indexAt(d, first, last, 1);
        return d[k].minus(d[first]).normalized();
    }

    private Vec2 rightTangent(Vec2[] d, int first, int last) {
        int k = indexAt(d, last, first, -1);
        return d[k].minus(d[last]).normalized();
    }

    private Vec2 centerTangent(Vec2[] d, int first, int split, int last) {
        int a = indexAt(d, split, first, -1);
        int b = indexAt(d, split, last, 1);
        Vec2 t = d[a].minus(d[b]).normalized();
        return t.norm() == 0 ? d[split - 1].minus(d[split + 1]).normalized() : t;
    }

    // walks from 'from' toward 'limit' until the tangent window length is reached
    private int indexAt(Vec2[] d, int from, int limit, int dir) {
        double total = 0;
        for (int i = Math.min(from, limit) + 1; i <= Math.max(from, limit); i++) total += d[i].distance(d[i - 1]);
        double window = Math.min(2 * tolerance, total / 3);
        double acc = 0;
        int i = from;
        while (i != limit) {
            int next = i + dir;
            acc += d[next].distance(d[i]);
            i = next;
            if (acc >= window) break;
        }
        return i;
    }

    private static final class Fit {
        final FitCurve curve;
        final double error;
        final int split;

        Fit(FitCurve curve, double error, int split) {
            this.curve = curve;
            this.error = error;
            this.split = split;
        }
    }

    private static final class Piece {
        final int first;
        final int last;
        final FitCurve curve;

        Piece(int first, int last, FitCurve curve) {
            this.first = first;
            this.last = last;
            this.curve = curve;
        }
    }
}
