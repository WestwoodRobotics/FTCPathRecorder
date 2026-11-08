package com.westwoodrobotics.hungamunga.pathrecorder.route;

import com.westwoodrobotics.hungamunga.pathrecorder.fit.FitCurve;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PathIR {
    public final int startPose;
    public final int endPose;
    public final List<FitCurve> pieces;
    public final HeadingMode heading;
    public final double constantHeading;
    public final double headingRms;
    public final boolean headingWithinTolerance;
    public final double maxFitError;
    public final List<PlacedAction> actions;

    public PathIR(int startPose, int endPose, List<FitCurve> pieces, HeadingMode heading, double constantHeading,
                  double headingRms, boolean headingWithinTolerance, double maxFitError, List<PlacedAction> actions) {
        this.startPose = startPose;
        this.endPose = endPose;
        this.pieces = Collections.unmodifiableList(new ArrayList<>(pieces));
        this.heading = heading;
        this.constantHeading = constantHeading;
        this.headingRms = headingRms;
        this.headingWithinTolerance = headingWithinTolerance;
        this.maxFitError = maxFitError;
        this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
    }
}
