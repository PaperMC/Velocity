# MixedVelocity

made by 未冬(QQ:2388990095)

一套离线+正版的VC登入方案，支持VC嵌套，由于MultiLogin未添加进入后二次密码认证，目前仅实验用途，不建议用于生产

相关代码开源 https://github.com/MixedLogin

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

## 使用方法

启动魔改VC后安装ML，将offline.yml拖入multilogin\services

低于1.19.1:
域名前加o-或者offical识别为离线玩家

高于1.19.1:
直接进入即可

如果下层要嵌套VC

设置mixedVC
````
player-info-forwarding-mode = "none"
````
并且下层vc添加以下参数并启用在线模式以确保资料被正确转发
````
-Dmojang.sessionserver="http://127.0.0.1:26749/api/yggdrasil/sessionserver/session/minecraft/hasJoined"
````

普通服务器可以使用常规UUID转发办法

made by 未冬(QQ:2388990095)