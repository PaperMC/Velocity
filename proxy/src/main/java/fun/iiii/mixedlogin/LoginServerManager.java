package fun.iiii.mixedlogin;

import fun.iiii.mixedlogin.yggdrasil.VirtualYggdrasilServer;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoginServerManager {
    private final VirtualYggdrasilServer virtualYggdrasilServer = new VirtualYggdrasilServer(26748, "127.0.0.1");
    private Map<String, String> serverIdReqMap = new ConcurrentHashMap<>();
    private static LoginServerManager instance;

    public void start() {
        instance = this;
        try {
            virtualYggdrasilServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean shouldOfflineHost(Optional<String> hostName) {
        if(hostName.isEmpty())return false;
        String hostNameUnpack=hostName.get();
        if (hostNameUnpack.startsWith("offline")) return true;
        if (hostNameUnpack.startsWith("o-")) return true;
        return false;
    }

    public static LoginServerManager getInstance() {
        return instance;
    }

    public String finishRequest(String userName) {
//        返回一个serverid
        return serverIdReqMap.remove(userName);
    }

    public String startRequest(String userName) {
//        返回一个serverid
        String gen = generateServerId(userName);
        serverIdReqMap.put(userName, gen);
        return gen;
    }

    public static String generateServerId(String userName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(userName.getBytes());
            digest.update(UUID.randomUUID().toString().getBytes());
            return twosComplementHexdigest(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static String twosComplementHexdigest(byte[] digest) {
        return new BigInteger(digest).toString(16);
    }
}
