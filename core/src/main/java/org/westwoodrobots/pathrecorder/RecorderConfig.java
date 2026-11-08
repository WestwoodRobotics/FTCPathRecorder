package org.westwoodrobots.pathrecorder;

import java.util.ArrayList;
import java.util.List;

public final class RecorderConfig {
    public double positionTolerance = 1.0;
    public double headingTolerance = 3.0;
    public double stopSpeed = 1.5;
    public double minStopTime = 150;
    public double sampleInterval = 0;

    public boolean degrees = true;
    public double mirrorAxis = Double.NaN;
    public OutputStyle style = OutputStyle.SNIPPET;

    public String followerName = "follower";
    public String commandName = "auto";
    public String startName = "start";
    public String poseName = "p";
    public String pathName = "path";

    public String packageName = "org.firstinspires.ftc.teamcode";
    public String className = "RecordedAuto";
    public List<String> parameters = new ArrayList<>();
    public List<String> imports = new ArrayList<>();

    public String outputDirectory = "/sdcard/FIRST/pathrecorder";
    public boolean livePreview = true;

    public double headingToleranceRadians() {
        return Math.toRadians(headingTolerance);
    }

    public boolean mirrored() {
        return !Double.isNaN(mirrorAxis);
    }
}
