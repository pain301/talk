https://my.oschina.net/feinik/blog/1590941?hmsr=toutiao.io&utm_medium=toutiao.io&utm_source=toutiao.io
https://mp.weixin.qq.com/s/nMd9oGLLohMK-junStiTdw
### 版本
```sh
cat /proc/version
uname -a
```
```sh
lsb_release -a
cat /etc/issue
cat /etc/redhat-release
```

### 添加用户
```sh
adduser pain
passwd pain
gpasswd -a pain wheel

adduser www
```

### 安装依赖
```sh
#!/bin/bash

set -e

# Add CentOS 7 EPEL repo
yum -y install epel-release
yum -y update

cat <<PACKAGES | xargs yum -y install
git
nc
wget
curl
unzip

gcc
gcc-c++
autoconf
automake

mhash
mhash-devel
mcrypt
libmcrypt-devel
libxml2
libxml2-devel
libjpeg
libjpeg-devel
libpng
libpng-devel
freetype
freetype-devel
libXpm-devel
gmp-devel
mariadb-devel
xmlrpc-c
xmlrpc-c-devel
zlib
zlib-devel
glibc
glibc-devel
glib2
glib2-devel
bzip2
bzip2-devel
libjpeg
libjpeg-devel
openssl
openssl-devel
curl-devel
gdbm-devel
gd-devel
libc-client-devel
python
python-pip

nodejs

geoip
PACKAGES
```

```sh
systemctl list-units --type=service
```

### PHP 环境
```sh
#!/bin/bash

set -e

wget http://am1.php.net/distributions/php-5.6.24.tar.gz
tar -xzvf php-5.6.24.tar.gz
cd ./php-5.6.24/

./configure --enable-bcmath \
--enable-calendar \
--with-imap \
--with-imap-ssl \
--with-kerberos \
--enable-mbstring \
--with-mcrypt \
--with-mhash \
--with-mysql \
--with-openssl \
--with-pcre-regex \
--with-pdo-mysql \
--with-regex \
--enable-sysvsem \
--enable-sysvshm \
--enable-sysvmsg \
--enable-sockets \
--with-libdir=lib64 \
--enable-inline-optimization \
--enable-mbregex \
--enable-opcache \
--enable-fpm \
--with-bz2 \
--with-zlib-dir \
--enable-zip \
--with-zlib \
--enable-soap \
--with-gd \
--enable-bcmath \
--with-curl \
--disable-debug \
--prefix=/usr/local/php \
--with-config-file-path=/usr/local/php/etc

make && make install
```

```sh
#!/bin/bash

set -e

cd /usr/local/php/etc
mkdir -m 0755 conf.d
mkdir -m 0755 fpm.d

echo "PATH=$PATH:/usr/local/php/bin:/usr/local/php/sbin" >> /etc/profile

source /etc/profile

cat > /usr/lib/systemd/system/php-fpm.service <<SERVICE
[Unit]
Description=The PHP FastCGI Process Manager
After=syslog.target network.target

[Service]
Type=simple
PIDFile=/run/php-fpm.pid
ExecStart=/usr/local/php/sbin/php-fpm --nodaemonize --fpm-config /usr/local/php/etc/php-fpm.conf
ExecReload=/bin/kill -USR2 $MAINPID

[Install]
WantedBy=multi-user.target
SERVICE
```
composer
```sh
EXPECTED_SIGNATURE=$(wget -q -O - https://composer.github.io/installer.sig)
php -r "copy('https://getcomposer.org/installer', 'composer-setup.php');"
ACTUAL_SIGNATURE=$(php -r "echo hash_file('SHA384', 'composer-setup.php');")

if [ "$EXPECTED_SIGNATURE" != "$ACTUAL_SIGNATURE" ]
then
    >&2 echo 'ERROR: Invalid installer signature'
    rm composer-setup.php
    exit 1
fi

php composer-setup.php --quiet --install-dir=/usr/local/bin --filename=composer
RESULT=$?
rm composer-setup.php
exit $RESULT
```

