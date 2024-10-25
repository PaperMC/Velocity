package fun.iiii.mixedlogin.yggdrasil.offline;

import fun.iiii.mixedlogin.LoginServerManager;
import fun.iiii.mixedlogin.yggdrasil.YggdrasilRequestObject;
import fun.iiii.mixedlogin.yggdrasil.YggdrasilResultProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualOfflineService implements YggdrasilResultProcessor {
    private final Map<String, String> serverIdReqMap = new ConcurrentHashMap<>();

    @Override
    public void onReceive(YggdrasilRequestObject requestObject) {
        String userName = requestObject.userName();
        String serverId = requestObject.serverId();
        ChannelHandlerContext ctx = requestObject.ctx();

        String genServerId = finishRequest(userName);
        if (genServerId == null || !genServerId.equals(serverId)) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.FORBIDDEN,
                    Unpooled.copiedBuffer("未进行认证", CharsetUtil.UTF_8));

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        // 创建http响应
        String json = "{id:" + generateOfflinePlayerUuid(userName) + ",name:" + userName + "}";
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        // 设置头信息
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
//        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        // 将html write到客户端
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    public static UUID generateOfflinePlayerUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
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
