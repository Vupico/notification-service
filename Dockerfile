FROM eclipse-temurin:21

ARG MAVEN_SERVER_USERNAME
ENV MAVEN_SERVER_USERNAME=$MAVEN_SERVER_USERNAME

ARG MAVEN_SERVER_PASSWORD
ENV MAVEN_SERVER_PASSWORD=$MAVEN_SERVER_PASSWORD

COPY . /src
WORKDIR /src

ARG APP_PROP
RUN echo "${APP_PROP}" > /src/src/main/resources/application.yml

RUN apt update
RUN apt install -y maven
COPY settings.xml /root/.m2/settings.xml
RUN mvn clean install -X -DskipTests

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar .

ENTRYPOINT ["sh", "-c", "java -javaagent:./opentelemetry-javaagent.jar ${JAVA_OPTS} -jar /src/target/notification-service-0.1.0-SNAPSHOT.jar"]
