package com.westwoodrobotics.hungamunga.pathrecorder.fit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FitCurve {
    private static final int TABLE = 256;

    private final Vec2[] p;
    private final double[] arc;

    private FitCurve(Vec2... points) {
        this.p = points;
        this.arc = new double[TABLE + 1];
        Vec2 prev = point(0);
        for (int i = 1; i <= TABLE; i++) {
            Vec2 next = point((double) i / TABLE);
            arc[i] = arc[i - 1] + next.distance(prev);
            prev = next;
        }
    }

    public static FitCurve line(Vec2 a, Vec2 b) {
        return new FitCurve(a, b);
    }

    public static FitCurve cubic(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3) {
        return new FitCurve(p0, p1, p2, p3);
    }

    public boolean isLine() {
        return p.length == 2;
    }

    public List<Vec2> controlPoints() {
        return Collections.unmodifiableList(Arrays.asList(p));
    }

    public Vec2 start() {
        return p[0];
    }

    public Vec2 end() {
        return p[p.length - 1];
    }

    public Vec2 point(double u) {
        if (isLine()) return p[0].plus(p[1].minus(p[0]).times(u));
        double v = 1 - u;
        return p[0].times(v * v * v)
                .plus(p[1].times(3 * v * v * u))
                .plus(p[2].times(3 * v * u * u))
                .plus(p[3].times(u * u * u));
    }

    public Vec2 derivative(double u) {
        if (isLine()) return p[1].minus(p[0]);
        double v = 1 - u;
        return p[1].minus(p[0]).times(3 * v * v)
                .plus(p[2].minus(p[1]).times(6 * v * u))
                .plus(p[3].minus(p[2]).times(3 * u * u));
    }

    public Vec2 secondDerivative(double u) {
        if (isLine()) return new Vec2(0, 0);
        return p[2].minus(p[1].times(2)).plus(p[0]).times(6 * (1 - u))
                .plus(p[3].minus(p[2].times(2)).plus(p[1]).times(6 * u));
    }

    public double length() {
        return arc[TABLE];
    }

    public double arcLength(double u) {
        double f = Math.max(0, Math.min(1, u)) * TABLE;
        int i = (int) Math.floor(f);
        if (i >= TABLE) return arc[TABLE];
        return arc[i] + (arc[i + 1] - arc[i]) * (f - i);
    }

    public double closestParameter(Vec2 q) {
        double best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i <= 64; i++) {
            double u = i / 64.0;
            double d = point(u).distance(q);
            if (d < bestDist) {
                bestDist = d;
                best = u;
            }
        }
        for (int k = 0; k < 8; k++) best = newton(best, q);
        return best;
    }

    public double distance(Vec2 q) {
        return point(closestParameter(q)).distance(q);
    }

    double newton(double u, Vec2 q) {
        Vec2 d = point(u).minus(q);
        Vec2 d1 = derivative(u);
        Vec2 d2 = secondDerivative(u);
        double den = d1.dot(d1) + d.dot(d2);
        if (Math.abs(den) < 1e-12) return u;
        return Math.max(0, Math.min(1, u - d.dot(d1) / den));
    }

    public FitCurve map(PointOperator op) {
        Vec2[] q = new Vec2[p.length];
        for (int i = 0; i < p.length; i++) q[i] = op.apply(p[i]);
        return new FitCurve(q);
    }

    public interface PointOperator {
        Vec2 apply(Vec2 v);
    }
}
