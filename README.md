# PathRecorder

PathRecorder turns a route that a driver drives into Java code for a
[Pedro Pathing 3](https://github.com/Pedro-Pathing/PedroPathing) autonomous with
[Ivy](https://github.com/Pedro-Pathing/Ivy) commands.

## What it does

1. The driver drives the route in a TeleOp OpMode and pushes buttons that start the team's own commands.
2. The library records the pose of the robot and the time of each button push.
3. At the end, the library makes:
   - Pedro paths with as few cubic Bézier curves (or lines) as possible, inside a position tolerance.
   - One heading mode for each path: tangent, reverse tangent, constant or linear.
   - One Ivy command that follows the paths in sequence. An action that the driver started while the robot was stopped becomes a step between paths. An action that the driver started while the robot moved starts at the same arc-length completion of the path.
4. The library writes the code to a file. You copy the code into your auto and edit it as you like.

The library does not replay the route. The output is plain code that your team owns.

## Install

Add JitPack to `TeamCode/build.gradle` (or to `build.dependencies.gradle` in the Quickstart):

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.pedropathing:revhub:3.0.2'
    implementation 'com.pedropathing.ivy:pedro:1.1.1'
    implementation 'com.westwoodrobotics.hungamunga:pathrecorder-ftc:0.1.0'
    implementation 'com.westwoodrobotics.hungamunga:pathrecorder-ivy:0.1.0'
    implementation 'com.westwoodrobotics.hungamunga:pathrecorder-panels:0.1.0'
}
```

The `panels` module is optional. Add it only if your project uses [Panels](https://github.com/ftcontrol/ftcontrol-panels).

The library uses `compileOnly` for Pedro, Ivy, Panels and the FTC SDK. Your project supplies these at runtime.

The group `com.westwoodrobotics.hungamunga` needs a JitPack custom domain. Without one, JitPack serves the modules as
`com.github.<owner>.PathRecorder:pathrecorder-<module>:<tag>`. To change the group, edit `group` in `gradle.properties`.

## Compatibility

| Dependency | Version | Source |
|---|---|---|
| Pedro Pathing | 3.0.2 | tag `v3.0.2` (`cf4be31`) |
| Ivy | 1.1.1 | tag `v1.1.1` (`d35242a`) |
| Panels field plugin | 1.0.10 | `mymaven.bylazar.com` (the repo has no tags, `a68ed55`) |
| FTC SDK | 12.0.0 | same as the Pedro Quickstart `v3.0.2` |
| Android Gradle Plugin | 8.13.2 | same as the Quickstart |
| Java | source and target 1.8 | same as the Quickstart |

Use Pedro 3.0.2 or later. The Quickstart `v3.0.2` still uses `revhub:3.0.1`. In Pedro 3.0.1, `linear` heading moves with the
curve parameter `t`. In 3.0.2 it moves with arc length, and the heading classifier uses arc length.

## Quick start

See [`RecordAuto`](sample/src/main/java/org/firstinspires/ftc/teamcode/RecordAuto.java).

```java
recorder = new PathRecorder<>(follower, new Pose(8, 72, 0), config, IvyRuntime.owned())
        .action("claw.open()", () -> gamepad1.aWasPressed(), claw::open)
        .action("claw.close()", () -> gamepad1.bWasPressed(), claw::close)
        .waypoint(() -> gamepad1.xWasPressed())
        .finish(() -> gamepad1.backWasPressed())
        .overlay(new PanelsOverlay());

// in loop()
recorder.update(gamepad1, telemetry);
telemetry.update();
```

- The first argument of `action` is the Java expression that the generator writes into the output. It must be valid in your auto.
- The last argument supplies a new `Command` for each push. The recorder schedules it.
- A trigger fires on its rising edge. `gamepad1.a` and `gamepad1.aWasPressed()` both work. `aWasPressed()` clears its flag when it is read, so do not read the same button in other code.
- The constructor calls `follower.setPose(start)`. `update` calls `follower.manual(...)` and `follower.update()`. Do not call them again in the same loop.
- `IvyRuntime.owned()` calls `Scheduler.execute()` in `update`. If your code already calls `Scheduler.execute()`, use `IvyRuntime.external()`. `Scheduler` is static, so call `Scheduler.reset()` in `init()`.
- The default stick mapping is the mapping of the Quickstart localization test. For field-centric drive, use `.drive(DriveInput.fieldCentric(0))`, or give your own `DriveInput`.

When the driver pushes the finish button (or the OpMode stops and calls `finish()`):

- The code goes to `/sdcard/FIRST/pathrecorder/<timestamp>.java`, and the raw trace to `<timestamp>.json`. Use `adb pull /sdcard/FIRST/pathrecorder`.
- Telemetry shows the segment count, curve count, maximum fit error and the file name.
- `http://192.168.43.1:8080/pathrecorder` shows the newest file with a Copy button. The page reads only the default output directory.

The output starts with the imports, then a blank line, then the statements. Put the imports at the top of your file and the statements in a method that has `follower` and your subsystems in scope. Then schedule the command and call `follower.setPose(start)`. With `config.style = OutputStyle.CLASS` you get a class with `start()` and `create(Follower, ...)` instead.

## Configuration

All settings are public fields of `RecorderConfig`.

| Field | Default | Meaning |
|---|---|---|
| `positionTolerance` | `1.0` | Maximum distance (in) from a recorded point to the fitted curve |
| `headingTolerance` | `3.0` | RMS heading error (deg) for a heading mode, and the turn tolerance |
| `stopSpeed` | `1.5` | Speed (in/s) below which the robot is stopped |
| `minStopTime` | `150` | Minimum stop time (ms) that splits a path |
| `sampleInterval` | `0` | Minimum time (ms) between samples, `0` records every loop |
| `degrees` | `true` | Degrees through `PoseFactory.degrees()`, or radians through `new Pose(...)` |
| `mirrorAxis` | `NaN` | Mirror the output with `PoseFactory.mirrorX(axis)`, for example `72` for the other alliance |
| `style` | `SNIPPET` | `SNIPPET` or `CLASS` |
| `followerName`, `commandName`, `startName`, `poseName`, `pathName` | `follower`, `auto`, `start`, `p`, `path` | Names in the output |
| `packageName`, `className`, `parameters`, `imports` | | Settings for `CLASS` output, for example `parameters.add("Claw claw")` |
| `outputDirectory` | `/sdcard/FIRST/pathrecorder` | Directory for the files |
| `livePreview` | `true` | Fit the route again at each stop for the overlay |

Coordinates are Pedro coordinates (0 to 144 in). The output rounds coordinates to 0.1 in and angles to 0.1°. The same recording always gives the same text.

## Limitations

- A short pause (less than `minStopTime`) with no turn does not split a path.
- A stop with a heading change becomes `PedroCommands.hold` and a wait for the heading, with a timeout of two times the recorded turn time (500 ms minimum).
- A waypoint during motion splits the path into two paths. Pedro can slow the robot at the end of each path.
- The robot waits at each stop until the actions of the stop are done. The recorded wait time is not in the output.
- A path with an action during motion is a `Groups.parallel`. The next step starts only when all actions of that path are done.
- One path can contain more than one Bézier curve as `Paths.path(...)`. In Pedro 3.0.2, `follower.completion()` is not correct for such a path. For these paths the output uses `follower.pathIndex()` and `follower.parametricCompletion()` instead.
- The output depends on `follower.update()` in each loop. `follower.completion()` is `1.0` until the first update after `follow()`, the same as `PedroCommands.follow`.
- A path that crosses itself can place an action at the wrong crossing.
- If no heading mode is inside the tolerance, the output uses `linear` and a `// heading error` note with the measured RMS error.
- Odometry drift goes into the output. The output is only as good as your localizer.

## Competition rules

This is a practice tool. The web page and the Panels overlay send data over the robot Wi-Fi. During a match, the Competition Manual (R704) does not permit live data streams other than Driver Station telemetry. Do not put the recorder in a competition OpMode.

## Credits

[Pedro Pathing](https://github.com/Pedro-Pathing/PedroPathing) and [Ivy](https://github.com/Pedro-Pathing/Ivy) by the Pedro Pathing team. PathRecorder uses their public API only. The Panels overlay uses the public API of [Panels](https://github.com/ftcontrol/ftcontrol-panels) by Lazar, which has its own license (FTControl License v1.0). The curve fitter follows Philip J. Schneider, "An Algorithm for Automatically Fitting Digitized Curves", Graphics Gems, 1990.

## License

BSD 3-Clause. See [LICENSE](LICENSE).
