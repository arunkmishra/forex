FROM openjdk:8-jre-alpine

WORKDIR /app

COPY target/scala-2.13/forex-assembly-1.0.1.jar ./app-assembly.jar
COPY src/main/resources/application.conf ./application.conf

ENTRYPOINT ["java", "-Dconfig.file=/app/application.conf", "-jar", "/app/app-assembly.jar"]
