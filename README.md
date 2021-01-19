# AlipayDemo

## [支付宝开放平台(沙箱环境)](https://openhome.alipay.com/platform/appDaily.htm)
蚂蚁沙箱环境 (Beta) 是协助开发者进行接口功能开发及主要功能联调的辅助环境。沙箱环境模拟了开放平台部分产品的主要功能和主要逻辑（当前沙箱支持产品请参考下文的 沙箱支持产品 列表）。 在开发者应用上线审核前，开发者可以根据自身需求，先在沙箱环境中了解、组合和调试各种开放接口，进行开发调试工作，从而帮助开发者在应用上线审核完成后，能更快速、更顺利的完成线上调试和验收。

## [文档](https://docs.open.alipay.com/)
用户需要对应用进行授权登录，应用才能调用支付宝支付接口
1.[授权](https://opendocs.alipay.com/open/284)
2.[支付](https://opendocs.alipay.com/open/270)

## [雪花算法原理](https://github.com/beyondfengyu/SnowFlake)
参考文章：http://www.wolfbe.com/detail/201611/381.html

## SSH 端口转发
```
本地：
ssh -NR <local-host>:<local-port>:<remote -host>:<remote-port> user@host
服务器：
ssh -NR <local-host>:<local-port>:<remote -host>:<remote-port> user@host

例如：
ssh -NR 10000:127.0.0.1:8080 root@forensicschain.com
ssh -NL *:8888:localhost:10000 localhost
访问：
http://forensicschain.com:8888/test
转发：
http://localhost:8080/test

-N 表示只连接远程主机，不打开远程shell
-R 将端口绑定到远程服务器，反向代理
如果不想将反向代理的接口开放，可以本地设置正向代理，开放指定端口
-L 将远程服务器的某个端口开放，并转发到本地机器的指定端口，正向代理

Notice:
修改/etc/ssh/sshd_config
-#GatewayPort no
+GatewayPort yes

systemctl restart sshd重启服务
```

## 沙箱测试alipay.trade.page.pay报错

（1）检查是否登录账户进行请求，建议推出登录进行调用接口请求支付。（如果仍存在问题，可清除浏览器所有痕迹和cookie）。

（2）更换浏览器测试，是否当前浏览器不兼容。

（3）检查沙箱是否维护中，建议等沙箱维护时间过去后再进行测试。

https://opensupport.alipay.com/support/helpcenter/97/201602622792?ant_source=zsearch