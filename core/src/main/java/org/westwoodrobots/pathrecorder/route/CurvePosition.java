package org.westwoodrobots.pathrecorder.route;

import org.westwoodrobots.pathrecorder.fit.FitCurve;
import org.westwoodrobots.pathrecorder.fit.Vec2;
import java.util.List;

public final class CurvePosition {
    public final int piece;
    public final double t;
    public final double completion;
    public final double distance;

    private CurvePosition(int piece, double t, double completion, double distance) {
        this.piece = piece;
        this.t = t;
        this.completion = completion;
        this.distance = distance;
    }

    public static CurvePosition closest(List<FitCurve> pieces, Vec2 q) {
        double total = 0;
        for (FitCurve c : pieces) total += c.length();
        int best = 0;
        double bestT = 0;
        double bestDist = Double.MAX_VALUE;
        double bestArc = 0;
        double before = 0;
        for (int i = 0; i < pieces.size(); i++) {
            FitCurve c = pieces.get(i);
            double t = c.closestParameter(q);
            double d = c.point(t).distance(q);
            if (d < bestDist) {
                bestDist = d;
                best = i;
                bestT = t;
                bestArc = before + c.arcLength(t);
            }
            before += c.length();
        }
        return new CurvePosition(best, bestT, total == 0 ? 0 : bestArc / total, bestDist);
    }
}
