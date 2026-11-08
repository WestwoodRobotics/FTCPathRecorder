package com.westwoodrobotics.hungamunga.pathrecorder.ivy;

import com.westwoodrobotics.hungamunga.pathrecorder.OutputStyle;
import com.westwoodrobotics.hungamunga.pathrecorder.RecorderConfig;
import com.westwoodrobotics.hungamunga.pathrecorder.Recording;
import com.westwoodrobotics.hungamunga.pathrecorder.SyntheticTrace;
import com.westwoodrobotics.hungamunga.pathrecorder.SyntheticTrace.Heading;
import java.util.Arrays;
import java.util.List;

final class Scenarios {
    static final List<String> NAMES = Arrays.asList("stop_turn", "moving_actions", "mirrored_class");

    private Scenarios() {}

    static Recording trace(String name) {
        switch (name) {
            case "stop_turn":
                return SyntheticTrace.start(8, 72, 0)
                        .pause(300)
                        .lineTo(40, 72).drive(Heading.HOLD, 30)
                        .pause(200).action("intake.collect()").pause(200)
                        .turnTo(90, 600)
                        .pause(100).action("lift.score()").pause(200)
                        .lineTo(40, 110).drive(Heading.TANGENT, 30)
                        .pause(300)
                        .build();
            case "moving_actions":
                return SyntheticTrace.start(20, 20, 90)
                        .noise(0.1, 11)
                        .pause(300)
                        .lineTo(20, 60).curveTo(20, 90, 50, 110, 80, 110)
                        .actionAt(0.3, "intake.collect()").actionAt(0.7, "lift.score()")
                        .drive(Heading.TANGENT, 40)
                        .pause(300)
                        .arcAround(80, 80, -350).actionAt(0.6, "claw.open()").drive(Heading.HOLD, 30)
                        .pause(300)
                        .build();
            default:
                return SyntheticTrace.start(10, 30, 180)
                        .noise(0.1, 5)
                        .pause(300)
                        .lineTo(60, 30).curveTo(80, 30, 90, 50, 90, 70).drive(Heading.REVERSE, 35)
                        .pause(250).action("claw.open()").pause(250)
                        .lineTo(40, 100).wiggle(25, 30)
                        .pause(300)
                        .build();
        }
    }

    static RecorderConfig config(String name) {
        RecorderConfig c = new RecorderConfig();
        if (name.equals("mirrored_class")) {
            c.style = OutputStyle.CLASS;
            c.degrees = false;
            c.mirrorAxis = 72;
            c.parameters.add("Intake intake");
            c.parameters.add("Lift lift");
            c.parameters.add("Claw claw");
        }
        return c;
    }
}
