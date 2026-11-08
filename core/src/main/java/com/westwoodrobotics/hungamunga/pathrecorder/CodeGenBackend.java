package com.westwoodrobotics.hungamunga.pathrecorder;

import com.westwoodrobotics.hungamunga.pathrecorder.route.RouteIR;

public interface CodeGenBackend {
    String generate(RouteIR route, RecorderConfig config);
}
