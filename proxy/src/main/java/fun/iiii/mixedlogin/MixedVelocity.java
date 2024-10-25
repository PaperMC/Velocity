package fun.iiii.mixedlogin;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;

import java.io.IOException;
import java.nio.file.Path;

public class MixedVelocity {
    private final VelocityServer server;
    private final LoginServerManager loginServerManager = new LoginServerManager();
    private MixedVelocityConfig mixedVelocityConfig;
    private static MixedVelocity instance;

    public static MixedVelocity getInstance() {
        return instance;
    }

    public MixedVelocity(VelocityServer server) {
        instance = this;
        this.server = server;
    }

    public MixedVelocityConfig getMixedVelocityConfig() {
        return mixedVelocityConfig;
    }

    public PlayerInfoForwarding getForwardingMode(String serverName) {
        PlayerInfoForwarding playerInfoForwarding = mixedVelocityConfig.getForwardingMode(serverName);
        return playerInfoForwarding == null ? server.getConfiguration().getPlayerInfoForwardingMode() : playerInfoForwarding;
    }

    public void start() {
        loginServerManager.start();

        Path configPath = Path.of("mixedvc.toml");
        try {
            mixedVelocityConfig = MixedVelocityConfig.read(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
