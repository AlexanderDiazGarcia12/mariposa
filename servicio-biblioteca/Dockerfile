FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

COPY src/ ./src/

RUN ./mvnw clean package -DskipTests -B && \
    cp target/servicio-biblioteca-*.jar target/app.jar


FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S spring && adduser -S spring -G spring && \
    apk add --no-cache wget

WORKDIR /app

COPY --from=builder /build/target/app.jar /app/app.jar

RUN chown -R spring:spring /app

USER spring:spring

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SERVICIO_BIBLIOTECA_PERFIL=docker
ENV SERVICIO_BIBLIOTECA_PUERTO=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
