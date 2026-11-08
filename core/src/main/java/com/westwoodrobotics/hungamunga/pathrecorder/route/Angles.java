package com.westwoodrobotics.hungamunga.pathrecorder.route;

final class Angles {
    private Angles() {}

    static double wrap(double a) {
        double r = a % (2 * Math.PI);
        if (r >= Math.PI) r -= 2 * Math.PI;
        if (r < -Math.PI) r += 2 * Math.PI;
        return r;
    }

    static double error(double from, double to) {
        return wrap(to - from);
    }
}
