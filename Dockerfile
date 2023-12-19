FROM openjdk:21
MAINTAINER owain@owainlewis.com
COPY target/dispatch-1.0-SNAPSHOT-jar-with-dependencies.jar dispatch.jar
ENTRYPOINT ["java","-jar","/dispatch.jar"]