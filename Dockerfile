FROM gradle:7.3.0
EXPOSE 12345/tcp
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon
CMD ["./gradlew", "run"]