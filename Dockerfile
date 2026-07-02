FROM eclipse-temurin:17-jdk

# Create working directory
WORKDIR /app

# Copy all the java source files and data.d
COPY chatapp/ /app/chatapp/

# Compile the headless server and dependencies
RUN javac chatapp/core/*.java chatapp/shared/*.java chatapp/server/HeadlessChatServerApp.java

# Expose our custom TCP pipeline ports
EXPOSE 5050
EXPOSE 4040

# Run the headless server
CMD ["java", "chatapp.server.HeadlessChatServerApp"]
