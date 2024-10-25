# MixedVelocity

made by 未冬(QQ:2388990095)

一套离线+正版的VC登入方案，支持VC嵌套，通过AuthME及其配套插件进行二次验证，已确认可用。
相关代码开源 https://github.com/MixedLogin

## 魔改后特性

1. 支持VC嵌套
2. 允许选择不同的后端信息传递
3. 支持离线和正版混合使用

## 需要的文件清单
offline.yml 为添加到MultiLogin中的yggd服务
````
# Below, only the most basic configuration is provided.
# You can refer to the template file to complete all configurations.

# Please edit before use.
id: 1

name: 'Offline'
# Don't change it unless you really want to.
serviceType: BLESSING_SKIN
yggdrasilAuth:
blessingSkin:
apiRoot: 'http://127.0.0.1:26748/api/yggdrasil'
````
MultiLogin-MixedLoginVelocity 为接入到魔改VC的MultiLogin

MixedVelocity-proxy-3.4.0-SNAPSHOT-all.jar 为魔改VC本体

AuthMeVelocity-MixedVelocity-4.1.2.jar 为魔改过的AuthMEVC，支持跳过正版玩家的登入

bukkit下的Authme和AuthMeVelocity是未经魔改的，可以随意替换

## 使用方法
### 基础安装
plugins放入魔改的MulitLogin以方便多认证源和档案管理，将offline.yml拖入multilogin\services

plugins放入魔改的AuthMeVelocity以支持跳过在线玩家的认证和设定登入后行为
### 兼容情况
低于1.19.1:
域名前加o-或者offline识别为离线玩家

高于1.19.1:
直接进入即可

### 下层VC设定
mixedvc.toml 编辑 其中lobby为下层VC服务器
````
[server-forwarding-mode]
#设定不同服务器的转发模式
lobby = "none"
````
并且下层vc添加以下参数并启用在线模式以确保资料被正确转发
````
-Dmojang.sessionserver="http://127.0.0.1:26749/api/yggdrasil/sessionserver/session/minecraft/hasJoined"
````

### 下层普通服务器设定
mixedvc.toml 编辑 其中login为下层服务器，转发模式自己根据需要写即可
````
[server-forwarding-mode]
#设定不同服务器的转发模式
login = "modern"
````

### 下层登入服务器设定
首先参考下层普通服务器设定，进行设定后装入AuthMe和AuthMeVelocity来进行离线认证

### 其他注意
AuthMeVelocity自行设置
参考设置如下
````
# AuthMeVelocity | by Glyart & 4drian3d

advanced {
    # Enable debug mode
    debug=true
    # Attempts to get a valid server in SendMode Random
    random-attempts=5
}
# List of login/registration servers
auth-servers=[
    login
]
commands {
    # Sets the commands that users who have not yet logged in can execute
    allowed-commands=[
        login,
        register,
        l,
        reg,
        email,
        captcha
    ]
    # Sets the message to send in case a non-logged-in player executes an unauthorized command
    # To deactivate the message, leave it empty
    blocked-message="<red>You cannot execute commands if you are not logged in yet"
}
ensure-auth-server {
    # Ensure that the first server to which players connect is an auth server
    ensure-auth-server=false
    # Selection Mode of the player's initial server
    # TO_FIRST | Send to the first valid server configured
    # TO_EMPTIEST_SERVER | Send to the server with the lowest number of players
    # RANDOM | Send to a random server
    send-mode=TO_FIRST
}
send-on-login {
    # Require players to have the authmevelocity.send-on-login permission?
    require-permission=false
    # Selection Mode of the server to which the player will be sent
    # TO_FIRST | Send to the first valid server configured
    # TO_EMPTIEST_SERVER | Send to the server with the lowest number of players
    # RANDOM | Send to a random server
    send-mode=RANDOM
    # Send logged in players to another server?
    send-on-login=true
    # List of servers to send
    # One of these servers will be chosen at random
    teleport-servers=[
        lobby
    ]
}
````

made by 未冬(QQ:2388990095)

如有问题请提交issue或加QQ群反馈 群号：946864759