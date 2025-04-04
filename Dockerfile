# syntax=docker/dockerfile:1
FROM gradle:8.6.0-jdk21-alpine AS TEMP_BUILD_IMAGE

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src

RUN gradle clean :webapp:shadowJar --no-daemon

# actual container
FROM eclipse-temurin:21-alpine
ENV ARTIFACT_NAME=MDROReport-Light.jar
ENV APP_HOME=/home/gradle/src

WORKDIR $APP_HOME
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/webapp/build/libs/$ARTIFACT_NAME .

ENTRYPOINT java -jar ${ARTIFACT_NAME} $SERVER_URL $USERNAME $PASSWORD $DATABASE