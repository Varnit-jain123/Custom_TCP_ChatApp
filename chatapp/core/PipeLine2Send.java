package chatapp.core;
import java.io.*;
import java.net.*;
import java.util.*;

public class PipeLine2Send extends Thread {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String clientId;
    private List<Job> jobQueue;
    private boolean clientConnected;
    private Application application;

    public PipeLine2Send(Application application, String clientId, Socket socket, InputStream inputStream,
            OutputStream outputStream) {
        this.clientConnected = true;
        this.socket = socket;
        this.clientId = clientId;
        this.application = application;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.jobQueue = Collections.synchronizedList(new ArrayList<Job>());
    }

    public boolean isClientConnected() { return this.clientConnected; }

    public void closeConnection() {
        try { this.socket.close(); this.clientConnected = false; } catch (Exception e) {}
    }

    public String addData(byte[] data) {
        if (!clientConnected) return null;
        String id = UUID.randomUUID().toString();
        Job job = new Job();
        job.id = id;
        job.bytes = data;
        synchronized (jobQueue) {
            jobQueue.add(job);
            jobQueue.notify();
        }
        return id;
    }

    public String getClientId() { return this.clientId; }

    public void run() {
        try {
            byte data[];
            Job job;
            while (true) {
                synchronized (jobQueue) {
                    if (jobQueue.size() == 0) {
                        jobQueue.wait();
                        continue;
                    }
                    job = jobQueue.remove(0);
                }
                data = job.bytes;
                
                String header = data.length + "#";
                outputStream.write(header.getBytes());
                outputStream.flush();

                int chunkSize = 1024;
                int bytesSent = 0;
                while (bytesSent < data.length) {
                    int len = Math.min(chunkSize, data.length - bytesSent);
                    outputStream.write(data, bytesSent, len);
                    bytesSent += len;
                }
                outputStream.flush();

                StringBuilder sb = new StringBuilder();
                int x;
                while ((x = inputStream.read()) != '#') {
                    if (x == -1) throw new Exception("Connection closed");
                    sb.append((char) x);
                }
                int responseLength = Integer.parseInt(sb.toString());
                byte[] responseArray = new byte[responseLength];
                int bytesRead = 0;
                while (bytesRead < responseLength) {
                    int count = inputStream.read(responseArray, bytesRead, responseLength - bytesRead);
                    if (count == -1) throw new Exception("Connection closed");
                    bytesRead += count;
                }

                application.onResponseBytes(job.id, responseArray);
            }
        } catch (Exception exception) {
            this.clientConnected = false;
        }
    }
}
