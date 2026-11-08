package org.westwoodrobots.pathrecorder.route;

public final class PlacedAction {
    public final String expression;
    public final int piece;
    public final double t;
    public final double completion;

    public PlacedAction(String expression, int piece, double t, double completion) {
        this.expression = expression;
        this.piece = piece;
        this.t = t;
        this.completion = completion;
    }
}
