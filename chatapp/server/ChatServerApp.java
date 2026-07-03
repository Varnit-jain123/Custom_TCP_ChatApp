package chatapp.server;

import chatapp.core.*;
import chatapp.shared.Protocol;
import chatapp.shared.Message;
import com.google.gson.Gson;
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
    private Gson gson = new Gson();

    public ChatServerApp() {
        super("Chat Server");
        loadData();
        setupUI();
        server = new Server(this);
        server.start();
        System.out.println("Server started.");
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

        JLabel lblTitle = new JLabel("Server Active - Online Users", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 14));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(lblTitle, BorderLayout.NORTH);

        JList<String> userList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(userList);
        add(scrollPane, BorderLayout.CENTER);

        JButton btnShutdown = new JButton("Shutdown Server");
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
                        response.type = Protocol.LOGIN_FAIL;
                        response.content = "Already logged in";
                        return gson.toJson(response).getBytes();
                    }
                    onlineUsers.add(user);
                    connectionIdToUsername.put(id, user);
                    usernameToConnectionId.put(user, id);
                    updateOnlineList();
                    broadcastOnlineUsers();
                    
                    response.type = Protocol.LOGIN_SUCCESS;
                    return gson.toJson(response).getBytes();
                } else {
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
