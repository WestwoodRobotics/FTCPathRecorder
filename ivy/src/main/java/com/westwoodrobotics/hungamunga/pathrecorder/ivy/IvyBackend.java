package com.westwoodrobotics.hungamunga.pathrecorder.ivy;

import com.pedropathing.math.Pose;
import com.westwoodrobotics.hungamunga.pathrecorder.CodeGenBackend;
import com.westwoodrobotics.hungamunga.pathrecorder.OutputStyle;
import com.westwoodrobotics.hungamunga.pathrecorder.RecorderConfig;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.FitCurve;
import com.westwoodrobotics.hungamunga.pathrecorder.fit.Vec2;
import com.westwoodrobotics.hungamunga.pathrecorder.route.PathIR;
import com.westwoodrobotics.hungamunga.pathrecorder.route.PlacedAction;
import com.westwoodrobotics.hungamunga.pathrecorder.route.RouteIR;
import com.westwoodrobotics.hungamunga.pathrecorder.route.Step;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public final class IvyBackend implements CodeGenBackend {
    private static final String INDENT = "        ";

    @Override
    public String generate(RouteIR route, RecorderConfig config) {
        return new Writer(route, config).write();
    }

    private static final class Writer {
        private final RouteIR route;
        private final RecorderConfig c;
        private final TreeSet<String> imports = new TreeSet<>();

        Writer(RouteIR route, RecorderConfig config) {
            this.route = route;
            this.c = config;
        }

        String write() {
            imports.add("com.pedropathing.ivy.Command");
            imports.add("com.pedropathing.ivy.groups.Groups");
            imports.add("com.pedropathing.math.Pose");
            if (c.degrees) imports.add("com.pedropathing.api.PoseFactory");
            imports.addAll(c.imports);

            List<String> body = new ArrayList<>();
            if (c.degrees) {
                body.add("PoseFactory poses = PoseFactory.degrees();");
                body.add("");
            }
            for (int i = 0; i < route.poses.size(); i++) {
                body.add("Pose " + poseName(i) + " = " + pose(route.poses.get(i)) + ";");
            }
            if (!route.paths.isEmpty()) {
                imports.add("com.pedropathing.api.Paths");
                imports.add("com.pedropathing.paths.Path");
                body.add("");
                for (int i = 0; i < route.paths.size(); i++) path(body, i);
            }
            body.add("");
            String command = command();

            StringBuilder out = new StringBuilder();
            if (c.style == OutputStyle.CLASS) {
                imports.add("com.pedropathing.follower.Follower");
                out.append("package ").append(c.packageName).append(";\n\n");
                importBlock(out);
                out.append("public final class ").append(c.className).append(" {\n");
                out.append("    private ").append(c.className).append("() {}\n\n");
                out.append("    public static Pose start() {\n");
                out.append("        return ").append(pose(route.poses.get(0), "PoseFactory.degrees()")).append(";\n");
                out.append("    }\n\n");
                out.append("    public static Command create(").append(parameters()).append(") {\n");
                for (String line : body) out.append(line.isEmpty() ? "" : "        " + line).append('\n');
                out.append("        return ").append(command.replace("\n", "\n        ")).append(";\n");
                out.append("    }\n}\n");
            } else {
                importBlock(out);
                for (String line : body) out.append(line).append('\n');
                out.append("Command ").append(c.commandName).append(" = ").append(command).append(";\n");
            }
            return out.toString();
        }

        private void importBlock(StringBuilder out) {
            for (String i : imports) out.append("import ").append(i).append(";\n");
            out.append('\n');
        }

        private String parameters() {
            StringBuilder b = new StringBuilder("Follower ").append(c.followerName);
            for (String p : c.parameters) b.append(", ").append(p);
            return b.toString();
        }

        private void path(List<String> body, int i) {
            PathIR p = route.paths.get(i);
            if (!p.headingWithinTolerance) {
                body.add("// heading error " + angle(p.headingRms) + (c.degrees ? " deg" : " rad"));
            }
            String start = poseName(p.startPose);
            String end = poseName(p.endPose);
            StringBuilder b = new StringBuilder("Path ").append(pathName(i)).append(" = ");
            if (p.pieces.size() == 1) {
                b.append(piece(p.pieces.get(0), start, end));
            } else {
                b.append("Paths.path(");
                for (int k = 0; k < p.pieces.size(); k++) {
                    String from = k == 0 ? start : point(p.pieces.get(k).start());
                    String to = k == p.pieces.size() - 1 ? end : point(p.pieces.get(k).end());
                    b.append('\n').append(INDENT).append(piece(p.pieces.get(k), from, to));
                    b.append(k == p.pieces.size() - 1 ? ")" : ",");
                }
            }
            b.append(heading(p, start, end)).append(';');
            body.add(b.toString());
        }

        private String piece(FitCurve curve, String from, String to) {
            if (curve.isLine()) return "Paths.line(" + from + ", " + to + ")";
            List<Vec2> cp = curve.controlPoints();
            return "Paths.curve(" + from + ", " + point(cp.get(1)) + ", " + point(cp.get(2)) + ", " + to + ")";
        }

        private String heading(PathIR p, String start, String end) {
            switch (p.heading) {
                case TANGENT:
                    return ".tangent()";
                case REVERSE_TANGENT:
                    return ".reverseTangent()";
                case CONSTANT:
                    return ".constant(" + radians(p.constantHeading) + ")";
                default:
                    return ".linear(" + start + ", " + end + ")";
            }
        }

        private String command() {
            List<String> parts = new ArrayList<>();
            for (Step s : route.steps) {
                switch (s.kind) {
                    case FOLLOW:
                        parts.add(follow(s.index));
                        break;
                    case ACTION:
                        parts.add(s.expression);
                        break;
                    case TURN:
                        parts.addAll(turn(s));
                        break;
                }
            }
            return group("Groups.sequential", parts);
        }

        private String follow(int index) {
            imports.add("com.pedropathing.ivy.pedro.PedroCommands");
            PathIR p = route.paths.get(index);
            String follow = "PedroCommands.follow(" + c.followerName + ", " + pathName(index) + ")";
            if (p.actions.isEmpty()) return follow;
            imports.add("com.pedropathing.ivy.commands.Commands");
            List<String> parts = new ArrayList<>();
            parts.add(follow);
            int piece = 0;
            double t = 0;
            double completion = 0;
            for (PlacedAction a : p.actions) {
                // later actions never trigger before earlier ones
                if (a.piece > piece || (a.piece == piece && a.t > t)) {
                    piece = a.piece;
                    t = a.t;
                }
                completion = Math.max(completion, a.completion);
                parts.add("Commands.waitUntil(() -> " + condition(p, piece, t, completion) + ").then(" + a.expression + ")");
            }
            return group("Groups.parallel", parts);
        }

        private String condition(PathIR p, int piece, double t, double completion) {
            String f = c.followerName;
            if (p.pieces.size() == 1) return f + ".completion() >= " + fraction(completion);
            // follower.completion() mixes piece t with the whole compound curve in pedro 3.0.2
            return "!" + f + ".following() || " + f + ".pathIndex() > " + piece + " || " + f + ".pathIndex() == "
                    + piece + " && " + f + ".parametricCompletion() >= " + fraction(t);
        }

        private List<String> turn(Step s) {
            imports.add("com.pedropathing.ivy.pedro.PedroCommands");
            imports.add("com.pedropathing.ivy.commands.Commands");
            imports.add("com.pedropathing.utils.Angle");
            String pose = poseName(s.index);
            String f = c.followerName;
            List<String> race = new ArrayList<>();
            race.add("Commands.waitUntil(() -> Math.abs(Angle.normalizeSigned(" + f + ".pose().heading() - " + pose
                    + ".heading())) < " + radians(Math.toRadians(c.headingTolerance)) + ")");
            race.add("Commands.waitMs(" + (long) s.timeoutMs + ")");
            List<String> out = new ArrayList<>();
            out.add("PedroCommands.hold(" + f + ", " + pose + ")");
            out.add(group("Groups.race", race));
            return out;
        }

        private static String group(String name, List<String> parts) {
            StringBuilder b = new StringBuilder(name).append('(');
            for (int i = 0; i < parts.size(); i++) {
                b.append('\n').append(INDENT).append(parts.get(i).replace("\n", "\n" + INDENT));
                if (i < parts.size() - 1) b.append(',');
            }
            return b.append(')').toString();
        }

        private String poseName(int i) {
            return i == 0 ? c.startName : c.poseName + i;
        }

        private String pathName(int i) {
            return c.pathName + (i + 1);
        }

        private String pose(Pose p) {
            return pose(p, "poses");
        }

        private String pose(Pose p, String factory) {
            if (c.degrees) {
                return factory + ".of(" + coord(p.x()) + ", " + coord(p.y()) + ", " + angle(p.heading()) + ")";
            }
            return "new Pose(" + coord(p.x()) + ", " + coord(p.y()) + ", " + angle(p.heading()) + ")";
        }

        private static String point(Vec2 v) {
            return "new Pose(" + coord(v.x) + ", " + coord(v.y) + ")";
        }

        private String radians(double a) {
            return c.degrees ? "Math.toRadians(" + angle(a) + ")" : angle(a);
        }

        private String angle(double radians) {
            double deg = Math.round(wrapDegrees(Math.toDegrees(radians)) * 10) / 10.0;
            if (deg == -180.0) deg = 180.0;
            if (c.degrees) return clean(String.format(Locale.ROOT, "%.1f", deg));
            return clean(String.format(Locale.ROOT, "%.4f", Math.toRadians(deg)));
        }

        private static String coord(double v) {
            return clean(String.format(Locale.ROOT, "%.1f", Math.round(v * 10) / 10.0));
        }

        private static String fraction(double v) {
            return clean(String.format(Locale.ROOT, "%.3f", Math.max(0, Math.min(1, v))));
        }

        private static String clean(String s) {
            return s.matches("-0\\.0+") ? s.substring(1) : s;
        }

        private static double wrapDegrees(double d) {
            double r = d % 360;
            if (r > 180) r -= 360;
            if (r <= -180) r += 360;
            return r;
        }
    }
}
