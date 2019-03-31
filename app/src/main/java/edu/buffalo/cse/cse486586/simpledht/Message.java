package edu.buffalo.cse.cse486586.simpledht;



public class Message {

    private String key;
    private String value;
    private String hashKey;
    private String origin;
    private MessageType messageType;
    private String prevNode;
    private String nextNode;


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

    public MessageType getMessageType() {
        return messageType;
    }

    public String getPrevNode() {
        return prevNode;
    }

    public void setPrevNode(String prevNode) {
        this.prevNode = prevNode;
    }

    public String getNextNode() {
        return nextNode;
    }

    public void setNextNode(String nextNode) {
        this.nextNode = nextNode;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Message() {
    }

    public Message(String packet){

        String strReceived [] = packet.trim().split(Constants.SEPARATOR);

        key = strReceived[0];
        value = strReceived[1];
        hashKey = strReceived[2];
        origin = strReceived[3];
        messageType = MessageType.valueOf(strReceived[4]);
        prevNode = strReceived[5];
        nextNode = strReceived[6];

    }

    public String createPacket(){

        return key + Constants.SEPARATOR + value + Constants.SEPARATOR + hashKey + Constants.SEPARATOR + origin +
                Constants.SEPARATOR + messageType.name() + Constants.SEPARATOR + prevNode + Constants.SEPARATOR +
                nextNode;

    }

}
