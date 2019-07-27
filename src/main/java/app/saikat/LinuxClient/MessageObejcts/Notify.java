package app.saikat.LinuxClient.MessageObejcts;

public class Notify {
    
    private String target;
    private int ttl;
    private String title;
    private String message;

    public Notify(String target, int ttl, String title, String message) {
        this.target = target;
        this.ttl = ttl;
        this.title = title;
        this.message = message;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getTtl() {
        return this.ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}