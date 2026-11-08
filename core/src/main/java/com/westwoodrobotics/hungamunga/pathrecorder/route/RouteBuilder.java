package com.westwoodrobotics.hungamunga.pathrecorder.route;

import com.pedropathing.math.Pose;
import com.westwoodrobotics.hungamunga.pathrecorder.RecorderConfig;
import com.westwoodrobotics.hungamunga.pathrecorder.Recording;
import com.westwoodrobotics.hungamunga.pathrecorder.Sample;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.CurveFitter;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.FitCurve;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.Vec2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RouteBuilder {
    private static final double MIN_TURN_TIMEOUT = 500;

    private final RecorderConfig config;

    public RouteBuilder(RecorderConfig config) {
        this.config = config;
    }

    public RouteIR build(Recording recording) {
        List<Sample> s = recording.samples;
        ActionPlacer placer = new ActionPlacer(recording.actions, s);
        if (s.isEmpty()) {
            List<Step> steps = new ArrayList<>();
            for (String e : placer.all()) steps.add(Step.action(e));
            return finish(new RouteIR(Collections.singletonList(recording.start), Collections.<PathIR>emptyList(), steps));
        }
        List<Boundary> bounds = new Segmenter(config).split(s, recording.start, recording.waypoints);
        double headingTol = config.headingToleranceRadians();
        CurveFitter fitter = new CurveFitter(config.positionTolerance);
        HeadingClassifier classifier = new HeadingClassifier(headingTol);

        List<Pose> poses = new ArrayList<>();
        int[] entry = new int[bounds.size()];
        int[] exit = new int[bounds.size()];
        for (int k = 0; k < bounds.size(); k++) {
            Boundary b = bounds.get(k);
            entry[k] = poses.size();
            poses.add(new Pose(b.x, b.y, b.entryHeading));
            exit[k] = entry[k];
            if (b.turns(headingTol)) {
                exit[k] = poses.size();
                poses.add(new Pose(b.x, b.y, b.exitHeading));
            }
        }

        List<PathIR> paths = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        for (int k = 0; k < bounds.size(); k++) {
            Boundary b = bounds.get(k);
            if (k > 0) {
                Boundary a = bounds.get(k - 1);
                List<Sample> inner = new ArrayList<>(s.subList(a.last + 1, b.first));
                List<Vec2> pts = new ArrayList<>();
                pts.add(new Vec2(a.x, a.y));
                for (Sample q : inner) pts.add(new Vec2(q.x, q.y));
                pts.add(new Vec2(b.x, b.y));
                List<FitCurve> pieces = fitter.fit(pts);
                double maxError = 0;
                for (Vec2 q : pts) maxError = Math.max(maxError, CurvePosition.closest(pieces, q).distance);
                HeadingClassifier.Result h = classifier.classify(pieces, inner, a.exitHeading, b.entryHeading);
                paths.add(new PathIR(exit[k - 1], entry[k], pieces, h.mode, h.constant, h.rms, h.withinTolerance,
                        maxError, placer.during(a, b, pieces)));
                steps.add(Step.follow(paths.size() - 1));
            }
            boolean first = k == 0;
            boolean last = k == bounds.size() - 1;
            if (!b.turns(headingTol)) {
                for (String e : placer.atStop(b, first, last, true, true)) steps.add(Step.action(e));
                continue;
            }
            for (String e : placer.atStop(b, first, last, true, false)) steps.add(Step.action(e));
            steps.add(Step.turn(exit[k], Math.max(MIN_TURN_TIMEOUT, Math.ceil(b.turnDuration * 20) * 100)));
            for (String e : placer.atStop(b, first, last, false, true)) steps.add(Step.action(e));
        }
        return finish(new RouteIR(poses, paths, steps));
    }

    private RouteIR finish(RouteIR route) {
        return config.mirrored() ? route.mirrorX(config.mirrorAxis) : route;
    }
}
