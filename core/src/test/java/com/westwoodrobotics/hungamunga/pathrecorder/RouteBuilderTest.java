package com.westwoodrobotics.hungamunga.pathrecorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.curves.bezier.BezierCurve;
import com.westwoodrobotics.hungamunga.pathrecorder.SyntheticTrace.Heading;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.Vec2;
import com.westwoodrobotics.hungamunga.pathrecorder.route.HeadingMode;
import com.westwoodrobotics.hungamunga.pathrecorder.route.PathIR;
import com.westwoodrobotics.hungamunga.pathrecorder.route.RouteBuilder;
import com.westwoodrobotics.hungamunga.pathrecorder.route.RouteIR;
import com.westwoodrobotics.hungamunga.pathrecorder.route.Step;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteBuilderTest {
    private final RecorderConfig config = new RecorderConfig();

    private RouteIR build(SyntheticTrace trace) {
        return new RouteBuilder(config).build(trace.build());
    }

    @Test
    void stopWithInPlaceTurnSplitsAndTurns() {
        RouteIR route = build(SyntheticTrace.start(10, 72, 0)
                .pause(300)
                .lineTo(40, 72).drive(Heading.HOLD, 30)
                .pause(300)
                .turnTo(90, 600)
                .pause(300)
                .lineTo(40, 110).drive(Heading.HOLD, 30)
                .pause(300));
        assertEquals(2, route.segmentCount());
        assertEquals(4, route.poses.size());
        assertEquals(Step.Kind.FOLLOW, route.steps.get(0).kind);
        assertEquals(Step.Kind.TURN, route.steps.get(1).kind);
        assertEquals(Step.Kind.FOLLOW, route.steps.get(2).kind);
        assertEquals(90, Math.toDegrees(route.poses.get(2).heading()), 0.5);
        assertTrue(route.paths.get(0).pieces.get(0).isLine());
        assertEquals(HeadingMode.TANGENT, route.paths.get(0).heading);
        assertEquals(HeadingMode.TANGENT, route.paths.get(1).heading);
    }

    @Test
    void shortPauseDoesNotSplit() {
        RouteIR route = build(SyntheticTrace.start(10, 72, 0)
                .lineTo(40, 72).drive(Heading.HOLD, 30)
                .pause(60)
                .lineTo(70, 72).drive(Heading.HOLD, 30));
        assertEquals(1, route.segmentCount());
    }

    @Test
    void pauseLongerThanMinimumSplits() {
        RouteIR route = build(SyntheticTrace.start(10, 72, 0)
                .lineTo(40, 72).drive(Heading.HOLD, 30)
                .pause(200)
                .lineTo(70, 72).drive(Heading.HOLD, 30));
        assertEquals(2, route.segmentCount());
    }

    @Test
    void waypointSplitsWhileMoving() {
        RouteIR route = build(SyntheticTrace.start(10, 72, 0)
                .lineTo(40, 72).lineTo(70, 72).waypointAt(0.5).drive(Heading.HOLD, 30));
        assertEquals(2, route.segmentCount());
        assertEquals(40, route.poses.get(1).x(), 0.5);
    }

    @Test
    void stopActionFollowsThePath() {
        RouteIR route = build(SyntheticTrace.start(10, 72, 0)
                .lineTo(40, 72).drive(Heading.HOLD, 30)
                .pause(200).action("intake.collect()").pause(200)
                .lineTo(70, 72).drive(Heading.HOLD, 30));
        assertEquals(3, route.steps.size());
        assertEquals("intake.collect()", route.steps.get(1).expression);
        assertTrue(route.paths.get(0).actions.isEmpty());
    }

    @Test
    void movingActionUsesArcLengthCompletion() {
        RouteIR route = build(SyntheticTrace.start(42, 72, 0)
                .arcAround(72, 72, 90).actionAt(0.5, "lift.score()").actionAt(0.8, "claw.open()")
                .drive(Heading.TANGENT, 30));
        PathIR path = route.paths.get(0);
        assertEquals(2, path.actions.size());
        assertEquals(0.5, path.actions.get(0).completion, 0.02);
        assertEquals(0.8, path.actions.get(1).completion, 0.02);
        assertEquals("lift.score()", path.actions.get(0).expression);
    }

    @Test
    void completionMatchesPedroArcLength() {
        RouteIR route = build(SyntheticTrace.start(42, 72, 0)
                .arcAround(72, 72, 150).actionAt(0.35, "lift.score()").drive(Heading.TANGENT, 30));
        PathIR path = route.paths.get(0);
        assertEquals(1, path.pieces.size());
        List<Vec2> cp = path.pieces.get(0).controlPoints();
        Pose[] poses = new Pose[cp.size()];
        for (int i = 0; i < poses.length; i++) poses[i] = new Pose(cp.get(i).x, cp.get(i).y);
        double pedro = new BezierCurve(poses).pathCompletion(path.actions.get(0).t);
        assertEquals(pedro, path.actions.get(0).completion, 0.005);
        assertEquals(0.35, pedro, 0.02);
    }

    @Test
    void headingModes() {
        assertEquals(HeadingMode.TANGENT, modeOf(Heading.TANGENT));
        assertEquals(HeadingMode.REVERSE_TANGENT, modeOf(Heading.REVERSE));
        assertEquals(HeadingMode.CONSTANT, modeOf(Heading.HOLD));
        RouteIR linear = build(SyntheticTrace.start(42, 72, 0)
                .arcAround(72, 72, 120).driveLinear(-45, 30));
        assertEquals(HeadingMode.LINEAR, linear.paths.get(0).heading);
        assertTrue(linear.paths.get(0).headingWithinTolerance);
    }

    @Test
    void wigglingHeadingFallsBackToLinearWithNote() {
        RouteIR route = build(SyntheticTrace.start(10, 72, 0)
                .lineTo(70, 72).wiggle(20, 30));
        PathIR path = route.paths.get(0);
        assertEquals(HeadingMode.LINEAR, path.heading);
        assertFalse(path.headingWithinTolerance);
        assertTrue(Math.toDegrees(path.headingRms) > 3);
    }

    @Test
    void noisyRouteStaysInsideTolerance() {
        RouteIR route = build(SyntheticTrace.start(10, 20, 90)
                .noise(0.15, 3)
                .lineTo(10, 60).curveTo(10, 90, 40, 110, 70, 110).drive(Heading.TANGENT, 40)
                .pause(300)
                .arcAround(70, 80, -180).drive(Heading.REVERSE, 30)
                .pause(300));
        assertEquals(2, route.segmentCount());
        assertTrue(route.maxFitError() <= config.positionTolerance, "error " + route.maxFitError());
        assertTrue(route.curveCount() <= 4, "curves " + route.curveCount());
    }

    @Test
    void mirrorFlipsAcrossAxis() {
        config.mirrorAxis = 72;
        RouteIR route = build(SyntheticTrace.start(10, 72, 0).pause(200)
                .lineTo(40, 72).drive(Heading.HOLD, 30).pause(200));
        assertEquals(134, route.poses.get(0).x(), 1e-9);
        assertEquals(Math.PI, route.poses.get(0).heading(), 1e-9);
        assertEquals(104, route.poses.get(1).x(), 0.5);
    }

    @Test
    void jsonRoundTrip() {
        Recording rec = SyntheticTrace.start(10, 72, 0).noise(0.1, 1)
                .lineTo(40, 72).actionAt(0.5, "a.b(\"x\")").drive(Heading.HOLD, 30).build();
        Recording back = Recording.fromJson(rec.toJson());
        assertEquals(rec.samples.size(), back.samples.size());
        assertEquals("a.b(\"x\")", back.actions.get(0).expression);
        assertEquals(rec.samples.get(5).x, back.samples.get(5).x, 1e-6);
        assertEquals(rec.toJson(), back.toJson());
    }

    private HeadingMode modeOf(Heading heading) {
        return build(SyntheticTrace.start(42, 72, 0).arcAround(72, 72, 90).drive(heading, 30)).paths.get(0).heading;
    }
}
