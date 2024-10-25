package fun.iiii.mixedlogin.yggdrasil.offline;

import com.velocitypowered.api.util.GameProfile;
import fun.iiii.mixedlogin.yggdrasil.YggdrasilRequestObject;
import fun.iiii.mixedlogin.yggdrasil.YggdrasilResultProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;

public class VirtualSubService implements YggdrasilResultProcessor {

    private final Map<String, GameProfile> serverIdReqMap = new ConcurrentHashMap<>();

    @Override
    public void onReceive(YggdrasilRequestObject requestObject) {
        String userName = requestObject.userName();
        String serverId = requestObject.serverId();
        ChannelHandlerContext ctx = requestObject.ctx();

        GameProfile gameProfile = finishRequest(serverId);
        if (gameProfile == null) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.FORBIDDEN,
                    Unpooled.copiedBuffer("无效验证", CharsetUtil.UTF_8));

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        String json = GENERAL_GSON.toJson(gameProfile);
        // 创建http响应
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        // 设置头信息
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        //response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        // 将html write到客户端
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public GameProfile finishRequest(String serverId) {
//        返回一个serverid
        return serverIdReqMap.remove(serverId);
    }

    public void startRequest(String serverId, GameProfile gameProfile) {
//        返回一个serverid
        serverIdReqMap.put(serverId, gameProfile);
    }

}
