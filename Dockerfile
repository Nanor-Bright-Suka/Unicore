
# Stage 1: Build Jar
# -----------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and config
COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/
COPY pom.xml .

# Copy source code
COPY src ./src

# Build jar (downloads parent and dependencies automatically)
RUN ./mvnw clean package -DskipTests \
    && mv target/*.jar app.jar

# -----------------------------
# Stage 2: Development Image

# -----------------------------
FROM eclipse-temurin:21-jre-jammy AS development
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/app.jar ./app.jar

# Expose app and debugger ports
EXPOSE 8080

# Run the app in dev mode
CMD ["java", "-jar", "app.jar"]



# -----------------------------
# Stage 3: Production Image
# -----------------------------
FROM eclipse-temurin:21-jre-alpine AS production

# Create non-root group and user
RUN addgroup -S nanor \
       && adduser -S nanor -G nanor

WORKDIR /app

# Create uploads folder and set ownership
RUN mkdir -p /app/uploads && chown -R nanor:nanor /app/uploads

# Copy jar from build stage
COPY --from=build /app/app.jar ./app.jar

# Change ownership so spring user can access it
RUN chown nanor:nanor app.jar

# Switch to non-root user
USER nanor

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

