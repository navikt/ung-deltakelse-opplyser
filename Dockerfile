FROM amazoncorretto:23-alpine3.20

COPY target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
