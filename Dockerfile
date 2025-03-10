FROM gradle:8.8-jdk21 AS build

WORKDIR /app

COPY . .

RUN gradle build --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

# Create directory for ELF files
RUN mkdir -p ./elf_files

# Copy the built JAR file
COPY --from=build /app/app/build/libs/*.jar app.jar

# Expose the port the app runs on
EXPOSE 9092

# Command to run the application
CMD ["java", "-jar", "app.jar"]