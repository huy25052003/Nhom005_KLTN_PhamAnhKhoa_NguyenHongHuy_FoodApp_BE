
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests=true dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests=true package


FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-XX:+UseG1GC **-Xmx490m**"
EXPOSE 8080
COPY --from=build /app/target/foodapp-server.jar /app/app.jar
ENV SERVER_FORWARD_HEADERS_STRATEGY=framework
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserve.forward-headers-strategy=$SERVER_FORWARD_HEADERS_STRATEGY -jar /app/app.jar"]