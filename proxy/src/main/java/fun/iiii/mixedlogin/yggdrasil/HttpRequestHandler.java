package fun.iiii.mixedlogin.yggdrasil;

import fun.iiii.mixedlogin.LoginServerManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final YggdrasilResultProcessor processor;

    public HttpRequestHandler(YggdrasilResultProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        //100 Continue
        if (is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.CONTINUE));
        }
        // 获取请求的uri
        String uri = req.uri();
        String[] firstSplitURL = uri.split("\\?");
        if (firstSplitURL.length < 2) {
            return;
        }
        if (!firstSplitURL[0].equals("/api/yggdrasil/sessionserver/session/minecraft/hasJoined")) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.NOT_FOUND,
                    Unpooled.copiedBuffer("404 Not Found", CharsetUtil.UTF_8));

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        Map<String, String> resMap = new HashMap<>();
        resMap.put("url", firstSplitURL[0]);
        String[] secondSplitURL = firstSplitURL[1].split("&");
        for (String s : secondSplitURL) {
            String[] thirdSplitURL = s.split("=");
            resMap.put(thirdSplitURL[0], thirdSplitURL[1]);
        }

        resMap.put("method", req.method().name());

        String userName = resMap.get("username");
        String serverId = resMap.get("serverId");
        String ip = resMap.get("ip");
        YggdrasilRequestObject requestObject = new YggdrasilRequestObject(userName, serverId,ip,ctx);
        processor.onReceive(requestObject);

    }

}
