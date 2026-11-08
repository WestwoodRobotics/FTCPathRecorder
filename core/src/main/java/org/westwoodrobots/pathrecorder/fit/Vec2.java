package org.westwoodrobots.pathrecorder.fit;

public final class Vec2 {
    public final double x;
    public final double y;

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 plus(Vec2 o) {
        return new Vec2(x + o.x, y + o.y);
    }

    public Vec2 minus(Vec2 o) {
        return new Vec2(x - o.x, y - o.y);
    }

    public Vec2 times(double s) {
        return new Vec2(x * s, y * s);
    }

    public double dot(Vec2 o) {
        return x * o.x + y * o.y;
    }

    public double norm() {
        return Math.hypot(x, y);
    }

    public double distance(Vec2 o) {
        return Math.hypot(x - o.x, y - o.y);
    }

    public double angle() {
        return Math.atan2(y, x);
    }

    public Vec2 normalized() {
        double n = norm();
        return n < 1e-12 ? new Vec2(0, 0) : new Vec2(x / n, y / n);
    }
}
