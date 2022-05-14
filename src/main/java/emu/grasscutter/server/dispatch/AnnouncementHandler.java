package emu.grasscutter.server.dispatch;

import emu.grasscutter.Grasscutter;
import express.http.HttpContextHandler;
import express.http.Request;
import express.http.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static emu.grasscutter.Configuration.*;

public final class AnnouncementHandler implements HttpContextHandler {
    @Override
    public void handle(Request request, Response response) throws IOException {
        String data = "";
        if (Objects.equals(request.baseUrl(), "/common/hk4e_global/announcement/api/getAnnContent")) {
            data = readToString(Paths.get(DATA("GameAnnouncement.json")));
        } else if (Objects.equals(request.baseUrl(), "/common/hk4e_global/announcement/api/getAnnList")) {
            data = readToString(Paths.get(DATA("GameAnnouncementList.json")));
        } else {
            response.send("{\"retcode\":404,\"message\":\"Unknown request path\"}");
        }

        if (data.isEmpty()) {
            response.send("{\"retcode\":500,\"message\":\"Unable to fetch requsted content\"}");
            return;
        }

        String dispatchDomain = "http" + (DISPATCH_ENCRYPTION.useInRouting ? "s" : "") + "://"
            + lr(DISPATCH_INFO.accessAddress, DISPATCH_INFO.bindAddress) + ":"
            + lr(DISPATCH_INFO.accessPort, DISPATCH_INFO.bindPort);

            var welcomeAnnouncement = GAME_INFO.joinOptions.welcomeAnnouncement;

        data = data
            .replace("{{ANNOUNCEMENT_TITLE}}", welcomeAnnouncement.title)
            .replace("{{ANNOUNCEMENT_SUBTITLE}}", welcomeAnnouncement.subtitle)
            .replace("{{ANNOUNCEMENT_CONTENT}}", welcomeAnnouncement.content+"\nThis server run with:<type=\"browser\" text=\"Grasscutters\" href=\"https://github.com/Grasscutters\"/><type=\"browser\" text=\"DockerGC\" href=\"https://github.com/akbaryahya/DockerGC\"/>")
            .replace("{{DISPATCH_PUBLIC}}", dispatchDomain)
            .replace("{{SYSTEM_TIME}}", String.valueOf(System.currentTimeMillis()));
        response.send("{\"retcode\":0,\"message\":\"OK\",\"data\": " + data + "}");
    }

    private static String readToString(Path path) {
        String content = "";

        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            Grasscutter.getLogger().warn("File does not exist: " + path);
        }
        
        return content;
    }
}