# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy gradle and project files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Grant execute permission for the gradlew script
RUN chmod +x gradlew

# Copy modules
COPY shared shared
COPY webApp webApp

# Build Wasm static production distribution
RUN ./gradlew :webApp:wasmJsBrowserDistribution --no-daemon

# Stage 2: Web Server stage
FROM nginx:stable-alpine

# Copy compiled static assets (HTML, JS, WASM, CSS) to Nginx html directory
COPY --from=build /app/webApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html

# Expose port 80 (standard HTTP port)
EXPOSE 80

# Run Nginx in the foreground
CMD ["nginx", "-g", "daemon off;"]
