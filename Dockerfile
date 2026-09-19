FROM eclipse-temurin:25-jre

WORKDIR /app

ARG DD_JAVA_AGENT_VERSION=1.66.0
ADD https://github.com/DataDog/dd-trace-java/releases/download/v${DD_JAVA_AGENT_VERSION}/dd-java-agent.jar /app/dd-java-agent.jar

COPY build/libs/chatwoot-spring-0.1.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-javaagent:/app/dd-java-agent.jar", \
    "-XX:+UseCompactObjectHeaders", \
    "-XX:+UseShenandoahGC", \
    "-XX:ShenandoahGCMode=generational", \
    "-Xms256m", \
    "-Xmx512m", \
    "-jar", \
    "app.jar"]
