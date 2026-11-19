FROM eclipse-temurin:25.0.5_8-jdk-noble AS probe
WORKDIR /probe
COPY docker/Healthcheck.java .
RUN javac Healthcheck.java

FROM eclipse-temurin:25.0.5_8-jre-noble
WORKDIR /app
COPY --from=probe /probe/Healthcheck.class /app/Healthcheck.class
COPY target/compara-preco-api-0.0.1-SNAPSHOT.jar /app/api.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 CMD ["java", "-cp", "/app", "Healthcheck"]
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/api.jar"]
