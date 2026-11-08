package com.westwoodrobotics.hungamunga.pathrecorder;

public final class Sample {
    public final double t;
    public final double x;
    public final double y;
    public final double heading;
    public final double vx;
    public final double vy;
    public final double omega;

    public Sample(double t, double x, double y, double heading, double vx, double vy, double omega) {
        this.t = t;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.vx = vx;
        this.vy = vy;
        this.omega = omega;
    }

    public double speed() {
        return Math.hypot(vx, vy);
    }
}
