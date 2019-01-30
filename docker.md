docker info
docker images

# 查看运行容器
docker ps -a
# 停止运行
docker stop <cid>
# 删除运行容器
docker rm <cid>

# 查找镜像
docker search <image name>
# 查看镜像历史版本
docker history <image name>
# 推送镜像
docker push <image name>

# --rm: 进程退出删除容器
# -ti: 分配伪终端进入交互模式
docker run --rm -ti redis /bin/bash

# -d: 后台运行
docker run -d -p 127.0.0.1:5050:6379 --name redis redis
# --name 指定名称为 tomcat
docker run -d -p 5000:5000 --name tomcat tomcat:8.0

# 主机 /tmp/data1 与容器 /tmp/data2 一一对应
docker run -d -ti --volumn /tmp/data1:/tmp/data2 --name redis redis /bin/bash

# 进入容器
docker exec -ti <cid> /bin/bash

# 容器状态保存为镜像
docker commit <cid> <image name>

# 打印容器
docker logs <cid>
