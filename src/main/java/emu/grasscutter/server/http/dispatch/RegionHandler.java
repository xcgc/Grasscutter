package emu.grasscutter.server.http.dispatch;

import static emu.grasscutter.Configuration.DISPATCH_INFO;
import static emu.grasscutter.Configuration.GAME_INFO;
import static emu.grasscutter.Configuration.HTTP_ENCRYPTION;
import static emu.grasscutter.Configuration.HTTP_INFO;
import static emu.grasscutter.Configuration.SERVER;
import static emu.grasscutter.Configuration.lr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.ByteString;

import emu.grasscutter.GameConstants;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.Grasscutter.ServerRunMode;
import emu.grasscutter.net.proto.QueryCurrRegionHttpRspOuterClass.QueryCurrRegionHttpRsp;
import emu.grasscutter.net.proto.QueryRegionListHttpRspOuterClass.QueryRegionListHttpRsp;
import emu.grasscutter.net.proto.RegionInfoOuterClass.RegionInfo;
import emu.grasscutter.net.proto.RegionSimpleInfoOuterClass.RegionSimpleInfo;
import emu.grasscutter.server.event.dispatch.QueryAllRegionsEvent;
import emu.grasscutter.server.event.dispatch.QueryCurrentRegionEvent;
import emu.grasscutter.server.game.GameServer;
import emu.grasscutter.server.http.Router;
import emu.grasscutter.utils.ConfigContainer.Region;
import emu.grasscutter.utils.Crypto;
import emu.grasscutter.utils.Utils;
import express.Express;
import express.http.Request;
import express.http.Response;
import io.javalin.Javalin;

/**
 * Handles requests related to region queries.
 */
public final class RegionHandler implements Router {
    private static final Map<String, RegionData> regions = new ConcurrentHashMap<>();
    private static String regionListResponse;
    
    public RegionHandler() {
        try { // Read & initialize region data.
            this.initialize();
        } catch (Exception exception) {
            Grasscutter.getLogger().error("Failed to initialize region data.", exception);
        }
    }

    /**
     * Configures region data according to configuration.
     */
    private void initialize() {
        String dispatchDomain = "http" + (HTTP_ENCRYPTION.useInRouting ? "s" : "") + "://"
                + lr(HTTP_INFO.accessAddress, HTTP_INFO.bindAddress) + ":"
                + lr(HTTP_INFO.accessPort, HTTP_INFO.bindPort);
        
        // Create regions.
        List<RegionSimpleInfo> servers = new ArrayList<>();
        List<String> usedNames = new ArrayList<>(); // List to check for potential naming conflicts.
        
        var configuredRegions = new ArrayList<>(List.of(DISPATCH_INFO.regions));
        if(SERVER.runMode != ServerRunMode.HYBRID && configuredRegions.size() == 0) {
            GameServer.doExit(0,"[Dispatch] There are no game servers available. Exiting due to unplayable state.");
        } else if (configuredRegions.size() == 0) 
            configuredRegions.add(new Region("os_usa", DISPATCH_INFO.defaultName,
                lr(GAME_INFO.accessAddress, GAME_INFO.bindAddress), 
                lr(GAME_INFO.accessPort, GAME_INFO.bindPort)));
        
        configuredRegions.forEach(region -> {
            if (usedNames.contains(region.Name)) {
                Grasscutter.getLogger().error("Region name already in use.");
                return;
            }
    
            // Create a region identifier.
            var identifier = RegionSimpleInfo.newBuilder()
                    .setName(region.Name).setTitle(region.Title).setType("DEV_PUBLIC")
                    .setDispatchUrl(dispatchDomain + "/query_cur_region/" + region.Name)
                    .build();
            usedNames.add(region.Name); servers.add(identifier);
            
            // Create a region info object.
            var regionInfo = RegionInfo.newBuilder()
                    .setGateserverIp(region.Ip).setGateserverPort(region.Port)
                    .setSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                    .build();
            // Create an updated region query.
            var updatedQuery = QueryCurrRegionHttpRsp.newBuilder().setRegionInfo(regionInfo).build();
            regions.put(region.Name, new RegionData(updatedQuery, Utils.base64Encode(updatedQuery.toByteString().toByteArray())));
        });
        
        // Create a config object.
        byte[] customConfig = "{\"sdkenv\":\"2\",\"checkdevice\":\"false\",\"loadPatch\":\"false\",\"showexception\":\"false\",\"regionConfig\":\"pm|fk|add\",\"downloadMode\":\"0\"}".getBytes();
        Crypto.xor(customConfig, Crypto.DISPATCH_KEY); // XOR the config with the key.
        
        // Create an updated region list.
        QueryRegionListHttpRsp updatedRegionList = QueryRegionListHttpRsp.newBuilder()
                .addAllRegionList(servers)
                .setClientSecretKey(ByteString.copyFrom(Crypto.DISPATCH_SEED))
                .setClientCustomConfigEncrypted(ByteString.copyFrom(customConfig))
                .setEnableLoginPc(true).build();
        
        // Set the region list response.
        regionListResponse = Utils.base64Encode(updatedRegionList.toByteString().toByteArray());
    }
    
