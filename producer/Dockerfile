FROM openjdk:17
LABEL authors="Andre"
ARG JAR_FILE=out/artifacts/producer_jar/\*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 3000
ENTRYPOINT ["java","-jar","/app.jar"]