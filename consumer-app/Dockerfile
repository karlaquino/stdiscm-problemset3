FROM openjdk:17
LABEL authors="Andre"
ARG JAR_FILE=build/libs/\*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
EXPOSE 12345
ENTRYPOINT ["java","-jar","/app.jar"]