package org.westwoodrobots.pathrecorder.route;

import org.westwoodrobots.pathrecorder.ActionEvent;
import org.westwoodrobots.pathrecorder.Sample;
import org.westwoodrobots.pathrecorder.fit.FitCurve;
import org.westwoodrobots.pathrecorder.fit.Vec2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ActionPlacer {
    private final List<ActionEvent> actions;
    private final List<Sample> samples;

    public ActionPlacer(List<ActionEvent> actions, List<Sample> samples) {
        List<ActionEvent> sorted = new ArrayList<>(actions);
        Collections.sort(sorted, (x, y) -> Double.compare(x.t, y.t));
        this.actions = sorted;
        this.samples = samples;
    }

    public List<PlacedAction> during(Boundary from, Boundary to, List<FitCurve> pieces) {
        List<PlacedAction> out = new ArrayList<>();
        for (ActionEvent e : actions) {
            if (e.t <= from.endTime || e.t >= to.startTime) continue;
            CurvePosition p = CurvePosition.closest(pieces, positionAt(from.last, to.first, e.t));
            out.add(new PlacedAction(e.expression, p.piece, p.t, p.completion));
        }
        return out;
    }

    public List<String> atStop(Boundary b, boolean first, boolean last, boolean beforeTurn, boolean afterTurn) {
        List<String> out = new ArrayList<>();
        for (ActionEvent e : actions) {
            boolean inside = (first && e.t <= b.endTime)
                    || (last && e.t >= b.startTime)
                    || (e.t >= b.startTime && e.t <= b.endTime);
            if (!inside) continue;
            boolean early = e.t < b.turnTime;
            if ((early && beforeTurn) || (!early && afterTurn)) out.add(e.expression);
        }
        return out;
    }

    public List<String> all() {
        List<String> out = new ArrayList<>();
        for (ActionEvent e : actions) out.add(e.expression);
        return out;
    }

    private Vec2 positionAt(int from, int to, double t) {
        for (int i = from; i < to; i++) {
            Sample a = samples.get(i);
            Sample b = samples.get(i + 1);
            if (t >= a.t && t <= b.t) {
                double f = b.t == a.t ? 0 : (t - a.t) / (b.t - a.t);
                return new Vec2(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f);
            }
        }
        Sample c = samples.get(to);
        return new Vec2(c.x, c.y);
    }
}
