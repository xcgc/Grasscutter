package emu.grasscutter.server.dispatch.http;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.utils.FileUtils;
import emu.grasscutter.utils.Utils;
import express.http.HttpContextHandler;
import express.http.Request;
import express.http.Response;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static emu.grasscutter.Configuration.DATA;

public class Hk4eIndexHandler implements HttpContextHandler {
    @Override
    public void handle(Request req, Response res) throws IOException {
        File renderFile = new File(Utils.toFilePath(DATA(req.path())));
        if (renderFile.exists()) {
            switch(req.path().substring(req.path().lastIndexOf(".") + 1)) {
                case "css":
                    res.type("text/css");
                    break;
                case "html":
                    res.type("text/html");
                    break;
                case "js":
                    res.type("text/javascript");
                    break;
                default:
                    res.type("application/octet-stream");
                    break;
            }

            res.send(FileUtils.read(renderFile));
        } else {
            Grasscutter.getLogger().warn("File does not exist: " + renderFile);
            res.status(404);
            res.send("");
        }
    }
}