FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.19.0715Z
LABEL org.opencontainers.image.source=https://github.com/navikt/ung-deltakelse-opplyser

COPY app/target/lib/*.jar /app/lib/
COPY app/target/*.jar app.jar