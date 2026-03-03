FROM cgr.dev/chainguard/jre:latest
ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/install/pia-brreg/ /app/
ENTRYPOINT ["java", "-cp", "/app/lib/*", "JobKt"]
