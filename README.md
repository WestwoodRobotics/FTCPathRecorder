## PathRecorderFTC

Converts a driver's route into Java code for a
[Pedro Pathing 3](https://github.com/Pedro-Pathing/PedroPathing) autonomous programs using
[Ivy](https://github.com/Pedro-Pathing/Ivy) commands. The recorder logs odometry poses alongside
button actions, fits smooth curves to those coordinates, and automatically generates the code.

### Install

Add these lines to `TeamCode/build.gradle` (or to `build.dependencies.gradle` in the Quickstart):

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

The `panels` module is optional, only if your team uses [Panels](https://github.com/ftcontrol/ftcontrol-panels).

### Compatibility

| Dependency            | Version               | Source                                        |
|-----------------------|-----------------------|-----------------------------------------------|
| Pedro Pathing         | 3.0.2+                | tag `v3.0.2`, commit `cf4be31`                |
| Ivy                   | 1.1.1                 | tag `v1.1.1`, commit `d35242a`                |
| Panels field plugin   | 1.0.10                | Maven `mymaven.bylazar.com`, commit `a68ed55` |
| FTC SDK               | 12.0.0                | Pedro Quickstart `v3.0.2`                     |
| Android Gradle Plugin | 8.13.2                | Pedro Quickstart `v3.0.2`                     |
| Java                  | source and target 1.8 | Pedro Quickstart `v3.0.2`                     |

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
Args of `action`:
1. text that the generator writes into the output. Must be a valid Java expr where `follower` and your subsystems are in scope.
2. (the trigger), recorder fires the action only when the trigger changes from `false` to `true`.
3. supplies a new `Command` each time the trigger fires, scheduled by the recorder.

Additionally:
- Constructor calls `follower.setPose(start)`. `update` calls `follower.manual(...)` and `follower.update()`. Do not call these methods yourself in the same loop.
- `IvyRuntime.owned()` makes `update` call `Scheduler.execute()`. If your code already calls `Scheduler.execute()`, use `IvyRuntime.external()`.
- `Scheduler` is static and keeps commands from an earlier OpMode. Call `Scheduler.reset()` in `init()`.
- The default stick mapping is Pedro's (robot-centric). For field-centric drive, use `.drive(DriveInput.fieldCentric(0))`, or supply your own `DriveInput`.

When the driver presses the finish button, or when the OpMode stops and calls `finish()`:

- The code is written to `/sdcard/FIRST/pathrecorder/<timestamp>.java` on the Control Hub. Get it with `adb pull /sdcard/FIRST/pathrecorder`.
- `http://192.168.43.1:8080/pathrecorder` shows the newest file.

The default output (`config.style = OutputStyle.SNIPPET`) has the imports first, then statements to paste into a method where `follower` and your subsystems are in scope. The statements call `follower.setPose(...)`, build the paths, and schedule the command.

With `config.style = OutputStyle.CLASS`, output is a complete class with the methods `create(Follower, ...)` and `start()`.

### Configuration

All settings are public fields of `RecorderConfig`.

| Field | Default | Meaning                                                                                                        |
|---|---|----------------------------------------------------------------------------------------------------------------|
| `positionTolerance` | `1.0` | Maximum distance (in) from a recorded point to the fitted curve                                                |
| `headingTolerance` | `3.0` | Maximum RMS heading error (deg) for a heading mode. Also the end tolerance of an in-place turn                 |
| `stopSpeed` | `1.5` | Speed (in/s) below which the robot counts as stopped                                                           |
| `minStopTime` | `150` | Minimum stop time (ms) that splits a path                                                                      |
| `sampleInterval` | `0` | Minimum time (ms) between samples. `0` records each loop                                                       |
| `degrees` | `true` | `true`: poses in degrees with `PoseFactory.degrees()`. `false`: poses in radians with `new Pose(...)`          |
| `mirrorAxis` | `NaN` | Mirrors the output with `PoseFactory.mirrorX(axis)`, for example `72` for the other alliance. `NaN`: no mirror |
| `style` | `SNIPPET` | `SNIPPET` or `CLASS` (see [Quick start](#quick-start))                                                         |
| `followerName`, `commandName`, `startName`, `poseName`, `pathName` | `follower`, `auto`, `start`, `p`, `path` | Variable names in the output                                                                                   |
| `packageName`, `className`, `parameters`, `imports` | | `CLASS` output only. For example `parameters.add("Claw claw")`                                                 |
| `outputDirectory` | `/sdcard/FIRST/pathrecorder` | Directory for output files                                                                                     |
| `livePreview` | `true` | Fits the route again at each stop to update the overlay                                                        |

### Notes

- Pauses < `minStopTime` with no turn does not split a path.
- Stops with rx changes -> `PedroCommands.hold` + a wait until rx is in tolerance
- Waypoints anywhere split up the path, but unnecessary waypoints can make the route slower due to Pedro's braking math.
- At stops, the robot waits until all actions of that stop are done; we exclude this.
- Path with actions during motion -> `Groups.parallel`, where next step only starts when all previous action finishes
- `Paths.path(...)` allows for more than one Bezier in the path. In Pedro 3.0.2, `follower.completion()` is not correct for such a path, so we use `follower.pathIndex()` and `follower.parametricCompletion()` for it.
- Requires `follower.update()` in each loop. `follower.completion()` is `1.0` until the first update after `follow()`. `PedroCommands.follow` has the same requirement.
- If a path crosses itself, an action can be placed at the wrong crossing.
- If no heading mode is in tolerance, default `linear` and adds a `// heading error` comment with the measured RMS error.
- Odom drift is accounted for. Requires a very finely tuned localizer.

### Competition rules

**Never use `PathRecorder` in a competition OpMode,** recording is prohibited by R704.

### License

BSD 3-Clause. See [LICENSE](LICENSE).