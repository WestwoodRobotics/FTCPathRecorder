package com.westwoodrobotics.hungamunga.pathrecorder.route;

import com.pedropathing.math.Pose;
import com.westwoodrobotics.hungamunga.pathrecorder.RecorderConfig;
import com.westwoodrobotics.hungamunga.pathrecorder.Sample;
import java.util.ArrayList;
import java.util.List;

public final class Segmenter {
    private final RecorderConfig config;

    public Segmenter(RecorderConfig config) {
        this.config = config;
    }

    public List<Boundary> split(List<Sample> s, Pose start, List<Double> waypoints) {
        int n = s.size();
        List<int[]> runs = stationaryRuns(s);
        List<int[]> stops = new ArrayList<>();
        double headingTol = config.headingToleranceRadians();
        for (int[] r : runs) {
            double duration = (s.get(r[1]).t - s.get(r[0]).t) * 1000;
            double turn = Math.abs(Angles.error(s.get(r[0]).heading, s.get(r[1]).heading));
            if (r[0] == 0 || r[1] == n - 1 || duration >= config.minStopTime || turn > headingTol) stops.add(r);
        }
        if (stops.isEmpty() || stops.get(0)[0] != 0) stops.add(0, new int[] {0, 0});
        if (stops.get(stops.size() - 1)[1] != n - 1) stops.add(new int[] {n - 1, n - 1});
        stops = mergeClose(s, stops);

        List<Boundary> out = new ArrayList<>();
        for (int k = 0; k < stops.size(); k++) {
            int[] r = stops.get(k);
            out.add(stop(s, r[0], r[1]));
            if (k + 1 < stops.size()) {
                double from = s.get(r[1]).t;
                double to = s.get(stops.get(k + 1)[0]).t;
                for (double w : sorted(waypoints)) {
                    if (w <= from || w >= to) continue;
                    int i = nearest(s, w, r[1] + 1, stops.get(k + 1)[0] - 1);
                    if (i > out.get(out.size() - 1).last) out.add(pass(s, i));
                }
            }
        }
        Boundary b0 = out.get(0);
        out.set(0, new Boundary(b0.first, b0.last, start.x(), start.y(), start.heading(), b0.exitHeading,
                b0.startTime, b0.endTime, true, b0.turnTime, b0.turnDuration));
        return out;
    }

    private List<int[]> stationaryRuns(List<Sample> s) {
        List<int[]> runs = new ArrayList<>();
        int begin = -1;
        for (int i = 0; i < s.size(); i++) {
            boolean still = speed(s, i) < config.stopSpeed;
            if (still && begin < 0) begin = i;
            if (!still && begin >= 0) {
                runs.add(new int[] {begin, i - 1});
                begin = -1;
            }
        }
        if (begin >= 0) runs.add(new int[] {begin, s.size() - 1});
        return runs;
    }

    private static double speed(List<Sample> s, int i) {
        Sample c = s.get(i);
        if (!Double.isNaN(c.vx) && !Double.isNaN(c.vy)) return c.speed();
        Sample a = s.get(Math.max(0, i - 1));
        Sample b = s.get(Math.min(s.size() - 1, i + 1));
        double dt = b.t - a.t;
        return dt <= 0 ? 0 : Math.hypot(b.x - a.x, b.y - a.y) / dt;
    }

    // joins two stops when the robot never left the tolerance circle between them
    private List<int[]> mergeClose(List<Sample> s, List<int[]> stops) {
        List<int[]> out = new ArrayList<>();
        for (int[] r : stops) {
            if (out.isEmpty()) {
                out.add(r.clone());
                continue;
            }
            int[] prev = out.get(out.size() - 1);
            Sample anchor = s.get(prev[1]);
            boolean close = true;
            for (int i = prev[1]; i <= r[0] && close; i++) {
                close = Math.hypot(s.get(i).x - anchor.x, s.get(i).y - anchor.y) <= config.positionTolerance;
            }
            if (close) prev[1] = r[1];
            else out.add(r.clone());
        }
        return out;
    }

    private Boundary stop(List<Sample> s, int a, int b) {
        double x = 0, y = 0;
        for (int i = a; i <= b; i++) {
            x += s.get(i).x;
            y += s.get(i).y;
        }
        int count = b - a + 1;
        double entry = s.get(a).heading;
        double exit = s.get(b).heading;
        double half = config.headingToleranceRadians() / 2;
        double turnStart = s.get(a).t;
        double turnEnd = s.get(b).t;
        for (int i = a; i <= b; i++) {
            if (Math.abs(Angles.error(entry, s.get(i).heading)) > half) {
                turnStart = s.get(i).t;
                break;
            }
        }
        for (int i = b; i >= a; i--) {
            if (Math.abs(Angles.error(exit, s.get(i).heading)) > half) {
                turnEnd = s.get(i).t;
                break;
            }
        }
        if (turnEnd < turnStart) turnEnd = turnStart;
        return new Boundary(a, b, x / count, y / count, entry, exit, s.get(a).t, s.get(b).t, true,
                (turnStart + turnEnd) / 2, turnEnd - turnStart);
    }

    private static Boundary pass(List<Sample> s, int i) {
        Sample c = s.get(i);
        return new Boundary(i, i, c.x, c.y, c.heading, c.heading, c.t, c.t, false, c.t, 0);
    }

    private static int nearest(List<Sample> s, double t, int from, int to) {
        int best = from;
        for (int i = from; i <= to; i++) {
            if (Math.abs(s.get(i).t - t) < Math.abs(s.get(best).t - t)) best = i;
        }
        return best;
    }

    private static List<Double> sorted(List<Double> in) {
        List<Double> out = new ArrayList<>(in);
        java.util.Collections.sort(out);
        return out;
    }
}
