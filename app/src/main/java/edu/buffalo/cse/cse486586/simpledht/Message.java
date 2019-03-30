package edu.buffalo.cse.cse486586.simpledht;

public class Message {

    private String key;
    private String value;
    private String hashKey;
    private String origin;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Message(String key, String value, String hashKey, String origin) {
        this.key = key;
        this.value = value;
        this.hashKey = hashKey;
        this.origin = origin;
    }

    public Message(String packet){

        String strReceived [] = packet.trim().split(Constants.SEPERATOR);

        key = strReceived[0];
        value = strReceived[1];
        hashKey = strReceived[2];
        origin = strReceived[3];

    }

    public String createPacket(){

        return key + Constants.SEPERATOR + value + Constants.SEPERATOR + hashKey + Constants.SEPERATOR + origin;

    }
}
