package com.westwoodrobotics.hungamunga.pathrecorder.ftc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class Exporter {
    private Exporter() {}

    static File write(String directory, long timeMillis, String code, String json) throws IOException {
        File dir = new File(directory);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(timeMillis));
        File java = new File(dir, stamp + ".java");
        save(java, code);
        save(new File(dir, stamp + ".json"), json);
        return java;
    }

    static File newest(String directory) {
        File[] files = new File(directory).listFiles((d, name) -> name.endsWith(".java"));
        if (files == null) return null;
        File best = null;
        for (File f : files) {
            if (best == null || f.getName().compareTo(best.getName()) > 0) best = f;
        }
        return best;
    }

    static String read(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void save(File file, String text) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
