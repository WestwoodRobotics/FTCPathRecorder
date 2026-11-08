package org.westwoodrobots.pathrecorder;

public final class ActionEvent {
    public final double t;
    public final String expression;

    public ActionEvent(double t, String expression) {
        this.t = t;
        this.expression = expression;
    }
}
