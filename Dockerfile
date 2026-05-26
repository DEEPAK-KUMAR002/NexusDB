# Use a lightweight Java 11 JDK image
FROM openjdk:11-jdk-slim

# Set the working directory
WORKDIR /app

# Copy all project files into the Docker container
COPY . /app

# Compile the Java source code
RUN javac src/vectordb/*.java

# Expose port 8080 (the default port)
EXPOSE 8080

# Command to run the application
CMD ["java", "-cp", "src", "vectordb.Main"]
