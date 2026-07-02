package chatapp.core;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private Application application;
    private ServerSocket serverSocket1 = null;
    private ServerSocket serverSocket2 = null;
    private ConcurrentHashMap<String, Object[]> socketStreams;
    private ConcurrentHashMap<String, PipeLines> pipeLinesMap;
    private Thread threadForServerSocket1;
    private Thread threadForServerSocket2;

    public Server(Application application) {
        this.application = application;
        this.pipeLinesMap = new ConcurrentHashMap<>();
        this.socketStreams = new ConcurrentHashMap<>();
    }

    public String sendData(String clientId, byte[] data) {
        PipeLines pl = pipeLinesMap.get(clientId);
        if (pl != null && pl.pipeLine2Send != null) {
            return pl.pipeLine2Send.addData(data);
        }
        return null;
    }

    public void start() {
        try {
            serverSocket1 = new ServerSocket(5050);
            serverSocket2 = new ServerSocket(4040);
            
            threadForServerSocket1 = new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket1.accept();
                        InputStream inputStream = socket.getInputStream();
                        OutputStream outputStream = socket.getOutputStream();
                        StringBuilder stringBuffer = new StringBuilder();
                        int x, i = 0;
                        while (true) {
                            i++;
                            x = inputStream.read();
                            if (x == '#' || i == 10) break;
                            stringBuffer.append((char) x);
                        }
                        if (x != '#' || !stringBuffer.toString().equals("CONNECT")) {
                            outputStream.write("INVALID#".getBytes());
                            outputStream.flush();
                            socket.close();
                            continue;
                        }
                        String id = UUID.randomUUID().toString();
                        Object[] objects = new Object[]{socket, inputStream, outputStream};
                        socketStreams.put(id, objects);
                        outputStream.write((id + "#").getBytes());
                        outputStream.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            threadForServerSocket1.start();

            threadForServerSocket2 = new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket2.accept();
                        InputStream inputStream = socket.getInputStream();
                        OutputStream outputStream = socket.getOutputStream();
                        StringBuilder stringBuffer = new StringBuilder();
                        int x, i = 0;
                        while (true) {
                            i++;
                            x = inputStream.read();
                            if (x == '#' || i == 100) break;
                            stringBuffer.append((char) x);
                        }
                        if (x != '#') {
                            outputStream.write("INVALID#".getBytes());
                            outputStream.flush();
                            socket.close();
                            continue;
                        }
                        String id = stringBuffer.toString();
                        Object[] objects = socketStreams.get(id);
                        if (objects == null) {
                            outputStream.write("INVALID#".getBytes());
                            outputStream.flush();
                            socket.close();
                            continue;
                        }
                        socketStreams.remove(id);
                        PipeLine2Send pipeLine2Send = new PipeLine2Send(application, id, socket, inputStream, outputStream);
                        PipeLine2Receive pipeLine2Receive = new PipeLine2Receive(application, id, (Socket) objects[0],
                                (InputStream) objects[1], (OutputStream) objects[2]);
                        PipeLines pipeLines = new PipeLines();
                        pipeLines.pipeLine2Send = pipeLine2Send;
                        pipeLines.pipeLine2Receive = pipeLine2Receive;
                        pipeLinesMap.put(id, pipeLines);
                        pipeLine2Send.start();
                        pipeLine2Receive.start();
                        outputStream.write("CONNECTED#".getBytes());
                        outputStream.flush();
                        application.onConnected(id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            threadForServerSocket2.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
