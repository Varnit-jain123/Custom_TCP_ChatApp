package chatapp.server;

import chatapp.core.*;
import chatapp.shared.Protocol;
import chatapp.shared.Message;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;

public class HeadlessChatServerApp implements Application {
    private Server server;
    private HashMap<String, String> userCredentials = new HashMap<>();
    private HashSet<String> onlineUsers = new HashSet<>();
    private HashMap<String, String> connectionIdToUsername = new HashMap<>();
    private HashMap<String, String> usernameToConnectionId = new HashMap<>();
    private Gson gson = new Gson();

    public HeadlessChatServerApp() {
        System.out.println("Starting Headless Chat Server for Docker...");
        loadData();
        server = new Server(this);
        server.start();
        System.out.println("Server running on ports 5050 and 4040. Waiting for connections...");
    }

    private void loadData() {
        try {
            File f = new File("chatapp/data.d");
            if (!f.exists()) f = new File("data.d");
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    userCredentials.put(parts[0], parts[1]);
                }
            }
            br.close();
            System.out.println("Loaded " + userCredentials.size() + " users.");
        } catch (Exception e) {
            System.out.println("Failed to load data.d: " + e.getMessage());
        }
    }

    private void broadcastOnlineUsers() {
        Message m = new Message();
        m.type = Protocol.ONLINE_USERS;
        m.userList = new ArrayList<>(onlineUsers);
        byte[] data = gson.toJson(m).getBytes();
        for (String cid : connectionIdToUsername.keySet()) {
            server.sendData(cid, data);
        }
    }

    @Override
    public byte[] onRequestBytes(String id, byte[] bytes) {
        try {
            String req = new String(bytes);
            Message msg = gson.fromJson(req, Message.class);

            if (msg.type.equals(Protocol.LOGIN)) {
                String user = msg.sender;
                String pass = msg.password;
                Message response = new Message();
                
                if (userCredentials.containsKey(user) && userCredentials.get(user).equals(pass)) {
                    if (onlineUsers.contains(user)) {
                        System.out.println("User " + user + " attempted to login, but is already logged in.");
                        response.type = Protocol.LOGIN_FAIL;
                        response.content = "Already logged in";
                        return gson.toJson(response).getBytes();
                    }
                    onlineUsers.add(user);
                    connectionIdToUsername.put(id, user);
                    usernameToConnectionId.put(user, id);
                    System.out.println("User connected: " + user);
                    broadcastOnlineUsers();
                    
                    response.type = Protocol.LOGIN_SUCCESS;
                    return gson.toJson(response).getBytes();
                } else {
                    System.out.println("Failed login attempt for user: " + user);
                    response.type = Protocol.LOGIN_FAIL;
                    response.content = "Invalid credentials";
                    return gson.toJson(response).getBytes();
                }
            } else if (msg.type.equals(Protocol.CHAT)) {
                String toUser = msg.recipient;
                String toCid = usernameToConnectionId.get(toUser);
                if (toCid != null) {
                    msg.sender = connectionIdToUsername.get(id); // Ensure correct sender
                    String payload = gson.toJson(msg);
                    server.sendData(toCid, payload.getBytes());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public void onResponseBytes(String id, byte[] bytes) {}

    @Override
    public void onConnected(String id) {
        System.out.println("New raw socket connection established. ID: " + id);
    }

    @Override
    public synchronized void onDisconnected(String id) {
        String user = connectionIdToUsername.get(id);
        if (user != null) {
            onlineUsers.remove(user);
            connectionIdToUsername.remove(id);
            usernameToConnectionId.remove(user);
            System.out.println("User disconnected: " + user);
            broadcastOnlineUsers();
        } else {
            System.out.println("Raw socket disconnected. ID: " + id);
        }
    }

    public static void main(String[] args) {
        new HeadlessChatServerApp();
    }
}
