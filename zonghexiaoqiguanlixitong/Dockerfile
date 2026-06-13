# ---- Stage 1: Build ----
FROM maven:3.8-openjdk-8 AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:resolve -B

# Copy source and build with prod profile (includes frontend build via frontend-maven-plugin)
COPY src ./src
RUN mvn package -Pprod -DskipTests -B

# ---- Stage 2: Runtime ----
FROM openjdk:8-jre-slim
WORKDIR /app

COPY --from=build /app/target/zonghexiaoqiguanlixitong-0.0.1-SNAPSHOT.jar app.jar

# Create upload directory for mounted volume
RUN mkdir -p /data/upload

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