php-fpm.conf
```
[global]
pid = /run/php-fpm.pid
error_log = log/php-fpm.log
emergency_restart_threshold = 60
emergency_restart_interval = 60
events.mechanism = epoll

[www]
user = www
group = www
listen = 127.0.0.1:9000
pm = dynamic
pm.max_children = 40
pm.start_servers = 4
pm.min_spare_servers = 2
pm.max_spare_servers = 20
pm.max_requests = 1000
```

php.ini
```
expose_php = Off
display_errors = On
display_startup_errors = On
extension_dir = "/usr/local/php/lib/php/extensions/no-debug-non-zts-20131226"
upload_max_filesize = 16M

extension=zip.so
extension=phalcon.so
extension=redis.so
extension=mongodb.so
zend_extension=opcache.so

date.timezone = UTC
session.name = SESSID
opcache.enable=0
opcache.enable_cli=0
opcache.memory_consumption=128
opcache.interned_strings_buffer=8
opcache.max_accelerated_files=4000
opcache.revalidate_freq=60
opcache.save_comments=0
opcache.fast_shutdown=1
```

```sh
systemctl enable php-fpm
systemctl start php-fpm
```

### nginx 环境
```sh
sudo yum -y install nginx
```

/etc/nginx/nginx.conf
```sh
user www www;
worker_processes auto;
worker_rlimit_nofile 204800;
pid /run/nginx.pid;

error_log /var/log/nginx/error.log;

events {
    use epoll;
    worker_connections 10240;
}

http {
    include             /etc/nginx/mime.types;
    default_type        application/octet-stream;

    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;


    server_tokens off;

    sendfile            on;
    tcp_nopush          on;
    tcp_nodelay         on;
    keepalive_timeout   65;

    client_header_buffer_size 16k;
    large_client_header_buffers 16 32k;
    client_body_buffer_size 128k;
    client_max_body_size 10m;

    gzip on;
    gzip_disable "msie6";
    gzip_vary on;
    gzip_min_length 1k;
    gzip_buffers 128 32k;
    gzip_comp_level 4;
    gzip_proxied any;
    gzip_types text/plain application/xml application/javascript text/css application/x-javascript;

    include /etc/nginx/conf.d/*.conf;
    server {
        listen       10443;
        server_name  _;
        root         /usr/share/nginx/html;

        include /etc/nginx/default.d/*.conf;

        location / {
        }

        error_page 404 /404.html;
        location = /40x.html {
        }

        error_page 500 502 503 504 /50x.html;
        location = /50x.html {
        }
    }
}
```
/etc/nginx/conf.d/lms.seraphic-corp.com.conf
```sh
server {
    # it should be set as default for SSL
    listen      443 default_server ssl;
    server_name lms.seraphic-corp.com;
    root        /srv/www/lms.seraphic-corp.com/public;
    index       index.php index.html;

    access_log  /var/log/nginx/access-lms.seraphic-corp.com.log;

    ssl_certificate /etc/nginx/ssl/seraphic-corp.com.crt;
    ssl_certificate_key /etc/nginx/ssl/seraphic-corp.com.key;
    ssl_session_timeout 50m;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;

    ssl_protocols   TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers     ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:DHE-RSA-AES256-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!3DES:!MD5:!PSK;
    ssl_prefer_server_ciphers on;

    #ssl_stapling on;
    #ssl_stapling_verify on;

    location / {
        try_files $uri $uri/ /index.php?$args;
    }

    location ~ \.php$ {
        try_files $uri =404;
        fastcgi_pass  127.0.0.1:9000;
        fastcgi_index index.php;

        include fastcgi_params;
        fastcgi_split_path_info       ^(.+\.php)(/.+)$;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
    }

    location ~ /\.ht {
        deny all;
    }
}
```
/etc/nginx/conf.d/admin-lms.seraphic.com.conf
```sh
server {
    listen      443 ssl;
    server_name admin-lms.seraphic.com;
    root        /srv/www/admin-lms.seraphic.com/public;
    index       index.php index.html;

    access_log  /var/log/nginx/access-admin-lms.seraphic.com.log;

    ssl_certificate /etc/nginx/ssl/seraphic-corp.com.crt;
    ssl_certificate_key /etc/nginx/ssl/seraphic-corp.com.key;
    ssl_session_timeout 50m;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;

    ssl_protocols   TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers     ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-DSS-AES128-GCM-SHA256:kEDH+AESGCM:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA:ECDHE-ECDSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-DSS-AES128-SHA256:DHE-RSA-AES256-SHA256:DHE-DSS-AES256-SHA:DHE-RSA-AES256-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!3DES:!MD5:!PSK;
    ssl_prefer_server_ciphers on;

    #ssl_stapling on;
    #ssl_stapling_verify on;

    location / {
        try_files $uri $uri/ /index.php?$args;
    }

    location ~ \.php$ {
        try_files $uri =404;
        fastcgi_pass  127.0.0.1:9000;
        fastcgi_index index.php;

        include fastcgi_params;
        fastcgi_split_path_info       ^(.+\.php)(/.+)$;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
    }

    location ~ /\.ht {
        deny all;
    }
}
```

