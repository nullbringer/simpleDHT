package edu.buffalo.cse.cse486586.simpledht;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    private static final String TAG = SimpleDhtProvider.class.getName();

    private static String MY_PORT;
    private static boolean isLeader = false;

    private static String PREV_NODE;
    private static String NEXT_NODE;

    private static TreeMap<String, String> ringStructure = new TreeMap<String, String>();

    @Override
    public boolean onCreate() {


        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        MY_PORT = String.valueOf(Integer.parseInt(portStr) * 2);
        PREV_NODE = NEXT_NODE = MY_PORT;

        try {

            if (MY_PORT.equals(Constants.LEADER_PORT)) {
                isLeader = true;
                String emulatorId = String.valueOf(Integer.parseInt(MY_PORT) / 2);
                ringStructure.put(genHash(emulatorId), MY_PORT);
            }

            Log.e(TAG, "my Port:::" + MY_PORT);


            /* Create server */

            ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return false;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Could not hash!");
            return false;
        }


        /* Join the Network */
        if (!isLeader) {

            Message message = new Message();
            message.setOrigin(String.valueOf(MY_PORT));
            message.setMessageType(MessageType.JOIN);

            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message.createPacket(), Constants.LEADER_PORT);
        }

        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, Message, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /* infinite while loop to accept multiple messeges */

            while (true) {

                try {

                    Socket clientSocket = serverSocket.accept();

                    DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                    String incomingMessege = dataInputStream.readUTF();


                    Message msg = new Message(incomingMessege);


                    Log.d(TAG, msg.toString());

                    switch (msg.getMessageType()) {

                        case JOIN:
                            addNodeToNetwork(msg);
                            break;

                        case JOIN_ACK:
                            updateAdjacentNodes(msg);
                            break;

                        case STORE:
                            saveInRing(msg);
                            break;

                        case GET:
                            String packets = "";

                            if (!msg.getOrigin().equals(MY_PORT)) packets = getGlobalData(msg);

                            /* Send back the retrieved through channel to caller (Previous node) */

                            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                            dataOutputStream.writeUTF(packets);
                            dataOutputStream.flush();

                            dataOutputStream.close();

                            break;

                        case DEL:
                            if (!msg.getOrigin().equals(MY_PORT)) deleteFromRing(msg);

                            break;

                        default:

                            Log.e(TAG,"default switch case executed!!");

                    }

                    clientSocket.close();

                } catch (IOException e) {
                    Log.e(TAG, "Client Connection failed");
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "Could not hash!!");
                }
            }

        }

    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String msg = msgs[0];
            int thisPort = Integer.parseInt(msgs[1]);

            /*
             * Send messeges to target node
             * */

            try {

                Socket socket = connectAndWriteMessege(thisPort, msg);

                socket.close();

            } catch (SocketTimeoutException e) {
                Log.e(TAG, "ClientTask SocketTimeoutException");

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");

            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException: ");

            }

            return null;
        }
    }


    private class ClientTaskToGetData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... msgs) {

            String msg = msgs[0];
            int thisPort = Integer.parseInt(msgs[1]);

            String result = "";

            /*
             * Send messeges to target node
             * */

            try {

                Socket socket = connectAndWriteMessege(thisPort, msg);
                result = readAckAndClose(socket);

                socket.close();

            } catch (SocketTimeoutException e) {
                Log.e(TAG, "ClientTask SocketTimeoutException");

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");

            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException: ");

            }

            return result;
        }
    }


    /*
     * Establish connecton to another node and write send a String
     * */

    private Socket connectAndWriteMessege(int thisPort, String msg) throws IOException {

        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                thisPort);

        socket.setSoTimeout(Constants.SOCKET_READ_TIMEOUT);

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeUTF(msg);
        dataOutputStream.flush();

        return socket;

    }

    private String readAckAndClose(Socket socket) throws IOException {

        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

        String reply = dataInputStream.readUTF();

        /*
         * Received data from successor!
         * */


        dataInputStream.close();
        return reply;

    }

    private void addNodeToNetwork(Message msg) throws NoSuchAlgorithmException {


        ringStructure.put(genHash(String.valueOf(Integer.parseInt(msg.getOrigin()) / 2)), msg.getOrigin());

        /* update New Node */
        Message returnMsg = new Message();
        returnMsg.setMessageType(MessageType.JOIN_ACK);

        returnMsg = findAdjacentNode(returnMsg, msg.getOrigin());

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, returnMsg.createPacket(), msg.getOrigin());


        /* update it's adjacent Nodes */

        // Previous Node

        Message newMsg = new Message();
        newMsg.setMessageType(MessageType.JOIN_ACK);


        newMsg = findAdjacentNode(newMsg, returnMsg.getPrevNode());
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newMsg.createPacket(), returnMsg.getPrevNode());

        // Next Node

        if (!returnMsg.getPrevNode().equals(returnMsg.getNextNode())) {

            newMsg = findAdjacentNode(newMsg, returnMsg.getNextNode());
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newMsg.createPacket(), returnMsg.getNextNode());
        }


    }

    private Message findAdjacentNode(Message msg, String targetPort) {


        msg.setPrevNode(ringStructure.lastEntry().getValue());


        Set<String> keys = ringStructure.keySet();
        Iterator<String> keyIterator = keys.iterator();

        while (keyIterator.hasNext()) {

            String key = keyIterator.next();
            String thisPort = ringStructure.get(key);

            if (thisPort.equals(targetPort)) {

                if (keyIterator.hasNext())
                    msg.setNextNode(ringStructure.get(keyIterator.next()));
                else
                    msg.setNextNode(ringStructure.firstEntry().getValue());


                break;


            } else {
                msg.setPrevNode(thisPort);
            }


        }

        return msg;

    }

    private void updateAdjacentNodes(Message msg) {

        PREV_NODE = msg.getPrevNode();
        NEXT_NODE = msg.getNextNode();

    }

    private void saveInRing(Message message) {

        try {

            if (doesBelongLocally(message)) {

                SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();

                editor.putString(message.getKey(), message.getValue());
                editor.apply();

            } else {

                /* forward the request to successor node */

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message.createPacket(), NEXT_NODE);

            }

        } catch (NoSuchAlgorithmException e) {

            Log.e(TAG, "Could not hash!!");

        }


    }

    private boolean doesBelongLocally(Message message) throws NoSuchAlgorithmException {

        boolean doesBelong = false;

        String hashedKey = genHash(message.getKey());
        String myNodeHash = genHash(String.valueOf(Integer.parseInt(MY_PORT) / 2));
        String prevNodeHash = genHash(String.valueOf(Integer.parseInt(PREV_NODE) / 2));
        if (myNodeHash.compareTo(prevNodeHash) == 0) {

            /* if the network has only one node*/

            doesBelong = true;

        } else {

            /* if the network has multiple nodes */

            if (myNodeHash.compareTo(prevNodeHash) < 0) {

                /* if the key belongs between last node and first node*/

                if (hashedKey.compareTo(myNodeHash) < 0 && hashedKey.compareTo(Constants.HASHED_VALUE_MIN) > 0) {

                    /* if the key belongs between first node and minimum key*/

                    prevNodeHash = Constants.HASHED_VALUE_MIN;

                } else if (hashedKey.compareTo(prevNodeHash) > 0 && hashedKey.compareTo(Constants.HASHED_VALUE_MAX) < 0) {

                    /* if the key belongs between last node and maximum key*/

                    myNodeHash = Constants.HASHED_VALUE_MAX;
                }
            }

            /* If target belongs between two successive nodes, write locally */

            if (hashedKey.compareTo(myNodeHash) <= 0 && hashedKey.compareTo(prevNodeHash) > 0) {

                doesBelong = true;

            }

        }

        return doesBelong;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        Message message = new Message();
        message.setMessageType(MessageType.DEL);
        message.setKey(selection);

        if (selection.equals(Constants.LOCAL_INDICATOR)) {

            deleteAllLocalData();

        } else {

            deleteFromRing(message);
        }

        Log.v("removed", selection);

        return 0;
    }

    private void deleteAllLocalData(){

        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
    }

    private void deleteFromRing(Message message){

        try {

            if(message.getKey().equals(Constants.GLOBAL_INDICATOR)){

                deleteAllLocalData();

                /* forward the request to successor node */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message.createPacket(), NEXT_NODE);

            } else if (doesBelongLocally(message)) {

                SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.remove(message.getKey());
                editor.apply();

            } else {

                /* forward the request to successor node */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message.createPacket(), NEXT_NODE);
            }




        } catch (NoSuchAlgorithmException e) {

            Log.e(TAG, "Could not hash!!");

        }

    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        String thisKey = values.getAsString(Constants.KEY_FIELD);
        String thisValue = values.getAsString(Constants.VALUE_FIELD);


        Message message = new Message();
        message.setMessageType(MessageType.STORE);
        message.setOrigin(MY_PORT);
        message.setKey(thisKey);
        message.setValue(thisValue);


        saveInRing(message);

        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        Log.v("query", selection);

        HashMap<String, String> hm;

        Message message = new Message();
        message.setOrigin(String.valueOf(MY_PORT));
        message.setMessageType(MessageType.GET);
        message.setKey(selection);

        if (selection.equals(Constants.LOCAL_INDICATOR)) {

            hm = getAllLocalData();

        } else {

            String res = getGlobalData(message);

            hm = convertPacketsToKeyPair(res);

        }

        MatrixCursor cursor = new MatrixCursor(
                new String[]{Constants.KEY_FIELD, Constants.VALUE_FIELD}
        );

        for (Map.Entry<String, String> entry : hm.entrySet()) {

            cursor.newRow()
                    .add(Constants.KEY_FIELD, entry.getKey())
                    .add(Constants.VALUE_FIELD, entry.getValue());
        }

        return cursor;
    }

    private String getGlobalData(Message message) {

        HashMap<String, String> hm = new HashMap<String, String>();
        String result = "";

        try {


            result = new ClientTaskToGetData().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, message.createPacket(), NEXT_NODE).get();

            //TODO: check async task return after getting local data for a key

            if (message.getKey().equals(Constants.GLOBAL_INDICATOR)) {
                /* if the event is part of global query, get all local data */

                hm = getAllLocalData();
            } else if (doesBelongLocally(message)) {

                hm = getLocalDataByKey(message.getKey());
            }

            result = result + Constants.LIST_SEPARATOR + convertMessageListToPacket(hm);

        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException");
        } catch (ExecutionException e) {
            Log.e(TAG, "ExecutionException!!");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Could not hash!");
        }


        return result;
    }

    private HashMap<String, String> getAllLocalData() {

        HashMap<String, String> hm = new HashMap<String, String>();

        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);

        Map<String, ?> keys = sharedPref.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {

            hm.put(entry.getKey(), entry.getValue().toString());

        }

        return hm;

    }

    private HashMap<String, String> getLocalDataByKey(String selection) {

        HashMap<String, String> hm = new HashMap<String, String>();


        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);

        hm.put(selection, sharedPref.getString(selection, null));

        return hm;

    }


    private String convertMessageListToPacket(HashMap<String, String> hm) {


        List<String> packetList = new ArrayList<String>();


        Iterator it = hm.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, String> pair = (Map.Entry) it.next();

            Message msg = new Message();
            msg.setKey(pair.getKey());
            msg.setValue(pair.getValue());
            msg.setOrigin(MY_PORT);
            msg.setMessageType(MessageType.GET);

            packetList.add(msg.createPacket());

            it.remove(); // avoids a ConcurrentModificationException
        }


        return TextUtils.join(Constants.LIST_SEPARATOR, packetList);

    }

    private HashMap<String, String> convertPacketsToKeyPair(String packets) {

        HashMap<String, String> hm = new HashMap<String, String>();

        List<String> ls = Arrays.asList(packets.split(Constants.LIST_SEPARATOR));

        for (String packet : ls) {
            if (packet != null && packet.trim().length() > 0) {
                Message msg = new Message(packet);

                hm.put(msg.getKey(), msg.getValue());

            }
        }

        return hm;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

}
