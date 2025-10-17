FROM ghcr.io/navikt/sif-baseimages/java-21:2025.10.17.1307Z
LABEL org.opencontainers.image.source=https://github.com/navikt/ung-deltakelse-opplyser

COPY app/target/lib/*.jar /app/lib/
COPY app/target/*.jar app.jar