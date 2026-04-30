FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
COPY ./build/libs/varsler-*.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
