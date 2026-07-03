# Custom TCP Chat Pipeline

Welcome to the **Custom TCP Chat Pipeline**! This is a fully functional, multi-threaded Chat Application built entirely from scratch in Java. 

Instead of relying on high-level networking libraries (like WebSockets or Spring Boot) that do all the heavy lifting behind the scenes, this project was designed from the ground up to handle raw TCP sockets directly. It demonstrates a deep understanding of low-level networking, thread safety, and memory management.

## 🚀 The Core Philosophy

When dealing with raw `InputStream` and `OutputStream` in Java, developers often run into the "Read-Ahead Buffer" problem. If you try to mix character reading (like `BufferedReader`) with raw byte reading, bytes get stolen from the stream, corrupting the data.

To solve this, I engineered a **Custom Chunked Byte-Streaming Protocol**. Instead of just throwing data at the socket, this protocol explicitly packages the data into chunks, sends the exact byte length first, and reads exactly that many bytes on the other end. This guarantees that large messages (like images or long texts) never cause memory overflows and never corrupt the stream.

## ✨ Key Technical Features

### 1. Dual-Socket Bi-Directional Pipeline
Every client connection actually establishes *two* independent TCP sockets:
* **Port 5050 (PipeLine2Receive):** Dedicated entirely to listening for incoming bytes.
* **Port 4040 (PipeLine2Send):** Dedicated entirely to transmitting outgoing bytes.
This guarantees true asynchronous, full-duplex communication without the read/write streams ever blocking each other.

### 2. Zero-CPU Idle Threading
Instead of using inefficient `while(true)` busy-loops that consume 100% of your CPU when waiting for messages, the `PipeLine2Send` thread implements low-level Java thread synchronization using `wait()` and `notify()`. The thread goes completely to sleep (0% CPU) until a new message is added to the queue, instantly waking it up to transmit the data.

### 3. Thread-Safe Architecture
Managing multiple clients concurrently requires strict thread safety. The server uses `ConcurrentHashMap` and `HashSet` wrapped in `synchronized` blocks to ensure that when users log in, log out, or send messages, there are absolutely no race conditions or deadlocks.

### 4. Containerized Headless Server (Docker)
Real-world backend servers don't run on monitors. I built a `HeadlessChatServerApp` version of the server that strips away all GUI elements, logging directly to the console. It is fully containerized with Docker, meaning you can deploy it to any cloud provider (AWS, DigitalOcean, etc.) in seconds.

### 5. Unified Java Swing Client UI
The client side features a beautifully designed, single-window UI using Java Swing. 
* **Dynamic Sidebar:** A real-time list of online users on the right side.
* **CardLayout Engine:** When you double-click a user (or when they message you), their chat box seamlessly pops up in the center of the screen without opening annoying new windows. It preserves your entire chat history perfectly as you swap between conversations.

---

## 🛠️ How to Run the Project

### Option A: Run the Server via Docker (Recommended)
You can run the server on any machine without needing to install Java, simply by pulling the Docker image.

```bash
docker run -p 5050:5050 -p 4040:4040 varnitjaintj/chatapp-server:latest
```
*The server will start up silently and listen for incoming connections on ports 5050 and 4040.*

### Option B: Compile and Run Locally
If you want to run the code manually, compile the Java files and run the applications.

**1. Start the Server:**
```bash
javac -cp ".;lib/*" chatapp/core/*.java chatapp/shared/*.java chatapp/server/*.java
java -cp ".;lib/*" chatapp.server.ChatServerApp
```

**2. Start the Client:**
Open a new terminal window to act as a user.
```bash
javac -cp ".;lib/*" chatapp/core/*.java chatapp/shared/*.java chatapp/client/*.java
java -cp ".;lib/*" chatapp.client.ChatClientApp
```
*(By default, the client attempts to connect to `localhost`. If you are hosting the Docker server on a cloud VM, change `localhost` in `ChatClientApp.java` to your VM's public IP address before compiling).*

---

## 📂 Project Architecture
The codebase is modularized like a professional enterprise application:
* **`chatapp.core`**: The low-level networking engine (`PipeLines`, `Server`, `Client`, `Job`). It doesn't know anything about "chatting"—it just guarantees bytes are moved safely.
* **`chatapp.shared`**: Contains `Protocol.java`, defining the string-based flags (like `LOGIN`, `CHAT`, `ONLINE_USERS`) used to route data.
* **`chatapp.server`**: The application layer that authenticates credentials against `data.d`, tracks who is online, and routes messages.
* **`chatapp.client`**: The presentation layer that handles the Swing GUI, user inputs, and renders the chat boxes.

---
*Built from scratch with Java, Sockets, and Threading.*
    