package org.westwoodrobots.pathrecorder.ftc;

import com.pedropathing.math.Pose;
import org.westwoodrobots.pathrecorder.Sample;
import org.westwoodrobots.pathrecorder.route.RouteIR;
import java.util.List;

public interface RecorderOverlay {
    // route is null until the first preview or the final export
    void draw(List<Sample> trace, RouteIR route, Pose pose);
}
