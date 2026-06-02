FROM eclipse-temurin:21-jdk-alpine
VOLUME /tmp
COPY platform-user/target/platform-user-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]