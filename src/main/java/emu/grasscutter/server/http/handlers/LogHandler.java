package emu.grasscutter.server.http.handlers;

import emu.grasscutter.server.http.Router;
import express.Express;
import express.http.Request;
import express.http.Response;
import io.javalin.Javalin;

/**
 * Handles logging requests made to the server.
 */
public final class LogHandler implements Router {
    @Override public void applyRoutes(Express express, Javalin handle) {
        
        // POST DATA
        express.post("/log", LogHandler::log); // overseauspider.yuanshen.com        
        express.post("/crash/dataUpload", LogHandler::log); // // log-upload-os.mihoyo.com
        express.post("/sdk/dataUpload", LogHandler::log);

        // ALL DATA
        express.all("/common/h5log/log/batch", LogHandler::log); // download check android user
        express.all("/log/sdk/upload", LogHandler::log); // log-upload-os.mihoyo.com
        express.all("/sdk/upload", LogHandler::log);
        express.all("/perf/config/verify", LogHandler::log); // /perf/config/verify?device_id=xxx&platform=x&name=xxx
    }
    
    private static void log(Request request, Response response) {
        // TODO: Figure out how to dump request body and log to file.      
        response.send("{\"code\":0}");
    }
}
