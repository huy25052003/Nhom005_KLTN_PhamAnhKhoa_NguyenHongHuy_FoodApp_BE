FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests=true dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests=true package


FROM eclipse-temurin:21-jre
WORKDIR /app
# 1. FIX OOM: Đặt giới hạn Heap tối đa an toàn (300MB)
ENV JAVA_OPTS="-XX:+UseG1GC -Xmx300m"
EXPOSE 8080
COPY --from=build /app/target/foodapp-server.jar /app/app.jar
ENV SERVER_FORWARD_HEADERS_STRATEGY=framework
# 2. FIX PORT & EXECUTION: Thêm '-Dserver.port=$PORT' và dùng 'exec'
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -Dserver.port=$PORT -Dserver.forward-headers-strategy=$SERVER_FORWARD_HEADERS_STRATEGY -jar /app/app.jar"]