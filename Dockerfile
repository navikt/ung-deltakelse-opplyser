FROM ghcr.io/navikt/sif-baseimages/java-21:2026.01.15.0735Z
LABEL org.opencontainers.image.source=https://github.com/navikt/ung-deltakelse-opplyser

COPY app/target/lib/*.jar /app/lib/
COPY app/target/*.jar app.jar