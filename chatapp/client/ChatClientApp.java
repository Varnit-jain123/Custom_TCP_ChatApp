package chatapp.client;

import chatapp.core.*;
import chatapp.shared.Protocol;
import chatapp.shared.Message;
import com.google.gson.Gson;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ChatClientApp implements Application {
    private Client client;
    private String username;
    private String pendingLoginJobId;
    private Gson gson = new Gson();
    
    // UI Elements
    private JFrame loginFrame;
    private JFrame mainFrame;
    private DefaultListModel<String> onlineListModel = new DefaultListModel<>();
    private JList<String> onlineList;
    private JPanel chatCardPanel; 
    private CardLayout cardLayout;
    private HashMap<String, ChatPanel> chatPanels = new HashMap<>();

    public ChatClientApp() {
        client = new Client(this, "localhost", 5050, 4040);
        showLoginUI();
    }

    private void showLoginUI() {
        loginFrame = new JFrame("Chat Login");
        loginFrame.setSize(300, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridLayout(3, 2, 5, 5));

        loginFrame.add(new JLabel("  Username:"));
        JTextField txtUser = new JTextField();
        loginFrame.add(txtUser);

        loginFrame.add(new JLabel("  Password:"));
        JPasswordField txtPass = new JPasswordField();
        loginFrame.add(txtPass);

        JButton btnLogin = new JButton("Login");
        loginFrame.add(new JLabel()); 
        loginFrame.add(btnLogin);

        btnLogin.addActionListener(e -> {
            try {
                if (client == null || pendingLoginJobId == null) {
                    client.connect(); 
                }
                this.username = txtUser.getText();
                String pass = new String(txtPass.getPassword());
                
                Message m = new Message();
                m.type = Protocol.LOGIN;
                m.sender = this.username;
                m.password = pass;
                
                String payload = gson.toJson(m);
                pendingLoginJobId = client.sendData(payload.getBytes());
                btnLogin.setEnabled(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(loginFrame, "Connection Failed: " + ex.getMessage());
                btnLogin.setEnabled(true);
            }
        });

        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    private void showMainUI() {
        mainFrame = new JFrame("Chat - " + username);
        mainFrame.setSize(800, 500);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLayout(new BorderLayout());

        JPanel sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setPreferredSize(new Dimension(200, 0));
        
        JLabel lblOnline = new JLabel("Online Users", SwingConstants.CENTER);
        lblOnline.setFont(new Font("Arial", Font.BOLD, 14));
        lblOnline.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        sidebarPanel.add(lblOnline, BorderLayout.NORTH);

        onlineList = new JList<>(onlineListModel);
        onlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sidebarPanel.add(new JScrollPane(onlineList), BorderLayout.CENTER);

        onlineList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedUser = onlineList.getSelectedValue();
                    if (selectedUser != null) {
                        openChatView(selectedUser);
                    }
                }
            }
        });

        cardLayout = new CardLayout();
        chatCardPanel = new JPanel(cardLayout);

        JPanel defaultPanel = new JPanel(new GridBagLayout());
        JLabel lblDefault = new JLabel("Double click a user on the right to start chatting!");
        lblDefault.setForeground(Color.GRAY);
        defaultPanel.add(lblDefault);
        chatCardPanel.add(defaultPanel, "DEFAULT");

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatCardPanel, sidebarPanel);
        splitPane.setResizeWeight(1.0); 
        mainFrame.add(splitPane, BorderLayout.CENTER);

        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private void openChatView(String remoteUser) {
        if (!chatPanels.containsKey(remoteUser)) {
            ChatPanel cp = new ChatPanel(remoteUser);
            chatPanels.put(remoteUser, cp);
            chatCardPanel.add(cp, remoteUser);
        }
        cardLayout.show(chatCardPanel, remoteUser);
    }

    @Override
    public byte[] onRequestBytes(String id, byte[] bytes) {
        try {
            String req = new String(bytes);
            Message msg = gson.fromJson(req, Message.class);

            if (msg.type.equals(Protocol.ONLINE_USERS)) {
                SwingUtilities.invokeLater(() -> {
                    onlineListModel.clear();
                    if (msg.userList != null) {
                        for (String u : msg.userList) {
                            if (!u.equals(username) && !u.isEmpty()) {
                                onlineListModel.addElement(u);
                            }
                        }
                    }
                });
            } else if (msg.type.equals(Protocol.CHAT)) {
                String fromUser = msg.sender;
                String text = msg.content;
                SwingUtilities.invokeLater(() -> {
                    if (!chatPanels.containsKey(fromUser)) {
                        ChatPanel cp = new ChatPanel(fromUser);
                        chatPanels.put(fromUser, cp);
                        chatCardPanel.add(cp, fromUser);
                    }
                    chatPanels.get(fromUser).appendMessage(fromUser, text);
                    cardLayout.show(chatCardPanel, fromUser);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public void onResponseBytes(String id, byte[] bytes) {
        if (id.equals(pendingLoginJobId)) {
            try {
                String response = new String(bytes);
                Message msg = gson.fromJson(response, Message.class);
                SwingUtilities.invokeLater(() -> {
                    if (msg == null) {
                        JOptionPane.showMessageDialog(loginFrame, "Error: Received empty response from server.");
                        return;
                    }
                    if (msg.type.equals(Protocol.LOGIN_SUCCESS)) {
                        loginFrame.setVisible(false);
                        showMainUI();
                    } else {
                        JOptionPane.showMessageDialog(loginFrame, "Login failed: " + msg.content);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            pendingLoginJobId = null;
        }
    }

    @Override
    public void onConnected(String id) {}

    @Override
    public void onDisconnected(String id) {
        System.out.println("Disconnected from server.");
        SwingUtilities.invokeLater(() -> {
            if (mainFrame != null) {
                JOptionPane.showMessageDialog(mainFrame, "Lost connection to server.");
                System.exit(0);
            }
        });
    }

    class ChatPanel extends JPanel {
        private String remoteUser;
        private JTextArea txtHistory;
        private JTextField txtMessage;

        public ChatPanel(String remoteUser) {
            this.remoteUser = remoteUser;
            setLayout(new BorderLayout());

            JLabel lblHeader = new JLabel("  Chatting with " + remoteUser);
            lblHeader.setFont(new Font("Arial", Font.BOLD, 14));
            lblHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            lblHeader.setOpaque(true);
            lblHeader.setBackground(new Color(230, 230, 230));
            add(lblHeader, BorderLayout.NORTH);

            txtHistory = new JTextArea();
            txtHistory.setEditable(false);
            txtHistory.setLineWrap(true);
            txtHistory.setWrapStyleWord(true);
            add(new JScrollPane(txtHistory), BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            txtMessage = new JTextField();
            JButton btnSend = new JButton("Send");
            bottomPanel.add(txtMessage, BorderLayout.CENTER);
            bottomPanel.add(btnSend, BorderLayout.EAST);
            add(bottomPanel, BorderLayout.SOUTH);

            ActionListener sendAction = e -> sendMessage();
            btnSend.addActionListener(sendAction);
            txtMessage.addActionListener(sendAction); 
        }

        private void sendMessage() {
            String text = txtMessage.getText().trim();
            if (!text.isEmpty()) {
                appendMessage("Me", text);
                
                Message m = new Message();
                m.type = Protocol.CHAT;
                m.recipient = remoteUser;
                m.content = text;
                
                String payload = gson.toJson(m);
                client.sendData(payload.getBytes());
                txtMessage.setText("");
            }
        }

        public void appendMessage(String sender, String msg) {
            txtHistory.append(sender + ": " + msg + "\n");
            txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
        }
    }

    public static void main(String[] args) {
        new ChatClientApp();
    }
}
