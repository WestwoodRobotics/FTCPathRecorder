package org.westwoodrobots.pathrecorder.route;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;
import org.westwoodrobots.pathrecorder.fit.FitCurve;
import org.westwoodrobots.pathrecorder.fit.Vec2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RouteIR {
    public final List<Pose> poses;
    public final List<PathIR> paths;
    public final List<Step> steps;

    public RouteIR(List<Pose> poses, List<PathIR> paths, List<Step> steps) {
        this.poses = Collections.unmodifiableList(new ArrayList<>(poses));
        this.paths = Collections.unmodifiableList(new ArrayList<>(paths));
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public int segmentCount() {
        return paths.size();
    }

    public int curveCount() {
        int n = 0;
        for (PathIR p : paths) n += p.pieces.size();
        return n;
    }

    public double maxFitError() {
        double max = 0;
        for (PathIR p : paths) max = Math.max(max, p.maxFitError);
        return max;
    }

    public RouteIR mirrorX(double axis) {
        final PoseFactory mirror = PoseFactory.radians().mirrorX(axis);
        List<Pose> mirroredPoses = new ArrayList<>();
        for (Pose p : poses) mirroredPoses.add(mirror.of(p.x(), p.y(), p.heading()));
        List<PathIR> mirroredPaths = new ArrayList<>();
        for (PathIR p : paths) {
            List<FitCurve> pieces = new ArrayList<>();
            for (FitCurve c : p.pieces) {
                pieces.add(c.map(v -> {
                    Pose m = mirror.of(v.x, v.y, 0);
                    return new Vec2(m.x(), m.y());
                }));
            }
            double constant = mirror.of(0, 0, p.constantHeading).heading();
            mirroredPaths.add(new PathIR(p.startPose, p.endPose, pieces, p.heading, constant, p.headingRms,
                    p.headingWithinTolerance, p.maxFitError, p.actions));
        }
        return new RouteIR(mirroredPoses, mirroredPaths, steps);
    }
}
