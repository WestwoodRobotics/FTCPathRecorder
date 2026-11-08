package com.westwoodrobotics.hungamunga.pathrecorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.westwoodrobotics.hungamunga.pathrecorder.fit.CurveFitter;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.FitCurve;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.Vec2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CurveFitterTest {
    private static final double TOL = 1.0;

    @Test
    void straightLineBecomesLine() {
        List<Vec2> pts = noisy(line(10, 10, 70, 40, 200), 0.1, 1);
        List<FitCurve> fit = new CurveFitter(TOL).fit(pts);
        assertEquals(1, fit.size());
        assertTrue(fit.get(0).isLine());
        assertInside(fit, pts, TOL);
    }

    @Test
    void quarterArcIsOneCubic() {
        List<Vec2> pts = arc(72, 72, 30, 0, 90, 300);
        List<FitCurve> fit = new CurveFitter(TOL).fit(pts);
        assertEquals(1, fit.size());
        assertInside(fit, pts, TOL);
    }

    @Test
    void halfCircleIsOneCubic() {
        List<Vec2> pts = arc(72, 72, 24, -90, 90, 300);
        List<FitCurve> fit = new CurveFitter(TOL).fit(pts);
        assertEquals(1, fit.size());
        assertInside(fit, pts, TOL);
    }

    @Test
    void sCurveUsesAtMostTwoPieces() {
        List<Vec2> pts = arc(40, 72, 20, 180, 90, 200);
        List<Vec2> second = arc(40, 112, 20, -90, 0, 200);
        pts.addAll(second.subList(1, second.size()));
        List<FitCurve> fit = new CurveFitter(TOL).fit(pts);
        assertTrue(fit.size() <= 2, "pieces " + fit.size());
        assertInside(fit, pts, TOL);
        assertG1(fit);
    }

    @Test
    void fullCircleSplitsAndStaysG1() {
        List<Vec2> pts = arc(72, 72, 20, 0, 350, 600);
        List<FitCurve> fit = new CurveFitter(TOL).fit(pts);
        assertTrue(fit.size() >= 2 && fit.size() <= 4, "pieces " + fit.size());
        assertInside(fit, pts, TOL);
        assertG1(fit);
    }

    @Test
    void noisyArcStaysInsideToleranceOfTruthAndRaw() {
        List<Vec2> truth = arc(72, 72, 30, 0, 120, 400);
        List<Vec2> raw = noisy(truth, 0.15, 7);
        List<FitCurve> fit = new CurveFitter(TOL).fit(raw);
        assertTrue(fit.size() <= 2, "pieces " + fit.size());
        assertInside(fit, raw, TOL);
        assertInside(fit, truth, TOL);
    }

    @Test
    void tighterToleranceNeedsMorePieces() {
        List<Vec2> pts = arc(72, 72, 20, 0, 350, 600);
        int loose = new CurveFitter(1.0).fit(pts).size();
        List<FitCurve> tight = new CurveFitter(0.05).fit(pts);
        assertTrue(tight.size() > loose);
        assertInside(tight, pts, 0.05);
    }

    static void assertInside(List<FitCurve> fit, List<Vec2> pts, double tol) {
        for (Vec2 q : pts) {
            double best = Double.MAX_VALUE;
            for (FitCurve c : fit) best = Math.min(best, c.distance(q));
            assertTrue(best <= tol + 1e-9, "point off by " + best);
        }
    }

    static void assertG1(List<FitCurve> fit) {
        for (int i = 0; i + 1 < fit.size(); i++) {
            Vec2 a = fit.get(i).derivative(1).normalized();
            Vec2 b = fit.get(i + 1).derivative(0).normalized();
            assertEquals(1.0, a.dot(b), 1e-9, "join " + i);
            assertEquals(0.0, fit.get(i).end().distance(fit.get(i + 1).start()), 1e-9);
        }
    }

    static List<Vec2> line(double x0, double y0, double x1, double y1, int n) {
        List<Vec2> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double u = i / (n - 1.0);
            out.add(new Vec2(x0 + (x1 - x0) * u, y0 + (y1 - y0) * u));
        }
        return out;
    }

    static List<Vec2> arc(double cx, double cy, double r, double fromDeg, double toDeg, int n) {
        List<Vec2> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * i / (n - 1.0));
            out.add(new Vec2(cx + r * Math.cos(a), cy + r * Math.sin(a)));
        }
        return out;
    }

    static List<Vec2> noisy(List<Vec2> in, double sigma, long seed) {
        Random r = new Random(seed);
        List<Vec2> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            Vec2 q = in.get(i);
            boolean end = i == 0 || i == in.size() - 1;
            out.add(end ? q : new Vec2(q.x + r.nextGaussian() * sigma, q.y + r.nextGaussian() * sigma));
        }
        return out;
    }
}
