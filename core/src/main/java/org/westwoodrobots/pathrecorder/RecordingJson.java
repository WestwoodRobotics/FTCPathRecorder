package org.westwoodrobots.pathrecorder;

import com.pedropathing.math.Pose;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class RecordingJson {
    private RecordingJson() {}

    static String write(Recording r) {
        StringBuilder b = new StringBuilder();
        b.append("{\n  \"start\": [")
                .append(num(r.start.x()))
                .append(", ")
                .append(num(r.start.y()))
                .append(", ")
                .append(num(r.start.heading()))
                .append("],\n  \"samples\": [");
        for (int i = 0; i < r.samples.size(); i++) {
            Sample s = r.samples.get(i);
            b.append(i == 0 ? "\n    [" : ",\n    [")
                    .append(num(s.t)).append(", ")
                    .append(num(s.x)).append(", ")
                    .append(num(s.y)).append(", ")
                    .append(num(s.heading)).append(", ")
                    .append(num(s.vx)).append(", ")
                    .append(num(s.vy)).append(", ")
                    .append(num(s.omega)).append(']');
        }
        b.append(r.samples.isEmpty() ? "],\n  \"actions\": [" : "\n  ],\n  \"actions\": [");
        for (int i = 0; i < r.actions.size(); i++) {
            ActionEvent a = r.actions.get(i);
            b.append(i == 0 ? "\n    " : ",\n    ")
                    .append("{\"t\": ")
                    .append(num(a.t))
                    .append(", \"expression\": ")
                    .append(str(a.expression))
                    .append('}');
        }
        b.append(r.actions.isEmpty() ? "],\n  \"waypoints\": [" : "\n  ],\n  \"waypoints\": [");
        for (int i = 0; i < r.waypoints.size(); i++) {
            b.append(i == 0 ? "" : ", ").append(num(r.waypoints.get(i)));
        }
        return b.append("]\n}\n").toString();
    }

    @SuppressWarnings("unchecked")
    static Recording read(String json) {
        Map<String, Object> root = (Map<String, Object>) new Parser(json).value();
        List<Object> start = (List<Object>) root.get("start");
        List<Sample> samples = new ArrayList<>();
        for (Object o : (List<Object>) root.get("samples")) {
            List<Object> s = (List<Object>) o;
            samples.add(new Sample(d(s, 0), d(s, 1), d(s, 2), d(s, 3), d(s, 4), d(s, 5), d(s, 6)));
        }
        List<ActionEvent> actions = new ArrayList<>();
        for (Object o : (List<Object>) root.get("actions")) {
            Map<String, Object> a = (Map<String, Object>) o;
            actions.add(new ActionEvent((Double) a.get("t"), (String) a.get("expression")));
        }
        List<Double> waypoints = new ArrayList<>();
        for (Object o : (List<Object>) root.get("waypoints")) waypoints.add((Double) o);
        return new Recording(new Pose(d(start, 0), d(start, 1), d(start, 2)), samples, actions, waypoints);
    }

    private static double d(List<Object> list, int i) {
        Object o = list.get(i);
        return o == null ? Double.NaN : (Double) o;
    }

    private static String num(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "null";
        return String.format(Locale.ROOT, "%.6f", v);
    }

    private static String str(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n");
            else if (c < 0x20) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
        }

        Object value() {
            skip();
            char c = s.charAt(i);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            return number();
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            skip();
            if (s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                skip();
                String key = string();
                skip();
                expect(':');
                m.put(key, value());
                skip();
                if (s.charAt(i++) == '}') return m;
            }
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++;
            skip();
            if (s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                l.add(value());
                skip();
                if (s.charAt(i++) == ']') return l;
            }
        }

        private String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                char e = s.charAt(i++);
                if (e == 'n') b.append('\n');
                else if (e == 't') b.append('\t');
                else if (e == 'u') {
                    b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                } else b.append(e);
            }
        }

        private Double number() {
            int from = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(from, i));
        }

        private void expect(char c) {
            if (s.charAt(i) != c) throw new IllegalArgumentException("expected '" + c + "' at " + i);
            i++;
        }

        private void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}
