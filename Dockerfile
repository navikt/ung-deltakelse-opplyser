FROM ghcr.io/navikt/sif-baseimages/java-25:2026.06.22.0724Z
LABEL org.opencontainers.image.source=https://github.com/navikt/ung-deltakelse-opplyser

COPY app/target/lib/*.jar /app/lib/
COPY app/target/*.jar app.jar