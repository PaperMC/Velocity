# MixedVelocity

made by 未冬(QQ:2388990095)

一套离线+正版的VC登入方案，支持VC嵌套，通过AuthME及其配套插件进行二次验证，已确认可用。  
相关代码开源 https://github.com/MixedLogin
## 使用后服务器结构 
玩家统一从MixedVelocity进入  
结构图如下
- MixedVelocity
    - 正常VC服务器
        - 正常服务器1
        - 正常服务器2
    - 登入大厅服务器
## 魔改后特性

1. 支持VC嵌套
2. 允许选择不同的后端信息传递
3. 支持离线和正版混合使用

## 兼容情况
### 版本兼容情况
低于1.19.1:  
域名前加o-或者offline识别为离线玩家  
高于1.19.1:  
直接进入即可
### 启动器兼容情况
[√] HMCL  
[√] PCL  
[√] Vanilla Launcher  
[√] Zalith
## 需要的文件清单
1. [本仓库的本体jar](https://github.com/MixedLogin/MixedLoginVelocity/releases/latest)
2. [MultiLogin-MixedLoginVelocity](https://github.com/MixedLogin/MultiLogin/releases/latest)
3. [AuthMeVelocity-MixedLoginVelocity](https://github.com/MixedLogin/AuthMeVelocity/releases/latest)
4. [Authme插件](https://github.com/AuthMe/AuthMeReloaded/releases/latest)


## 使用方法
注意！！如果您是小白不会使用VC，请先学习如何使用Velocity，本说明不包含基础配置操作，如添加服务器等。
### MixedVelocity服务器配置
#### 安装插件
把下载好的魔改MulitLogin和魔改AuthMeVelocity丢入plugins
#### 配置插件
##### MultiLogin
创建multilogin\service\offline.yml为以下内容
````
id: 1
name: 'Offline'
serviceType: BLESSING_SKIN
yggdrasilAuth:
blessingSkin:
apiRoot: 'http://127.0.0.1:26748/api/yggdrasil'
````
##### AuthMeVelocity
编辑authmevelocity\config.conf  
设定`skin-online-login=true`  
找到并配置登入服务器
````
# List of login/registration servers
auth-servers=[
    login
]
````
#### 配置服务端
编辑mixedvc.toml为以下内容，想diy看注释即可
````
#登录服务器的名字
login-server = "login"
[server-forwarding-mode]
#设定不同服务器的转发模式
login = "modern"
vc = "none"
````
编辑velocity.toml，找到以下段，添加服务器，并删除forced-hosts下全部内容和多余的服务器，配置后效果和下方一致
````
[servers]
# Configure your servers here. Each key represents the server's name, and the value
# represents the IP address of the server to connect to.
login = "127.0.0.1:25578"
vc = "127.0.0.1:30066"

# In what order we should try servers when a player logs in or is kicked from a server.
try = [
    "vc"
]
[forced-hosts]
# Configure your forced hosts here.

````
打开forwarding.secret文件，记下其中的内容，后面要用

### 下层登入大厅设定
#### 安装插件
随便装个AuthMe，无需配置
#### 配置服务端
编辑config\paper-global.yml
找到以下内容，设置成一致的配置，其中<MixedVelocity的forwarding.secret>就是我们刚才看的内容
````
  velocity:
    enabled: true
    online-mode: true
    secret: <MixedVelocity的forwarding.secret>
````
编辑server.properties，设定`online-mode=false`，设定`server-port=25578`

### 下层VC服务器配置
#### 启动脚本配置
为命令行添加以下参数
````
-Dmojang.sessionserver="http://127.0.0.1:26749/api/yggdrasil/sessionserver/session/minecraft/hasJoined"
````
#### 配置服务端
编辑velocity.toml  
设定`player-info-forwarding-mode = "modern"`  
设定`bind = "0.0.0.0:30066"`  
找到以下段，添加服务器play，并删除forced-hosts下全部内容和多余的服务器，配置后效果和下方一致
````
[servers]
# Configure your servers here. Each key represents the server's name, and the value
# represents the IP address of the server to connect to.
play = "127.0.0.1:30067"

# In what order we should try servers when a player logs in or is kicked from a server.
try = [
    "play"
]

[forced-hosts]
# Configure your forced hosts here.

````
打开forwarding.secret文件，记下其中的内容，后面要用
### 正常服务器设定
#### 配置服务端
编辑config\paper-global.yml
找到以下内容，设置成一致的配置，其中<下层VC服务器的forwarding.secret>就是我们刚才看的内容
````
  velocity:
    enabled: true
    online-mode: true
    secret: <下层VC服务器的forwarding.secret>
````
编辑server.properties，设定`online-mode=false`，设定`server-port=30067`
## 看不懂？ 
去群里下载懒人包吧
## 其他
made by 未冬(QQ:2388990095)  
如有问题请提交issue或加QQ群反馈 群号：946864759