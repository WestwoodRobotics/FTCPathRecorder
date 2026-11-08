package org.westwoodrobots.pathrecorder;

import com.pedropathing.math.Pose;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SyntheticTrace {
    public enum Heading {
        TANGENT,
        REVERSE,
        HOLD
    }

    private interface Geometry {
        double[] at(double u);
    }

    private final Pose start;
    private final List<Sample> samples = new ArrayList<>();
    private final List<ActionEvent> actions = new ArrayList<>();
    private final List<Double> waypoints = new ArrayList<>();
    private final List<double[]> truth = new ArrayList<>();
    private final List<Geometry> pending = new ArrayList<>();
    private final List<double[]> pendingActions = new ArrayList<>();
    private final List<String> pendingNames = new ArrayList<>();
    private final List<Double> pendingWaypoints = new ArrayList<>();
    private double hz = 50;
    private double sigma = 0;
    private Random random = new Random(0);
    private double t = 0;
    private double x, y, h;

    private SyntheticTrace(double x, double y, double headingDeg) {
        this.x = x;
        this.y = y;
        this.h = Math.toRadians(headingDeg);
        this.start = new Pose(x, y, h);
    }

    public static SyntheticTrace start(double x, double y, double headingDeg) {
        return new SyntheticTrace(x, y, headingDeg);
    }

    public SyntheticTrace rate(double hz) {
        this.hz = hz;
        return this;
    }

    public SyntheticTrace noise(double sigma, long seed) {
        this.sigma = sigma;
        this.random = new Random(seed);
        return this;
    }

    public SyntheticTrace lineTo(final double ex, final double ey) {
        final double[] from = tail();
        pending.add(u -> new double[] {from[0] + (ex - from[0]) * u, from[1] + (ey - from[1]) * u});
        return this;
    }

    public SyntheticTrace arcAround(final double cx, final double cy, double sweepDeg) {
        final double[] from = tail();
        final double r = Math.hypot(from[0] - cx, from[1] - cy);
        final double a0 = Math.atan2(from[1] - cy, from[0] - cx);
        final double sweep = Math.toRadians(sweepDeg);
        pending.add(u -> new double[] {cx + r * Math.cos(a0 + sweep * u), cy + r * Math.sin(a0 + sweep * u)});
        return this;
    }

    public SyntheticTrace curveTo(double c1x, double c1y, double c2x, double c2y, double ex, double ey) {
        final double[] p0 = tail();
        final double[] p1 = {c1x, c1y}, p2 = {c2x, c2y}, p3 = {ex, ey};
        pending.add(u -> {
            double v = 1 - u;
            double a = v * v * v, b = 3 * v * v * u, c = 3 * v * u * u, d = u * u * u;
            return new double[] {a * p0[0] + b * p1[0] + c * p2[0] + d * p3[0], a * p0[1] + b * p1[1] + c * p2[1] + d * p3[1]};
        });
        return this;
    }

    public SyntheticTrace actionAt(double fraction, String expression) {
        pendingActions.add(new double[] {fraction});
        pendingNames.add(expression);
        return this;
    }

    public SyntheticTrace waypointAt(double fraction) {
        pendingWaypoints.add(fraction);
        return this;
    }

    public SyntheticTrace drive(Heading mode, double peakSpeed) {
        return drive(mode, Double.NaN, 0, peakSpeed);
    }

    public SyntheticTrace driveLinear(double endHeadingDeg, double peakSpeed) {
        return drive(null, Math.toRadians(endHeadingDeg), 0, peakSpeed);
    }

    public SyntheticTrace wiggle(double amplitudeDeg, double peakSpeed) {
        return drive(Heading.HOLD, Double.NaN, Math.toRadians(amplitudeDeg), peakSpeed);
    }

    private SyntheticTrace drive(Heading mode, double endHeading, double wiggle, double peakSpeed) {
        List<double[]> poly = new ArrayList<>();
        for (Geometry g : pending) {
            for (int i = poly.isEmpty() ? 0 : 1; i <= 400; i++) poly.add(g.at(i / 400.0));
        }
        double[] cum = new double[poly.size()];
        for (int i = 1; i < poly.size(); i++) {
            cum[i] = cum[i - 1] + Math.hypot(poly.get(i)[0] - poly.get(i - 1)[0], poly.get(i)[1] - poly.get(i - 1)[1]);
        }
        double length = cum[cum.length - 1];
        double duration = 1.5 * length / peakSpeed;
        double h0 = h;
        double dt = 1 / hz;
        int steps = (int) Math.ceil(duration / dt);
        double[] prev = poly.get(0);
        for (int k = 1; k <= steps; k++) {
            double tau = Math.min(1, k * dt / duration);
            double s = length * (3 * tau * tau - 2 * tau * tau * tau);
            double[] p = along(poly, cum, s);
            double[] q = along(poly, cum, Math.min(length, s + 1e-3));
            double[] q0 = along(poly, cum, Math.max(0, s - 1e-3));
            double tangent = Math.atan2(q[1] - q0[1], q[0] - q0[0]);
            double heading;
            if (mode == Heading.TANGENT) heading = tangent;
            else if (mode == Heading.REVERSE) heading = tangent + Math.PI;
            else if (mode == Heading.HOLD) heading = h0 + wiggle * Math.sin(6 * Math.PI * s / length);
            else heading = h0 + wrap(endHeading - h0) * (s / length);
            double speed = 6 * tau * (1 - tau) * length / duration;
            double dir = Math.atan2(p[1] - prev[1], p[0] - prev[0]);
            emit(p[0], p[1], heading, speed * Math.cos(dir), speed * Math.sin(dir), 0);
            prev = p;
            double before = (k - 1) * dt / duration;
            for (int i = 0; i < pendingActions.size(); i++) {
                double f = pendingActions.get(i)[0];
                double timeFraction = timeAtArcFraction(f);
                if (timeFraction > before && timeFraction <= tau) actions.add(new ActionEvent(t - (tau - timeFraction) * duration, pendingNames.get(i)));
            }
            for (double f : pendingWaypoints) {
                double timeFraction = timeAtArcFraction(f);
                if (timeFraction > before && timeFraction <= tau) waypoints.add(t);
            }
        }
        pending.clear();
        pendingActions.clear();
        pendingNames.clear();
        pendingWaypoints.clear();
        return this;
    }

    public SyntheticTrace pause(double ms) {
        int steps = (int) Math.round(ms / 1000 * hz);
        for (int k = 0; k < steps; k++) emit(x, y, h, 0, 0, 0);
        return this;
    }

    public SyntheticTrace turnTo(double headingDeg, double ms) {
        double h0 = h;
        double delta = wrap(Math.toRadians(headingDeg) - h0);
        int steps = Math.max(1, (int) Math.round(ms / 1000 * hz));
        for (int k = 1; k <= steps; k++) {
            double tau = (double) k / steps;
            emit(x, y, h0 + delta * (3 * tau * tau - 2 * tau * tau * tau), 0, 0, delta / (ms / 1000));
        }
        return this;
    }

    public SyntheticTrace action(String expression) {
        actions.add(new ActionEvent(t, expression));
        return this;
    }

    public Recording build() {
        if (samples.isEmpty()) emit(x, y, h, 0, 0, 0);
        return new Recording(start, samples, actions, waypoints);
    }

    public List<double[]> truth() {
        return truth;
    }

    private void emit(double px, double py, double heading, double vx, double vy, double omega) {
        t += 1 / hz;
        x = px;
        y = py;
        h = wrap(heading);
        truth.add(new double[] {px, py});
        double n = sigma;
        samples.add(new Sample(t, px + gauss(n), py + gauss(n), wrap(heading + gauss(Math.toRadians(n * 2))),
                vx + gauss(n), vy + gauss(n), omega));
    }

    private double gauss(double s) {
        return s == 0 ? 0 : random.nextGaussian() * s;
    }

    private double[] tail() {
        if (pending.isEmpty()) return new double[] {x, y};
        return pending.get(pending.size() - 1).at(1);
    }

    private static double timeAtArcFraction(double f) {
        double lo = 0, hi = 1;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (3 * mid * mid - 2 * mid * mid * mid < f) lo = mid;
            else hi = mid;
        }
        return (lo + hi) / 2;
    }

    private static double[] along(List<double[]> poly, double[] cum, double s) {
        int lo = 0, hi = cum.length - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) / 2;
            if (cum[mid] <= s) lo = mid;
            else hi = mid;
        }
        double seg = cum[hi] - cum[lo];
        double f = seg == 0 ? 0 : (s - cum[lo]) / seg;
        double[] a = poly.get(lo), b = poly.get(hi);
        return new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f};
    }

    private static double wrap(double a) {
        double r = a % (2 * Math.PI);
        if (r >= Math.PI) r -= 2 * Math.PI;
        if (r < -Math.PI) r += 2 * Math.PI;
        return r;
    }
}
