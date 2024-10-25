package fun.iiii.mixedlogin.yggdrasil;

import io.netty.channel.ChannelHandlerContext;

public record YggdrasilRequestObject(String userName, String serverId, String ip, ChannelHandlerContext ctx) {
}
