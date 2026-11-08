package org.westwoodrobots.pathrecorder.route;

public final class Boundary {
    public final int first;
    public final int last;
    public final double x;
    public final double y;
    public final double entryHeading;
    public final double exitHeading;
    public final double startTime;
    public final double endTime;
    public final boolean stop;
    public final double turnTime;
    public final double turnDuration;

    Boundary(int first, int last, double x, double y, double entryHeading, double exitHeading,
             double startTime, double endTime, boolean stop, double turnTime, double turnDuration) {
        this.first = first;
        this.last = last;
        this.x = x;
        this.y = y;
        this.entryHeading = entryHeading;
        this.exitHeading = exitHeading;
        this.startTime = startTime;
        this.endTime = endTime;
        this.stop = stop;
        this.turnTime = turnTime;
        this.turnDuration = turnDuration;
    }

    public boolean turns(double tolerance) {
        return Math.abs(Angles.error(entryHeading, exitHeading)) > tolerance;
    }
}
