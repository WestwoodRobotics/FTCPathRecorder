package org.westwoodrobots.pathrecorder;

import org.westwoodrobots.pathrecorder.route.RouteIR;

public interface CodeGenBackend {
    String generate(RouteIR route, RecorderConfig config);
}
