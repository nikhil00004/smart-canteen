# Use official Java 17 image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy project files
COPY lib/ /app/lib/
COPY src/ /app/src/
COPY web/ /app/web/
COPY sql/ /app/sql/
COPY uploads/ /app/uploads/

# Create out directory
RUN mkdir -p out

# Compile the project
RUN find src -name "*.java" > sources.txt && \
    javac -cp "lib/*" -d out @sources.txt && \
    rm sources.txt

# Expose port (Render will set PORT env variable)
EXPOSE 8080

# Start the application
CMD ["sh", "-c", "java -cp 'out:lib/*' com.smartcanteen.Main"]