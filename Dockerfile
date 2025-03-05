FROM ghcr.io/navikt/sif-baseimages/java-21:2025.02.13.1522Z
LABEL org.opencontainers.image.source=https://github.com/navikt/ung-deltakelse-opplyser

COPY target/lib/*.jar lib/
COPY target/*.jar app.jar
