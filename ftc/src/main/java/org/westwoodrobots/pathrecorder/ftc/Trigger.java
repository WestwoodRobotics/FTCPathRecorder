package org.westwoodrobots.pathrecorder.ftc;

import java.util.function.BooleanSupplier;

final class Trigger {
    private final BooleanSupplier source;
    private boolean last;

    Trigger(BooleanSupplier source) {
        this.source = source;
    }

    // rising edge, so both gamepad1.a and gamepad1.aWasPressed() work
    boolean poll() {
        boolean now = source.getAsBoolean();
        boolean edge = now && !last;
        last = now;
        return edge;
    }
}