```sh
# 生成 .key
openssl genrsa -des3 -out ssl.tmp.key 2048
openssl rsa -in ssl.tmp.key -out ssl.key
# 生成 .crt
openssl req -new -key ssl.key -out ssl.csr
openssl x509 -req -days 365 -in ssl.csr -signkey ssl.key -out ssl.crt

sudo mkdir /etc/nginx/ssl
```

```
ssl_certificate /etc/nginx/ssl/ssl.crt;
ssl_certificate_key /etc/nginx/ssl/ssl.key;
ssl_session_cache shared:SSL:20m;
ssl_session_timeout 60m;
ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
```
header
Strict-Transport-Security

```sh
sudo systemctl enable nginx
sudo systemctl start nginx
```

### mysql 环境
```sh
yum -y install mariadb-server
```
/etc/my.cnf
```sh
[mysqld]
character-set-server=utf8
datadir=/var/lib/mysql
socket=/var/lib/mysql/mysql.sock

max_allowed_packet=100M

back_log=128
max_connections=1000
max_user_connections=800

thread_concurrency=24
thread_cache_size=100

skip-name-resolve

# Disabling symbolic-links is recommended to prevent assorted security risks
symbolic-links=0
# Settings user and group are ignored when systemd is used.
# If you need to run mysqld under a different user or group,
# customize your systemd unit file for mariadb according to the
# instructions in http://fedoraproject.org/wiki/Systemd

[mysqld_safe]
log-error=/var/log/mariadb/mariadb.log
pid-file=/var/run/mariadb/mariadb.pid

#
# include all files from the config directory
#
!includedir /etc/my.cnf.d
```

```sh
# 初始化
mysql_secure_installation
systemctl enable mariadb
systemctl start mariadb
```

```sh
source ~/data.sql
```

### redis
```sh
yum -y install redis
systemctl enable redis
systemctl start redis
```

### beanstalkd
```sh
yum -y install beanstalkd
systemctl enable beanstalkd
systemctl start beanstalkd
```

### supervisor
```sh
pip install supervisor
```

