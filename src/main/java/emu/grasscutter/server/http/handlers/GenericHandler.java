package emu.grasscutter.server.http.handlers;

import static emu.grasscutter.Configuration.ACCOUNT;

import java.util.ArrayList;
import java.util.Map;

import dev.morphia.annotations.Entity;
import emu.grasscutter.GameConstants;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.server.http.Router;
import emu.grasscutter.server.http.objects.HttpJsonResponse;
import emu.grasscutter.server.http.objects.WebStaticVersionResponse;
import emu.grasscutter.utils.ConfigContainer;
import express.Express;
import express.http.Request;
import express.http.Response;
import io.javalin.Javalin;

/**
 * Handles all generic, hard-coded responses.
 */
public final class GenericHandler implements Router {
    @Override
    public void applyRoutes(Express express, Javalin handle) {
        // hk4e-sdk-os.hoyoverse.com
        express.get("/hk4e_global/mdk/agreement/api/getAgreementInfos",
                new HttpJsonResponse("{\"retcode\":0,\"message\":\"OK\",\"data\":{\"marketing_agreements\":[]}}"));
        // hk4e-sdk-os.hoyoverse.com
        // this could be either GET or POST based on the observation of different
        // clients
        express.all("/hk4e_global/combo/granter/api/compareProtocolVersion", new HttpJsonResponse(
                "{\"retcode\":0,\"message\":\"OK\",\"data\":{\"modified\":true,\"protocol\":{\"id\":0,\"app_id\":4,\"language\":\"en\",\"user_proto\":\"\",\"priv_proto\":\"\",\"major\":7,\"minimum\":0,\"create_time\":\"0\",\"teenager_proto\":\"\",\"third_proto\":\"\"}}}"));

        // api-account-os.hoyoverse.com
        express.post("/account/risky/api/check", new HttpJsonResponse(
                "{\"retcode\":0,\"message\":\"OK\",\"data\":{\"id\":\"none\",\"action\":\"ACTION_NONE\",\"geetest\":null}}"));

        // sdk-os-static.hoyoverse.com
        express.get("/combo/box/api/config/sdk/combo", new HttpJsonResponse(
                "{\"retcode\":0,\"message\":\"OK\",\"data\":{\"vals\":{\"disable_email_bind_skip\":\"false\",\"email_bind_remind_interval\":\"7\",\"email_bind_remind\":\"true\"}}}"));
        // hk4e-sdk-os-static.hoyoverse.com
        express.get("/hk4e_global/combo/granter/api/getConfig", new HttpJsonResponse(
                "{\"retcode\":0,\"message\":\"OK\",\"data\":{\"protocol\":true,\"qr_enabled\":false,\"log_level\":\"INFO\",\"announce_url\":\"https://webstatic-sea.hoyoverse.com/hk4e/announcement/index.html?sdk_presentation_style=fullscreen\\u0026sdk_screen_transparent=true\\u0026game_biz=hk4e_global\\u0026auth_appid=announcement\\u0026game=hk4e#/\",\"push_alias_type\":2,\"disable_ysdk_guard\":false,\"enable_announce_pic_popup\":true}}"));
        // hk4e-sdk-os-static.hoyoverse.com
        express.get("/hk4e_global/mdk/shield/api/loadConfig", new HttpJsonResponse(
                "{\"retcode\":0,\"message\":\"OK\",\"data\":{\"id\":6,\"game_key\":\"hk4e_global\",\"client\":\"PC\",\"identity\":\"I_IDENTITY\",\"guest\":false,\"ignore_versions\":\"\",\"scene\":\"S_NORMAL\",\"name\":\"原神海外\",\"disable_regist\":false,\"enable_email_captcha\":false,\"thirdparty\":[\"fb\",\"tw\"],\"disable_mmt\":false,\"server_guest\":false,\"thirdparty_ignore\":{\"tw\":\"\",\"fb\":\"\"},\"enable_ps_bind_account\":false,\"thirdparty_login_configs\":{\"tw\":{\"token_type\":\"TK_GAME_TOKEN\",\"game_token_expires_in\":604800},\"fb\":{\"token_type\":\"TK_GAME_TOKEN\",\"game_token_expires_in\":604800}}}}"));
        // Test api?
        // abtest-api-data-sg.hoyoverse.com
        express.post("/data_abtest_api/config/experiment/list", new HttpJsonResponse(
                "{\"retcode\":0,\"success\":true,\"message\":\"\",\"data\":[{\"code\":1000,\"type\":2,\"config_id\":\"14\",\"period_id\":\"6036_99\",\"version\":\"1\",\"configs\":{\"cardType\":\"old\"}}]}"));        

        express.get("/admin/mi18n/plat_oversea/*", new WebStaticVersionResponse());
        express.get("/status/server", GenericHandler::serverStatus);
        
        // API DockerGC (TODO)
        express.all("/api/ping", new HttpJsonResponse("{\"code\":200}"));
    }

    private static void serverStatus(Request request, Response response) {
        var APIServer = new APIServer();
        response.set("Accept-Charset", "utf-8");
        response.status(200).json(APIServer.getOutput());
    }
}

@Entity
// TODO: move this class
class APIServer {
    private Map<Integer, Player> playersMap = Grasscutter.getGameServer().getPlayers();
    private ArrayList<User> UserList = new ArrayList<User>();

    public APIServer() {
        // Load Player
        playersMap.forEach((uid, player) -> {
            addUser(uid, player.getNickname());
        });
    }

    public ArrayList<User> getUser() {
        return this.UserList;
    }

    public void addUser(int uid, String nickname) {
        this.UserList.add(new User(uid, nickname));
    }

    public Output getOutput() {
        return new Output();
    }

    @Entity
    public class Output {
        public int retcode = 0;
        public Status status = new Status();
        public ArrayList<User> user = getUser();       
        public Output() {
        }
    }

    @Entity
    public class Status {
        public int playerCount = getUser().size();
        public int maxPlayer = ACCOUNT.maxPlayer;
        public String DockerGC = ConfigContainer.version_DockerGC;
        public String Version = GameConstants.VERSION;
        public Status(){
            
        }
    }

    @Entity
    public static class User {
        public String nickname;
        public int uid;

        public User(int uid, String nickname) {
            this.nickname = nickname;
            this.uid = uid;
        }
    }
}