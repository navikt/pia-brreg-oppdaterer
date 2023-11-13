FROM cgr.dev/chainguard/jre:latest
ENV TZ="Europe/Oslo"
COPY build/libs/pia-brreg-1.0-SNAPSHOT.jar app.jar
CMD ["-jar", "app.jar"]
