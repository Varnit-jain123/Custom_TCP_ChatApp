package chatapp.client;

import chatapp.core.*;
import chatapp.shared.Protocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ChatClientApp implements Application {
    private Client client;
    private String username;
    private String pendingLoginJobId;
    
    // UI Frames
    private JFrame loginFrame;
    private JFrame dashboardFrame;
    private DefaultListModel<String> onlineListModel = new DefaultListModel<>();
    private HashMap<String, ChatWindow> chatWindows = new HashMap<>();

    public ChatClientApp() {
        client = new Client(this, "localhost", 5050, 4040);
        showLoginUI();
    }

    private void showLoginUI() {
        loginFrame = new JFrame("Chat Login");
        loginFrame.setSize(300, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridLayout(3, 2));

        loginFrame.add(new JLabel("Username:"));
        JTextField txtUser = new JTextField();
        loginFrame.add(txtUser);

        loginFrame.add(new JLabel("Password:"));
        JPasswordField txtPass = new JPasswordField();
        loginFrame.add(txtPass);

        JButton btnLogin = new JButton("Login");
        loginFrame.add(new JLabel()); // empty cell
        loginFrame.add(btnLogin);

        btnLogin.addActionListener(e -> {
            try {
                if (client == null || pendingLoginJobId == null) {
                    client.connect(); // Connect if not connected
                }
                this.username = txtUser.getText();
                String pass = new String(txtPass.getPassword());
                String payload = Protocol.LOGIN + "#" + this.username + "#" + pass;
                pendingLoginJobId = client.sendData(payload.getBytes());
                btnLogin.setEnabled(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(loginFrame, "Connection Failed: " + ex.getMessage());
            }
        });

        loginFrame.setVisible(true);
    }

    private void showDashboardUI() {
        dashboardFrame = new JFrame("Dashboard - " + username);
        dashboardFrame.setSize(300, 400);
        dashboardFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        dashboardFrame.setLayout(new BorderLayout());

        JList<String> list = new JList<>(onlineListModel);
        dashboardFrame.add(new JScrollPane(list), BorderLayout.CENTER);

        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedUser = list.getSelectedValue();
                    if (selectedUser != null) {
                        openChatWindow(selectedUser);
                    }
                }
            }
        });

        dashboardFrame.setVisible(true);
    }

    private ChatWindow openChatWindow(String remoteUser) {
        if (!chatWindows.containsKey(remoteUser)) {
            ChatWindow cw = new ChatWindow(remoteUser);
            chatWindows.put(remoteUser, cw);
        }
        ChatWindow cw = chatWindows.get(remoteUser);
        cw.frame.setVisible(true);
        return cw;
    }

    @Override
    public byte[] onRequestBytes(String id, byte[] bytes) {
        String req = new String(bytes);
        String[] parts = req.split("#");
        String cmd = parts[0];

        if (cmd.equals(Protocol.ONLINE_USERS)) {
            SwingUtilities.invokeLater(() -> {
                onlineListModel.clear();
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    String[] users = parts[1].split(",");
                    for (String u : users) {
                        if (!u.equals(username) && !u.isEmpty()) {
                            onlineListModel.addElement(u);
                        }
                    }
                }
            });
        } else if (cmd.equals(Protocol.CHAT)) {
            String fromUser = parts[1];
            String msg = parts.length > 2 ? parts[2] : "";
            SwingUtilities.invokeLater(() -> {
                ChatWindow cw = openChatWindow(fromUser);
                cw.appendMessage(fromUser, msg);
            });
        }

        return new byte[0];
    }

    @Override
    public void onResponseBytes(String id, byte[] bytes) {
        if (id.equals(pendingLoginJobId)) {
            String response = new String(bytes);
            SwingUtilities.invokeLater(() -> {
                if (response.startsWith(Protocol.LOGIN_SUCCESS)) {
                    loginFrame.setVisible(false);
                    showDashboardUI();
                } else {
                    JOptionPane.showMessageDialog(loginFrame, "Login failed: " + response);
                }
            });
            pendingLoginJobId = null;
        }
    }

    @Override
    public void onConnected(String id) {}

    class ChatWindow {
        JFrame frame;
        JTextArea txtHistory;
        JTextField txtMessage;

        public ChatWindow(String remoteUser) {
            frame = new JFrame("Chat with " + remoteUser);
            frame.setSize(400, 300);
            frame.setLayout(new BorderLayout());

            txtHistory = new JTextArea();
            txtHistory.setEditable(false);
            frame.add(new JScrollPane(txtHistory), BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout());
            txtMessage = new JTextField();
            JButton btnSend = new JButton("Send");
            bottom.add(txtMessage, BorderLayout.CENTER);
            bottom.add(btnSend, BorderLayout.EAST);
            frame.add(bottom, BorderLayout.SOUTH);

            btnSend.addActionListener(e -> {
                String msg = txtMessage.getText();
                if (!msg.isEmpty()) {
                    appendMessage("Me", msg);
                    String payload = Protocol.CHAT + "#" + remoteUser + "#" + msg;
                    client.sendData(payload.getBytes());
                    txtMessage.setText("");
                }
            });
        }

        public void appendMessage(String sender, String msg) {
            txtHistory.append(sender + ": " + msg + "\n");
        }
    }

    public static void main(String[] args) {
        new ChatClientApp();
    }
}
