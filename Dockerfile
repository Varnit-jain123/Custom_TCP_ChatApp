FROM eclipse-temurin:17-jdk

# Create working directory
WORKDIR /app

# Copy all the java source files and data.d
COPY chatapp/ /app/chatapp/

# Copy the lib folder containing GSON
COPY lib/ /app/lib/

# Compile the headless server and dependencies with the GSON classpath
RUN javac -cp ".:lib/*" chatapp/core/*.java chatapp/shared/*.java chatapp/server/HeadlessChatServerApp.java

# Expose our custom TCP pipeline ports
EXPOSE 5050
EXPOSE 4040

# Run the headless server with the GSON classpath
CMD ["java", "-cp", ".:lib/*", "chatapp.server.HeadlessChatServerApp"]
