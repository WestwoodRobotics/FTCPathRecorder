## PathRecorderFTC

Turns routes driven by drivers into Java code for a
[Pedro Pathing 3](https://github.com/Pedro-Pathing/PedroPathing) autonomous with
[Ivy](https://github.com/Pedro-Pathing/Ivy) commands.

### Install

Add JitPack to `TeamCode/build.gradle` (or to `build.dependencies.gradle` in the Quickstart):

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.pedropathing:revhub:3.0.2'
    implementation 'com.pedropathing.ivy:pedro:1.1.1'
    implementation 'org.westwoodrobots:pathrecorder-ftc:0.1.0'
    implementation 'org.westwoodrobots:pathrecorder-ivy:0.1.0'
    implementation 'org.westwoodrobots:pathrecorder-panels:0.1.0'
}
```

`panels` module is optional. Add only if your team uses [Panels](https://github.com/ftcontrol/ftcontrol-panels).

The library uses `compileOnly` for Pedro, Ivy, Panels and the FTC SDK. Your project supplies these at runtime.

### Compatibility

| Dependency | Version | Source |
|---|---|---|
| Pedro Pathing | 3.0.2 | `v3.0.2` (`cf4be31`) |
| Ivy | 1.1.1 | `v1.1.1` (`d35242a`) |
| Panels field plugin | 1.0.10 | `mymaven.bylazar.com` (`a68ed55`) |
| FTC SDK | 12.0.0 | `v3.0.2` |
| Android Gradle Plugin | 8.13.2 | same as Quickstart |
| Java | source and target 1.8 | same as Quickstart |

Use Pedro 3.0.2 or later. The Quickstart `v3.0.2` still uses `revhub:3.0.1`. In Pedro 3.0.1, `linear` heading moves with the
curve parameter `t`. In 3.0.2 it moves with arc length, and the heading classifier uses arc length.

### Quick start

See [`RecordAuto`](sample/src/main/java/org/firstinspires/ftc/teamcode/RecordAuto.java) in `sample/`.

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

- The first arg of `action` is the Java expr that the generator writes into the output, must be valid.
- The last arg supplies a new `Command` for each push, scheduled by the recorder.
- Trigger is fired on the rising edge.
- The constructor calls `follower.setPose(start)`. `update` calls `follower.manual(...)` and `follower.update()`. Do not call them again in the same loop.
- `IvyRuntime.owned()` calls `Scheduler.execute()` in `update`. If your code already calls `Scheduler.execute()`, use `IvyRuntime.external()`. `Scheduler` is static, so call `Scheduler.reset()` in `init()`.
- The default stick mapping is the mapping of the Pedro localization test. For field-centric drive, use `.drive(DriveInput.fieldCentric(0))`, or give your own `DriveInput`.

When the driver pushes the finish button (or the OpMode stops and calls `finish()`):

- Code goes to `/sdcard/FIRST/pathrecorder/<timestamp>.java` on the Control Hub. Use `adb pull /sdcard/FIRST/pathrecorder`.
- `http://192.168.43.1:8080/pathrecorder` will display the newest file

The output format should be imports first, statements in a method in scope of follower/subsystem, and command is scheduled, and setPose() is called.

With `config.style = OutputStyle.CLASS` you get a class with `start()` and `create(Follower, ...)` instead.

### Configuration

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

### Notes

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

### Competition rules

During a match, the Competition Manual (R704) does not permit live data streams other than Driver Station telemetry. **Do not put any recorders in a competition OpMode.**

### License

BSD 3-Clause. See [LICENSE](LICENSE).
