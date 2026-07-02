package chatapp.shared;

public class Protocol {
    public static final String LOGIN = "LOGIN"; // LOGIN#user#pass
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS"; // LOGIN_SUCCESS#
    public static final String LOGIN_FAIL = "LOGIN_FAIL"; // LOGIN_FAIL#reason
    public static final String ONLINE_USERS = "ONLINE_USERS"; // ONLINE_USERS#user1,user2...
    public static final String CHAT = "CHAT"; // CHAT#toUser#msg OR CHAT#fromUser#msg
}
