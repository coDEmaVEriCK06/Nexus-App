# ---- build stage: compile and package the boot jar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---- runtime stage: small JRE image, non-root ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system nexus && useradd --system --gid nexus nexus
COPY --from=build /app/target/nexus-1.0.0.jar app.jar
USER nexus
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
