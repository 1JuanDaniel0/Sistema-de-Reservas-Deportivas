FROM openjdk:25-ea-17-jdk
VOLUME /tmp
EXPOSE 8080
ADD ./target/project-0.0.1-SNAPSHOT.jar proyecto.jar
ENTRYPOINT ["java", "-jar", "proyecto.jar"]