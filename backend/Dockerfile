FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 placemate
COPY --from=build /workspace/target/placemate-api-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads/resumes && chown -R placemate:placemate /app
USER placemate
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
