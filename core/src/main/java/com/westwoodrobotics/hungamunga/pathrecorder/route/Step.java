package com.westwoodrobotics.hungamunga.pathrecorder.route;

public final class Step {
    public enum Kind {
        FOLLOW,
        ACTION,
        TURN
    }

    public final Kind kind;
    public final int index;
    public final String expression;
    public final double timeoutMs;

    private Step(Kind kind, int index, String expression, double timeoutMs) {
        this.kind = kind;
        this.index = index;
        this.expression = expression;
        this.timeoutMs = timeoutMs;
    }

    public static Step follow(int path) {
        return new Step(Kind.FOLLOW, path, null, 0);
    }

    public static Step action(String expression) {
        return new Step(Kind.ACTION, -1, expression, 0);
    }

    public static Step turn(int pose, double timeoutMs) {
        return new Step(Kind.TURN, pose, null, timeoutMs);
    }
}
