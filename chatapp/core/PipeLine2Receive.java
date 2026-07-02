package chatapp.core;
import java.io.*;
import java.net.*;

public class PipeLine2Receive extends Thread {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String clientId;
    private boolean clientConnected;
    private Application application;

    public PipeLine2Receive(Application application, String clientId, Socket socket, InputStream inputStream,
            OutputStream outputStream) {
        this.clientConnected = true;
        this.socket = socket;
        this.clientId = clientId;
        this.application = application;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public boolean isClientConnected() { return this.clientConnected; }

    public void closeConnection() {
        try { this.socket.close(); this.clientConnected = false; } catch (Exception e) {}
    }

    public String getClientId() { return this.clientId; }

    public void run() {
        try {
            while (true) {
                StringBuilder sb = new StringBuilder();
                int x;
                while ((x = inputStream.read()) != '#') {
                    if (x == -1) throw new Exception("Connection closed");
                    sb.append((char) x);
                }
                int requestLength = Integer.parseInt(sb.toString());

                byte[] bytes = new byte[requestLength];
                int bytesRead = 0;
                while (bytesRead < requestLength) {
                    int count = inputStream.read(bytes, bytesRead, requestLength - bytesRead);
                    if (count == -1) throw new Exception("Connection closed");
                    bytesRead += count;
                }

                byte[] responseBytes = application.onRequestBytes(clientId, bytes);
                if (responseBytes == null) {
                    responseBytes = new byte[0];
                }

                String header = responseBytes.length + "#";
                outputStream.write(header.getBytes());
                outputStream.flush();

                int chunkSize = 1024;
                int bytesSent = 0;
                while (bytesSent < responseBytes.length) {
                    int len = Math.min(chunkSize, responseBytes.length - bytesSent);
                    outputStream.write(responseBytes, bytesSent, len);
                    bytesSent += len;
                }
                outputStream.flush();
            }
        } catch (Exception exception) {
            this.clientConnected = false;
        }
    }
}