```sh
set -e

mkdir -m 0766 /etc/supervisord.d
mkdir -m 0776 /var/log/supervisor

cat > /etc/supervisord.conf <<SUPERVISOR
; Sample supervisor config file.

[unix_http_server]
file=/run/supervisor.sock   ; (the path to the socket file)

[supervisord]
logfile=/var/log/supervisor/supervisord.log  ; (main log file;default $CWD/supervisord.log)
logfile_maxbytes=50MB       ; (max main logfile bytes b4 rotation;default 50MB)
logfile_backups=10          ; (num of main logfile rotation backups;default 10)
loglevel=info               ; (log level;default info; others: debug,warn,trace)
pidfile=/run/supervisord.pid ; (supervisord pidfile;default supervisord.pid)
nodaemon=false              ; (start in foreground if true;default false)
minfds=1024                 ; (min. avail startup file descriptors;default 1024)
minprocs=200                ; (min. avail process descriptors;default 200)
;umask=022                  ; (process file creation umask;default 022)
user=root                 ; (default is current user, required if root)
;identifier=supervisor       ; (supervisord identifier, default is 'supervisor')
;directory=/tmp              ; (default is not to cd during start)
;nocleanup=true              ; (don't clean up tempfiles at start;default false)
;childlogdir=/tmp            ; ('AUTO' child log dir, default $TEMP)
;environment=KEY=value       ; (key value pairs to add to environment)
;strip_ansi=false            ; (strip ansi escape codes in logs; def. false)

; the below section must remain in the config file for RPC
; (supervisorctl/web interface) to work, additional interfaces may be
; added by defining them in separate rpcinterface: sections
[rpcinterface:supervisor]
supervisor.rpcinterface_factory = supervisor.rpcinterface:make_main_rpcinterface

[supervisorctl]
serverurl=unix:///run/supervisor.sock ; use a unix:// URL  for a unix socket

[include]
files = supervisord.d/*.ini
SUPERVISOR

cat > /etc/supervisord.d/laravel-worker.ini <<WORKER
[program:laravel-worker]
command=/usr/local/php/bin/php /srv/www/admin-lms.seraphic.com/artisan queue:work --sleep=3 --tries=3 --daemon
directory=/srv/www/admin-lms.seraphic.com
process_name=%(program_name)s_%(process_num)02d
autostart=true
autorestart=true
user=www
numprocs=4
redirect_stderr=true
stdout_logfile=/var/log/supervisor/laravel-worker.log
WORKER

cat > /usr/lib/systemd/system/supervisord.service <<SERVICE
# supervisord service for sysstemd (CentOS 7.0+)
# by ET-CS (https://github.com/ET-CS)

[Unit]
Description=Supervisor daemon

[Service]
Type=forking
ExecStart=/usr/bin/supervisord -c /etc/supervisord.conf
ExecStop=/usr/bin/supervisorctl $OPTIONS shutdown
ExecReload=/usr/bin/supervisorctl $OPTIONS reload
KillMode=process
Restart=on-failure
RestartSec=42s

[Install]
WantedBy=multi-user.target
SERVICE
```

```sh
systemctl enable supervisord
systemctl start supervisord
supervisorctl status all
```

### 应用依赖安装
```sh
sudo env "PATH=$PATH" composer install
php artisan key:generate
```

### 定时任务
/etc/cron.d/lms
```sh
* * * * * www /usr/local/php/bin/php /srv/www/admin-lms.seraphic.com/artisan schedule:run >> /dev/null 2>&1
```
```sh
service crond restart
```

### 前端代码编译
node 6.10.2
npm 3.10.10
```sh
npm install -g gulp
npm install
gulp build
```

### 禁用 SELinux
```sh
setenforce 0
```
```sh
setenforce 1
getenforce
sestatus
cat /etc/sysconfig/selinux
```

### 防火墙
```sh
firewall-cmd --state
firewall-cmd --list-all
firewall-cmd --get-services
firewall-cmd --list-ports

firewall-cmd --get-zones
firewall-cmd --get-default-zone
firewall-cmd --set-default-zone=internal
firewall-cmd --get-active-zones
firewall-cmd --list-all-zones
systemctl status firewalld
```

```sh
firewall-cmd --reload
systemctl restart firewalld
systemctl enable firewalld
systemctl disable firewalld
systemctl start firewalld
systemctl stop firewalld
```

```sh
# Services are simply collections of ports with an associated name and description.
firewall-cmd --permanent --add-service=https
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --remove-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --permanent --add-port=4990-4999/udp
firewall-cmd --permanent --remove-service=http
```

```sh
# add the rule to both the permanent and runtime sets
firewall-cmd --zone=public --add-service=http --permanent
firewall-cmd --zone=public --add-service=http
```

```sh
firewall-cmd --zone=public --add-service=http --permanent
firewall-cmd --reload
```

```sh
firewall-cmd --zone=public --add-rich-rule 'rule family="ipv4" source address=192.168.0.14 accept'
firewall-cmd --zone=public --add-rich-rule 'rule family="ipv4" source address="192.168.1.10" port port=22 protocol=tcp reject'
```

#### Construct Ruleset
```sh
firewall-cmd --set-default-zone=dmz
firewall-cmd --zone=dmz --add-interface=eth0

firewall-cmd --zone=dmz --add-service=http --permanent
firewall-cmd --zone=dmz --add-service=https --permanent
firewall-cmd --reload
```
