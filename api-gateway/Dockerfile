# Multi-stage build: build frontend, then package gateway JAR containing static assets
FROM node:20-alpine AS frontend-build
WORKDIR /frontend
COPY ../../frontend/package*.json ./
RUN npm install --no-audit --no-fund
COPY ../../frontend .
RUN npm run build

FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml api-gateway/pom.xml
COPY src api-gateway/src
# Copy built frontend into gateway static resources
COPY --from=frontend-build /frontend/dist api-gateway/src/main/resources/static
RUN mvn -q -f api-gateway/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
