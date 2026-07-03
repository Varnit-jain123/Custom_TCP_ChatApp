package chatapp.shared;

import java.util.List;

public class Message {
    public String type; // LOGIN, CHAT, ONLINE_USERS, LOGIN_SUCCESS, LOGIN_FAIL
    public String sender;
    public String recipient;
    public String content;
    public String password;
    public List<String> userList;
    
    // Default constructor for GSON
    public Message() {}
}
