package org.firstinspires.ftc.teamcode;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.commands.Commands;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

public class Claw {
    private final Servo servo;

    public Claw(HardwareMap hardwareMap) {
        servo = hardwareMap.get(Servo.class, "claw");
    }

    public Command open() {
        return Commands.instant(() -> servo.setPosition(1.0));
    }

    public Command close() {
        return Commands.instant(() -> servo.setPosition(0.0));
    }
}
