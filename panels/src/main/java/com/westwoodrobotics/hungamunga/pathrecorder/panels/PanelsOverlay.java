package com.westwoodrobotics.hungamunga.pathrecorder.panels;

import com.bylazar.field.FieldManager;
import com.bylazar.field.PanelsField;
import com.pedropathing.math.Pose;
import com.westwoodrobotics.hungamunga.pathrecorder.Sample;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.FitCurve;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.Vec2;
import com.westwoodrobotics.hungamunga.pathrecorder.ftc.RecorderOverlay;
import com.westwoodrobotics.hungamunga.pathrecorder.route.PathIR;
import com.westwoodrobotics.hungamunga.pathrecorder.route.RouteIR;
import java.util.List;

public final class PanelsOverlay implements RecorderOverlay {
    private static final int MAX_TRACE_LINES = 400;
    private static final int CURVE_STEPS = 16;
    private static final double ROBOT_RADIUS = 9;

    private final long intervalNanos;
    private long lastDraw = Long.MIN_VALUE;

    public PanelsOverlay() {
        this(100);
    }

    public PanelsOverlay(long intervalMs) {
        this.intervalNanos = intervalMs * 1_000_000L;
    }

    @Override
    public void draw(List<Sample> trace, RouteIR route, Pose pose) {
        long now = System.nanoTime();
        if (lastDraw != Long.MIN_VALUE && now - lastDraw < intervalNanos) return;
        lastDraw = now;
        FieldManager field = PanelsField.INSTANCE.getField();
        field.setOffsets(PanelsField.INSTANCE.getPresets().getPEDRO_PATHING());

        field.setStyle(PanelsField.INSTANCE.getTRANSPARENT(), "#9e9e9e", 0.5);
        int step = Math.max(1, trace.size() / MAX_TRACE_LINES);
        for (int i = step; i < trace.size(); i += step) {
            Sample a = trace.get(i - step);
            Sample b = trace.get(i);
            field.moveCursor(a.x, a.y);
            field.line(b.x, b.y);
        }

        if (route != null) {
            field.setStyle(PanelsField.INSTANCE.getTRANSPARENT(), "#2196f3", 1.0);
            for (PathIR path : route.paths) {
                for (FitCurve c : path.pieces) {
                    Vec2 prev = c.start();
                    for (int k = 1; k <= CURVE_STEPS; k++) {
                        Vec2 next = c.point((double) k / CURVE_STEPS);
                        field.moveCursor(prev.x, prev.y);
                        field.line(next.x, next.y);
                        prev = next;
                    }
                }
            }
        }

        field.setStyle(PanelsField.INSTANCE.getTRANSPARENT(), "#4caf50", 1.0);
        field.moveCursor(pose.x(), pose.y());
        field.circle(ROBOT_RADIUS);
        field.moveCursor(pose.x(), pose.y());
        field.line(pose.x() + ROBOT_RADIUS * Math.cos(pose.heading()), pose.y() + ROBOT_RADIUS * Math.sin(pose.heading()));
        field.update();
    }
}
