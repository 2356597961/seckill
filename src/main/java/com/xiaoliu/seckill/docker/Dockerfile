# 指定基础镜像
# 你的开发环境是jdk8,就使用下面命令
FROM openjdk:8-jdk-alpine
# openjdk9 在18年3月分开始不支持了, 支持从11开始，如果说你使用jdk1.9，则使用jdk14
#FROM openjdk:14.0-jdk

# 数据卷，宿主机文件夹挂载到容器
VOLUME /tmp

# 环境变量
# 在创建容器时使用 -e 可改变值，如 -e NACOS_SERVER_ADDR=39.108.187.100:8848 -e SPRING_PROFILES_ACTIVE=dev
# nacos服务器地址
#ENV NACOS_SERVER_ADDR=182.61.134.185:8848
# 项目环境变量
ENV SPRING_PROFILES_ACTIVE=dev
# 可以在运行的时候指定运行的内存（使用 docker run -it -d --restart unless-stopped
# -p 8001:8001 --net host  -e CE="dev" -e JAVA_OPTS="-Xmx128m -Xss512m" ）
#ENV JAVA_OPTS=$JAVA_OPTS
# 复制文件
ADD xiaoliuSeckill_*.jar app.jar

# Docker容器启动时执行的命令，注意：是 -D 开头。如果有换行，后面要加上英文状态的 \ 进行连接
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "/app.jar"]
