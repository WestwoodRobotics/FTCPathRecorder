package org.westwoodrobots.pathrecorder.route;

import org.westwoodrobots.pathrecorder.Sample;
import org.westwoodrobots.pathrecorder.fit.FitCurve;
import org.westwoodrobots.pathrecorder.fit.Vec2;
import java.util.List;

public final class HeadingClassifier {
    private final double tolerance;

    public HeadingClassifier(double toleranceRadians) {
        this.tolerance = toleranceRadians;
    }

    public Result classify(List<FitCurve> pieces, List<Sample> samples, double start, double end) {
        int n = samples.size();
        if (n == 0) return new Result(HeadingMode.LINEAR, 0, 0, true);
        double sin = 0, cos = 0;
        for (Sample s : samples) {
            sin += Math.sin(s.heading);
            cos += Math.cos(s.heading);
        }
        double mean = Math.atan2(sin, cos);
        double delta = Angles.error(start, end);
        double tangent = 0, reverse = 0, constant = 0, linear = 0;
        for (Sample s : samples) {
            CurvePosition p = CurvePosition.closest(pieces, new Vec2(s.x, s.y));
            double along = pieces.get(p.piece).derivative(p.t).angle();
            tangent += sq(Angles.error(along, s.heading));
            reverse += sq(Angles.error(along + Math.PI, s.heading));
            constant += sq(Angles.error(mean, s.heading));
            linear += sq(Angles.error(start + delta * p.completion, s.heading));
        }
        double[] rms = {Math.sqrt(tangent / n), Math.sqrt(reverse / n), Math.sqrt(constant / n), Math.sqrt(linear / n)};
        HeadingMode[] modes = HeadingMode.values();
        for (int i = 0; i < modes.length; i++) {
            if (rms[i] <= tolerance) return new Result(modes[i], mean, rms[i], true);
        }
        return new Result(HeadingMode.LINEAR, mean, rms[3], false);
    }

    private static double sq(double v) {
        return v * v;
    }

    public static final class Result {
        public final HeadingMode mode;
        public final double constant;
        public final double rms;
        public final boolean withinTolerance;

        Result(HeadingMode mode, double constant, double rms, boolean withinTolerance) {
            this.mode = mode;
            this.constant = constant;
            this.rms = rms;
            this.withinTolerance = withinTolerance;
        }
    }
}