    @Override public void applyRoutes(Express express, Javalin handle) {
        express.get("/query_region_list", RegionHandler::queryRegionList);
        express.get("/query_cur_region/:region", RegionHandler::queryCurrentRegion );
    }

    /**
     * @route /query_region_list
     */
    private static void queryRegionList(Request request, Response response) {        
        // Invoke event.
        QueryAllRegionsEvent event = new QueryAllRegionsEvent(regionListResponse); event.call();
        // Respond with event result.
        response.send(event.getRegionList());        
        // Log to console.
        Grasscutter.getLogger().info(String.format("[Dispatch] Client %s request: query_region_list", Utils.getClientIpAddress(request)));
    }

    /**
     * @route /query_cur_region/:region
     */
    private static void queryCurrentRegion(Request request, Response response) {

        // Get region to query.
        String regionName = request.params("region");
        
        String versionName = request.query("version");
        String dispatchSeedName = request.query("dispatchSeed");
        String full = request.originalUrl();

        Grasscutter.getLogger().info("Client "+Utils.getClientIpAddress(request)+" ("+versionName+") ("+dispatchSeedName+") request: query_cur_region/"+regionName+"");
        Grasscutter.getLogger().info(full);
        
         // check update         
        if(!versionName.contains(GameConstants.VERSION)) {

            boolean iserror = true;
            
            if(versionName.contains("2.7.50")){
                // ReportErrorCode DISPATCH_REGION_DECRYPT_FAIL = 4214;
                iserror=false;
            }

            if(iserror){
             var updatedQuery = QueryCurrRegionHttpRsp.newBuilder()
             .setMsg("Server Version is "+GameConstants.VERSION+"\nYour Current Version is "+versionName+"\n\nPlease update your game client from official server by turning  off proxy or download last client from yuuki site.\n\nInfo: game.yuuki.me")
             .build();
             response.send(Utils.base64Encode(updatedQuery.toByteString().toByteArray()));                          
            }else{
             response.send("0");    
            }
            
            return;
        }
        
        // Get region data.
        String regionData = "CAESGE5vdCBGb3VuZCB2ZXJzaW9uIGNvbmZpZw==";
        if (request.query().values().size() > 0) {
            var region = regions.get(regionName);
            if(region != null) regionData = region.getBase64();
        }
        
        // Invoke event.
        QueryCurrentRegionEvent event = new QueryCurrentRegionEvent(regionData); event.call();
        // Respond with event result.
        response.send(event.getRegionInfo());
    }

    /**
     * Region data container.
     */
    public static class RegionData {
        private final QueryCurrRegionHttpRsp regionQuery;
        private final String base64;

        public RegionData(QueryCurrRegionHttpRsp prq, String b64) {
            this.regionQuery = prq;
            this.base64 = b64;
        }

        public QueryCurrRegionHttpRsp getRegionQuery() {
            return this.regionQuery;
        }

        public String getBase64() {
            return this.base64;
        }
    }

    /**
     * Gets the current region query.
     * @return A {@link QueryCurrRegionHttpRsp} object.
     */
    public static QueryCurrRegionHttpRsp getCurrentRegion() {
        return SERVER.runMode == ServerRunMode.HYBRID ? regions.get("os_usa").getRegionQuery() : null;
    }
}
