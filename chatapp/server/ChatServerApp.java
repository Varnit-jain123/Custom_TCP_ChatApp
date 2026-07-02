package chatapp.server;

import chatapp.core.*;
import chatapp.shared.Protocol;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class ChatServerApp extends JFrame implements Application {
    private Server server;
    private HashMap<String, String> userCredentials = new HashMap<>();
    private HashSet<String> onlineUsers = new HashSet<>();
    private HashMap<String, String> connectionIdToUsername = new HashMap<>();
    private HashMap<String, String> usernameToConnectionId = new HashMap<>();

    private DefaultListModel<String> listModel = new DefaultListModel<>();

    public ChatServerApp() {
        super("Chat Server");
        loadData();
        setupUI();
        server = new Server(this);
        server.start();
    }

    private void loadData() {
        try {
            File f = new File("chatapp/data.d");
            if (!f.exists())
                f = new File("data.d");
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    userCredentials.put(parts[0], parts[1]);
                }
            }
            br.close();
            System.out.println("Loaded " + userCredentials.size() + " users.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        setSize(300, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JList<String> list = new JList<>(listModel);
        add(new JScrollPane(list), BorderLayout.CENTER);

        JButton btnShutdown = new JButton("Shutdown");
        btnShutdown.addActionListener(e -> System.exit(0));
        add(btnShutdown, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void updateOnlineList() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (String user : onlineUsers) {
                listModel.addElement(user);
            }
        });
    }

    private void broadcastOnlineUsers() {
        StringBuilder sb = new StringBuilder(Protocol.ONLINE_USERS).append("#");
        for (String user : onlineUsers) {
            sb.append(user).append(",");
        }
        byte[] data = sb.toString().getBytes();
        for (String cid : connectionIdToUsername.keySet()) {
            server.sendData(cid, data);
        }
    }

    @Override
    public byte[] onRequestBytes(String id, byte[] bytes) {
        String req = new String(bytes);
        String[] parts = req.split("#");
        String command = parts[0];

        if (command.equals(Protocol.LOGIN)) {
            String user = parts[1];
            String pass = parts[2];
            if (userCredentials.containsKey(user) && userCredentials.get(user).equals(pass)) {
                if (onlineUsers.contains(user)) {
                    return (Protocol.LOGIN_FAIL + "#Already logged in").getBytes();
                }
                onlineUsers.add(user);
                connectionIdToUsername.put(id, user);
                usernameToConnectionId.put(user, id);
                updateOnlineList();
                broadcastOnlineUsers();
                return (Protocol.LOGIN_SUCCESS + "#").getBytes();
            } else {
                return (Protocol.LOGIN_FAIL + "#Invalid credentials").getBytes();
            }
        } else if (command.equals(Protocol.CHAT)) {
            // CHAT#toUser#message
            String toUser = parts[1];
            String message = parts.length > 2 ? parts[2] : "";
            String fromUser = connectionIdToUsername.get(id);
            String toCid = usernameToConnectionId.get(toUser);
            if (toCid != null) {
                String payload = Protocol.CHAT + "#" + fromUser + "#" + message;
                server.sendData(toCid, payload.getBytes());
            }
        }

        return new byte[0];
    }

    @Override
    public void onResponseBytes(String id, byte[] bytes) {
    }

    @Override
    public void onConnected(String id) {
    }

    @Override
    public synchronized void onDisconnected(String id) {
        String user = connectionIdToUsername.get(id);
        if (user != null) {
            onlineUsers.remove(user);
            connectionIdToUsername.remove(id);
            usernameToConnectionId.remove(user);
            updateOnlineList();
            broadcastOnlineUsers();
        }
    }

    public static void main(String[] args) {
        new ChatServerApp();
    }
}
