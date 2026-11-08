package org.westwoodrobots.pathrecorder;

import com.pedropathing.math.Pose;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Recording {
    public final Pose start;
    public final List<Sample> samples;
    public final List<ActionEvent> actions;
    public final List<Double> waypoints;

    public Recording(Pose start, List<Sample> samples, List<ActionEvent> actions, List<Double> waypoints) {
        this.start = start;
        this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
        this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
        this.waypoints = Collections.unmodifiableList(new ArrayList<>(waypoints));
    }

    public String toJson() {
        return RecordingJson.write(this);
    }

    public static Recording fromJson(String json) {
        return RecordingJson.read(json);
    }
}
