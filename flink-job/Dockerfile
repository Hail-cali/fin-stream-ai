FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle shadowJar --no-daemon

FROM flink:1.18-java17
COPY --from=build /app/build/libs/flink-job.jar /opt/flink/usrlib/flink-job.jar
