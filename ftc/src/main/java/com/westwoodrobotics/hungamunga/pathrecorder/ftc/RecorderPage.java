package com.westwoodrobotics.hungamunga.pathrecorder.ftc;

import android.content.Context;
import com.qualcomm.robotcore.util.WebHandlerManager;
import com.westwoodrobotics.hungamunga.pathrecorder.RecorderConfig;
import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.IOException;
import org.firstinspires.ftc.ftccommon.external.WebHandlerRegistrar;

public final class RecorderPage {
    public static final String PATH = "/pathrecorder";

    private RecorderPage() {}

    @WebHandlerRegistrar
    public static void attach(Context context, WebHandlerManager manager) {
        manager.register(PATH, session -> NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK, "text/html", html(new RecorderConfig().outputDirectory)));
    }

    static String html(String directory) {
        File file = Exporter.newest(directory);
        String code;
        try {
            code = file == null ? "" : Exporter.read(file);
        } catch (IOException e) {
            code = "";
        }
        String title = file == null ? "no recording yet" : file.getName();
        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>PathRecorder</title>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<style>body{font-family:sans-serif;margin:16px}textarea{width:100%;height:75vh;font-family:monospace}"
                + "button{font-size:1.1em;padding:6px 16px}</style></head><body>"
                + "<h3>" + escape(title) + "</h3>"
                + "<button onclick=\"var t=document.getElementById('code');t.select();document.execCommand('copy');"
                + "this.textContent='Copied'\">Copy</button>"
                + "<p><textarea id=\"code\" readonly>" + escape(code) + "</textarea></p></body></html>";
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
