package fun.iiii.mixedlogin;

import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import fun.iiii.mixedlogin.yggdrasil.VirtualYggdrasilServer;
import fun.iiii.mixedlogin.yggdrasil.offline.VirtualOfflineService;
import fun.iiii.mixedlogin.yggdrasil.offline.VirtualSubService;

import java.nio.file.Path;
import java.util.Optional;

public class LoginServerManager {
    private final VirtualOfflineService virtualOfflineService = new VirtualOfflineService();
    private final VirtualSubService virtualSubService = new VirtualSubService();
    private final VirtualYggdrasilServer offlineYggdrasilServer = new VirtualYggdrasilServer(26748, "127.0.0.1", virtualOfflineService);
    private final VirtualYggdrasilServer subYggdrasilServer = new VirtualYggdrasilServer(26749, "127.0.0.1", virtualSubService);

    private static LoginServerManager instance;

    public void start() {
        instance = this;
        try {
            offlineYggdrasilServer.start();
            subYggdrasilServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean shouldOfflineHost(Optional<String> hostName) {
        if (hostName.isEmpty()) return false;
        String hostNameUnpack = hostName.get();
        if (hostNameUnpack.startsWith("offline")) return true;
        if (hostNameUnpack.startsWith("o-")) return true;
        return false;
    }

    public static LoginServerManager getInstance() {
        return instance;
    }

    public String startOfflineRequest(String userName) {
        return virtualOfflineService.startRequest(userName);
    }


    public void startSubRequest(String serverId, GameProfile gameProfile) {
        virtualSubService.startRequest(serverId,gameProfile);
    }

}
