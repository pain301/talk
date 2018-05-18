```sh
# Nginx 进程数，按照 CPU 数目指定，一般为它的倍数
worker_processes 8;
# 一个 Nginx 进程打开的最多文件描述符数目
worker_rlimit_nofile 65535;
# 每个进程允许的最多连接数
worker_connections 65535;
# 超时时间
keepalive_timeout 60;
# 客户端请求头部的缓冲区大小
client_header_buffer_size 4k;
# 为打开文件指定缓存，默认是没有启用的，max 指定缓存数量，建议和打开文件数一致，inactive 是指经过多长时间文件没被请求后删除缓存
open_file_cache max=65535 inactive=60s;

# 限制可用方法
if ($request_method !~ ^(GET|HEAD|POST)$ ) {
return 444;
}
# 阻止机器人
if ($http_user_agent ~* LWP::Simple|BBBike|wget) {
return 403;
}
if ($http_user_agent ~* Sosospider|YodaoBot) {
return 403;
}
# 图片盗链
location /images/ {
  valid_referers none blocked www.example.com example.com;
  if ($invalid_referer) {
    return   403;
  }
}
```
```sh
# 阻止来同一个IP的60秒钟内超过15个连接端口80的连接
sbin/iptables -A INPUT -p tcp –dport 80 -i eth0 -m state –state NEW -m recent –set
sbin/iptables -A INPUT -p tcp –dport 80 -i eth0 -m state –state NEW -m recent –update –seconds 60  –hitcount 15 -j DROP
service iptables save
```
Nginx 一般以用户 nginx 运行，但是根目录（/nginx或者/usr/local/nginx/html）不应该设置属于用户 nginx 或对用户 nginx 可写
```sh
# 查找错误权限
find /nginx -user nginx
find /usr/local/nginx/html -user nginx
```
限制 Nginx 连接传出
/sbin/iptables -A OUTPUT -o eth0 -m owner –uid-owner vivek -p tcp –dport 80 -m state –state NEW,ESTABLISHED  -j ACCEPT
