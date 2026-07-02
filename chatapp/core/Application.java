package chatapp.core;
public interface Application {
    public byte[] onRequestBytes(String id, byte[] bytes);
    public void onResponseBytes(String id, byte[] bytes);
    public void onConnected(String id);
}
