FROM openjdk:8-jre-slim

LABEL maintainer="dev-team"

RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 上传文件持久化目录
RUN mkdir -p /data/uploads
VOLUME ["/data/uploads"]

COPY target/*.jar app.jar

# 入口脚本: 首次启动时将 JAR 内的 Excel 模板复制到持久化卷
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
