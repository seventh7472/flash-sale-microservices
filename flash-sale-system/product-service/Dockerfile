FROM azul/zulu-openjdk:21
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8102
ENTRYPOINT ["java", "-jar", "app.jar"]