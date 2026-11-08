package com.westwoodrobotics.hungamunga.pathrecorder.ftc;

import com.pedropathing.math.Pose;
import com.westwoodrobotics.hungamunga.pathrecorder.Sample;
import com.westwoodrobotics.hungamunga.pathrecorder.route.RouteIR;
import java.util.List;

public interface RecorderOverlay {
    // route is null until the first preview or the final export
    void draw(List<Sample> trace, RouteIR route, Pose pose);
}
