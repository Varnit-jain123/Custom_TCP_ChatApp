package chatapp.core;
import java.io.*;
import java.net.*;

public class Client {
    private Application application;
    private Socket socket2Send;
    private Socket socket2Receive;
    private PipeLine2Send pipeLine2Send;
    private PipeLine2Receive pipeLine2Receive;
    private String server;
    private int portNumber1, portNumber2;

    public Client(Application application, String server, int portNumber1, int portNumber2) {
        this.application = application;
        this.server = server;
        this.portNumber1 = portNumber1;
        this.portNumber2 = portNumber2;
    }

    public String sendData(byte[] data) {
        if (pipeLine2Send != null) {
            return pipeLine2Send.addData(data);
        }
        return null;
    }

    public void connect() throws ConnectionException {
        try {
            socket2Send = new Socket(server, portNumber1);
            InputStream inputStream1 = socket2Send.getInputStream();
            OutputStream outputStream1 = socket2Send.getOutputStream();
            String request = "CONNECT#";
            outputStream1.write(request.getBytes());
            outputStream1.flush();
            StringBuilder stringBuffer = new StringBuilder();
            int x;
            while (true) {
                x = inputStream1.read();
                if (x == '#') break;
                stringBuffer.append((char) x);
            }
            String clientId = stringBuffer.toString();

            socket2Receive = new Socket(server, portNumber2);
            InputStream inputStream2 = socket2Receive.getInputStream();
            OutputStream outputStream2 = socket2Receive.getOutputStream();
            request = clientId + "#";
            outputStream2.write(request.getBytes());
            outputStream2.flush();
            stringBuffer = new StringBuilder();
            while (true) {
                x = inputStream2.read();
                if (x == '#') break;
                stringBuffer.append((char) x);
            }
            String response = stringBuffer.toString();
            if (response.equals("INVALID")) {
                throw new ConnectionException("unable to connect");
            }

            pipeLine2Send = new PipeLine2Send(application, clientId, socket2Send, inputStream1, outputStream1);
            pipeLine2Receive = new PipeLine2Receive(application, clientId, socket2Receive, inputStream2, outputStream2);
            pipeLine2Send.start();
            pipeLine2Receive.start();
            application.onConnected(clientId);

        } catch (Exception e) {
            throw new ConnectionException(e.getMessage());
        }
    }
}
