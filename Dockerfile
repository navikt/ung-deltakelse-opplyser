FROM amazoncorretto:21-alpine3.20

COPY target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
