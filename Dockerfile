FROM eclipse-temurin:25-jre

WORKDIR /app

COPY build/libs/chatwoot-spring-0.1.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseCompactObjectHeaders", \
  "-XX:+UseShenandoahGC", \
  "-XX:ShenandoahGCMode=generational", \
  "-Xms256m", \
  "-Xmx512m", \
  "-jar", \
  "app.jar"]
