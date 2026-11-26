FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.26.0840Z
LABEL org.opencontainers.image.source=https://github.com/navikt/ung-deltakelse-opplyser

COPY app/target/lib/*.jar /app/lib/
COPY app/target/*.jar app.jar