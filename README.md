# 🚀 Custom TCP Chat Architecture

Welcome to the **Custom TCP Chat Architecture**! This is a fully functional, multi-threaded, and cloud-hosted Chat Application built entirely from scratch in Java.

Instead of relying on high-level networking libraries (like WebSockets, Netty, or Spring Boot) that hide the heavy lifting, this project was designed from the ground up to handle raw TCP sockets directly. It demonstrates a deep understanding of low-level networking, thread synchronization, memory management, and modern protocol design.

## ✨ The Core Philosophy

When dealing with raw `InputStream` and `OutputStream` in Java, developers often run into the "Read-Ahead Buffer" problem. If you mix character reading with raw byte reading, bytes get stolen from the stream, corrupting the data.

To solve this, I engineered a **Custom Chunked Byte-Streaming Protocol**. Instead of just throwing data at the socket, this protocol explicitly packages the data into chunks, sends the exact byte length first (`length#`), and reads exactly that many bytes on the other end. This guarantees that large messages (like images or long texts) never cause memory overflows and never corrupt the stream.

## 🛠️ Key Technical Features

### 1. Dual-Socket Bi-Directional Pipeline
Every client connection actually establishes *two* independent TCP sockets:
* **Port 5050 (PipeLine2Receive):** Dedicated entirely to listening for incoming bytes.
* **Port 4040 (PipeLine2Send):** Dedicated entirely to transmitting outgoing bytes.
This guarantees true asynchronous, full-duplex communication without the read/write streams ever blocking each other.

### 2. GSON Data Serialization
The application protocol has been modernized from legacy string-concatenation to a fully structured JSON architecture. Every network payload is encapsulated in a strongly typed `Message` object and serialized seamlessly using Google's **Gson** library. 

### 3. Zero-CPU Idle Threading
Instead of using inefficient `while(true)` busy-loops that consume 100% of your CPU when waiting for messages, the `PipeLine2Send` thread implements low-level Java thread synchronization using `wait()` and `notify()`. The thread goes completely to sleep (0% CPU) until a new message is added to the queue, instantly waking it up to transmit the data.

### 4. Containerized Cloud Server (Docker & AWS)
Real-world backend servers don't run on local monitors. I built a `HeadlessChatServerApp` version of the server that strips away all GUI elements, logging directly to the console. It is fully containerized with Docker, meaning it can be deployed to AWS, DigitalOcean, or any cloud provider in seconds.

### 5. Unified Java Swing Client UI
The client side features a beautifully designed, single-window UI using Java Swing. 
* **Dynamic Sidebar:** A real-time list of online users on the right side.
* **CardLayout Engine:** When you double-click a user (or when they message you), their chat box seamlessly pops up in the center of the screen without opening annoying new windows. It preserves your entire chat history perfectly as you swap between conversations.

---

## ☁️ Live on the Cloud (AWS)

This project isn't just local—it is **currently deployed and live on the AWS Cloud!** 

The headless server has been containerized with Docker and is actively running on an Amazon EC2 instance. This means that if you download the client code and enter the public IP address, you can seamlessly connect and chat with anyone else in the world, in real-time, with zero latency. 

It is a fully realized, production-ready, global architecture!

---

## 💻 How to Run Locally

If you want to run the code manually to test changes or host the server on your own computer, you have two options for the server:

### 1. Start the Server (via Docker)
The easiest way to run the server locally is to just pull the Docker image!
```powershell
docker run -p 5050:5050 -p 4040:4040 varnitjaintj/chatapp-server:latest
```

### 2. Start the Server (via Source Code)
If you'd rather compile the code manually to see the GUI server version:
```powershell
javac -cp ".;lib/*" chatapp/core/*.java chatapp/shared/*.java chatapp/server/*.java
java -cp ".;lib/*" chatapp.server.ChatServerApp
```

**2. Start the Client:**
Open a new terminal window to act as a user.
```powershell
javac -cp ".;lib/*" chatapp/core/*.java chatapp/shared/*.java chatapp/client/*.java
java -cp ".;lib/*" chatapp.client.ChatClientApp
```

*(By default, the client attempts to connect to `localhost`. If you are hosting the Docker server on your AWS cloud VM, make sure to change `"localhost"` in `ChatClientApp.java` to your VM's public IP address before compiling).*

---

## 📂 Project Architecture

The codebase is modularized like a professional enterprise application:
* **`chatapp.core`**: The low-level networking engine (`PipeLines`, `Server`, `Client`, `Job`). It doesn't know anything about "chatting"—it just guarantees bytes are moved safely.
* **`chatapp.shared`**: Contains the protocol definitions and the JSON `Message` class used to route data cleanly.
* **`chatapp.server`**: The application layer that authenticates credentials against `data.d`, tracks who is online, and routes messages.
* **`chatapp.client`**: The presentation layer that handles the Swing GUI, user inputs, and renders the chat boxes seamlessly on the EDT.

---
*Built from scratch with Java, Sockets, and Threading.*