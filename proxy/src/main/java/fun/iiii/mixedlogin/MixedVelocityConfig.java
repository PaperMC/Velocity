package fun.iiii.mixedlogin;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MixedVelocityConfig {

    private final ForwardingModes forwardingModes;
    private final String loginServerName;

    private MixedVelocityConfig(ForwardingModes forwardingModes, String loginServerName) {
        this.forwardingModes = forwardingModes;
        this.loginServerName = loginServerName;
    }

    public String getLoginServerName() {
        return loginServerName;
    }

    public PlayerInfoForwarding getForwardingMode(String serverName) {
        return forwardingModes.modes.get(serverName);
    }

    public static MixedVelocityConfig read(Path path) throws IOException {
        URL defaultConfigLocation = VelocityConfiguration.class.getClassLoader()
                .getResource("default-mixedvc.toml");
        if (defaultConfigLocation == null) {
            throw new RuntimeException("Default configuration file does not exist.");
        }

        try (final CommentedFileConfig config = CommentedFileConfig.builder(path)
                .defaultData(defaultConfigLocation)
                .autosave()
                .preserveInsertionOrder()
                .sync()
                .build()
        ) {
            config.load();
            final String loginServerName = config.getOrElse("login-server", "login");

            final CommentedConfig forwardingModesConfig = config.get("server-forwarding-mode");
            ForwardingModes modes = new ForwardingModes(forwardingModesConfig);
            return new MixedVelocityConfig(modes, loginServerName);

        }
    }


    private static class ForwardingModes {

        private Map<String, PlayerInfoForwarding> modes = ImmutableMap.of(
                "login", PlayerInfoForwarding.MODERN
        );

        private ForwardingModes() {
        }

        private ForwardingModes(CommentedConfig config) {
            if (config != null) {
                Map<String, PlayerInfoForwarding> servers = new HashMap<>();
                for (UnmodifiableConfig.Entry entry : config.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        servers.put(cleanServerName(entry.getKey()), PlayerInfoForwarding.valueOf(((String) entry.getValue()).toUpperCase()));
                    }
                }
                this.modes = ImmutableMap.copyOf(servers);
            }
        }

        private ForwardingModes(Map<String, PlayerInfoForwarding> modes) {
            this.modes = modes;
        }

        private Map<String, PlayerInfoForwarding> getModes() {
            return modes;
        }

        public void setModes(Map<String, PlayerInfoForwarding> modes) {
            this.modes = modes;
        }

        private String cleanServerName(String name) {
            return name.replace("\"", "");
        }
    }

}
