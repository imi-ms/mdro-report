# syntax=docker/dockerfile:1
FROM gradle:7.6.0-jdk11-alpine AS TEMP_BUILD_IMAGE

COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src

RUN gradle clean :webapp:shadowJar --no-daemon

# actual container
FROM adoptopenjdk/openjdk11:alpine-jre
ENV ARTIFACT_NAME=MREReport-Light.jar
ENV APP_HOME=/home/gradle/src

WORKDIR $APP_HOME
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/webapp/build/libs/$ARTIFACT_NAME .

ENTRYPOINT java -jar ${ARTIFACT_NAME}