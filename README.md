# Java TCP Chat App & Custom Pipeline Protocol

A full-duplex, multi-threaded Chat Application built entirely from scratch in Java. This project bypasses standard high-level networking libraries to implement a custom Remote Procedure Call (RPC) pipeline over raw TCP sockets. 

It handles low-level thread synchronization, eliminates read-ahead buffer issues, and utilizes chunked byte-streaming to guarantee memory safety during data transmission. 

The server is containerized with Docker, running entirely headless, while the clients use Java Swing for an interactive, event-driven graphical interface.

## Features
* **Custom TCP Pipeline**: Reliable byte-chunking protocol for flawless data streaming.
* **Thread-Safe Architecture**: Concurrent HashMaps and Thread pooling to avoid race conditions.
* **0% CPU Idle**: Optimized with low-level `wait()` and `notify()` mechanisms instead of busy-loops.
* **Dockerized Server**: The backend is headless and containerized for instant cloud deployment.
* **Java Swing GUI**: Multi-window client interface for seamless user experiences.

## How to Run

### 1. Run the Server (Docker)
Pull the image and run the server on your local machine with port forwarding:
```bash
docker run -p 5050:5050 -p 4040:4040 varnitjaintj/chatapp-server
```

### 2. Run the Client
Compile and execute the client locally. Ensure the server IP is pointing to `localhost` (or your cloud server's IP):
```bash
javac chatapp/core/*.java chatapp/shared/*.java chatapp/client/*.java
java chatapp.client.ChatClientApp
```

## Architecture
- **`chatapp.core`**: The foundational networking engine (`PipeLine2Send`, `PipeLine2Receive`, `Server`, `Client`).
- **`chatapp.server`**: The headless application routing the messages and broadcasting states.
- **`chatapp.client`**: The UI rendering chat histories and online user dashboards.
