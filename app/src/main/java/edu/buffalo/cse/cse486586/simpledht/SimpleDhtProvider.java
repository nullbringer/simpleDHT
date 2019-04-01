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
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    private static final String TAG = SimpleDhtProvider.class.getName();

    private static String MY_PORT;
    private static boolean isLeader = false;

    private static String PREV_NODE;
    private static String NEXT_NODE;

    private static TreeMap<String, String> ringStructure = new TreeMap<String,String>();

    @Override
    public boolean onCreate() {


        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        MY_PORT = String.valueOf(Integer.parseInt(portStr) * 2);

        try {

            if(MY_PORT.equals(Constants.LEADER_PORT)){
                isLeader = true;
                ringStructure.put(genHash(MY_PORT),MY_PORT);
                PREV_NODE = NEXT_NODE = MY_PORT;
            }

            Log.e(TAG,"my Port:::" + MY_PORT);


            /* Create server */



            ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return false;
        } catch (NoSuchAlgorithmException e){
            Log.e(TAG, "Could not hash!");
            return false;
        }


        /* Join the Network */
        if(!isLeader) {

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

                    if (incomingMessege!=null){

                        Message msg = new Message(incomingMessege);

                        if(msg.getMessageType() == MessageType.JOIN){

                            addNodeToNetwork(msg);

                        } else if(msg.getMessageType() == MessageType.JOIN_ACK){

                            updateAdjacentNodes(msg);

                        } else {

                            publishProgress(msg);

                        }





                    }


                    clientSocket.close();


                } catch (IOException e) {
                    Log.e(TAG, "Client Connection failed");
                } catch (NoSuchAlgorithmException e){
                    Log.e(TAG, "Could not hash!!");
                }
            }


        }

        protected void onProgressUpdate(Message...msgs) {


            try {

                //do stuff

            } catch (Exception e) {
                Log.d(TAG,"CloneNotSupportedException in queueing!!");
            }


            return;
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
//                    readAckAndClose(socket);

                socket.close();


            } catch (SocketTimeoutException e){
                Log.e(TAG, "ClientTask SocketTimeoutException");

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");

            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException: ");

            }


            return null;
        }
    }


    /*
     * Establish connecton to another node and write send a String
     * */

    private Socket connectAndWriteMessege(int thisPort, String msg) throws IOException {

        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                thisPort);

//        socket.setSoTimeout(READ_TIMEOUT_RANGE);

        String msgToSend = msg;

        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeUTF(msgToSend);
        dataOutputStream.flush();

        return socket;

    }

    private void addNodeToNetwork(Message msg) throws NoSuchAlgorithmException{

        ringStructure.put(genHash(msg.getOrigin()),msg.getOrigin());

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

        if (returnMsg.getPrevNode()!= returnMsg.getNextNode()) {

            newMsg = findAdjacentNode(newMsg, returnMsg.getNextNode());
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, newMsg.createPacket(), returnMsg.getNextNode());
        }


    }

    private Message findAdjacentNode(Message msg, String targetPort){




        msg.setPrevNode(ringStructure.lastEntry().getValue());




        Set<String> keys = ringStructure.keySet();
        Iterator<String> keyIterator = keys.iterator();

        while(keyIterator.hasNext()){

            String key = keyIterator.next();
            String thisPort = ringStructure.get(key);

            if(thisPort.equals(targetPort)){

                if(keyIterator.hasNext())
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

    private void updateAdjacentNodes(Message msg){

        PREV_NODE = msg.getPrevNode();
        NEXT_NODE = msg.getNextNode();

    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if(selection.equals(Constants.GLOBAL_INDICATOR)){

            //TODO: delete DHT data

        } else if(selection.equals(Constants.LOCAL_INDICATOR)){
            editor.clear();

        } else{
            editor.remove(selection);
        }

        editor.commit();

        Log.v("removed", selection);

        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(values.getAsString(Constants.KEY_FIELD), values.getAsString(Constants.VALUE_FIELD));
        editor.commit();

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        Log.v("query", selection);

        SharedPreferences sharedPref = getContext().getSharedPreferences(Constants.PREFERENCE_FILE, Context.MODE_PRIVATE);

        HashMap<String,String> hm = new HashMap<String,String>();

        if(selection.equals(Constants.GLOBAL_INDICATOR)){

            //TODO: get DHT data

        } else if(selection.equals(Constants.LOCAL_INDICATOR)){

            Map<String,?> keys = sharedPref.getAll();

            for(Map.Entry<String,?> entry : keys.entrySet()){

                hm.put(entry.getKey(),entry.getValue().toString());

                //Log.d("map values",entry.getKey() + ": " + entry.getValue().toString());
            }

        } else {

            hm.put(selection, sharedPref.getString(selection, null));


        }

        MatrixCursor cursor = new MatrixCursor(
                new String[] {Constants.KEY_FIELD, Constants.VALUE_FIELD}
        );

        for (Map.Entry<String, String> entry : hm.entrySet()) {

            cursor.newRow()
                    .add(Constants.KEY_FIELD, entry.getKey())
                    .add(Constants.VALUE_FIELD, entry.getValue());
        }

        return cursor;
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
