FROM gcr.io/distroless/java17-debian11
ENV TZ="Europe/Oslo"
COPY build/libs/pia-brreg-1.0-SNAPSHOT.jar app.jar
CMD ["app.jar"]
