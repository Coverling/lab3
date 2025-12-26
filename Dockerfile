FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Копируем настройки Maven с зеркалом
COPY maven-settings.xml /root/.m2/settings.xml

COPY pom.xml .
# Используем custom settings и добавляем retry логику
RUN mvn dependency:resolve -s /root/.m2/settings.xml || \
    mvn dependency:resolve -s /root/.m2/settings.xml || \
    mvn dependency:resolve -s /root/.m2/settings.xml

COPY src ./src
RUN mvn clean package -DskipTests -s /root/.m2/settings.xml

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/notification-service-*.jar app.jar

# Создаем папку для JFR файлов
RUN mkdir -p /profiling && chmod 777 /profiling

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher --version || exit 1

# Используем shell форму для поддержки JAVA_OPTS
ENTRYPOINT sh -c "java $JAVA_OPTS -jar app.jar"
