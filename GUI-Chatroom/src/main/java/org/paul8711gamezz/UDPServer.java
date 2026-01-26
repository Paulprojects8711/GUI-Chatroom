package org.paul8711gamezz;

import com.google.gson.Gson;
import org.paul8711gamezz.helpers.Hash;
import org.paul8711gamezz.helpers.ServerUIHandler;
import org.paul8711gamezz.helpers.VCInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.*;

public class UDPServer {
    // variables used for the generation of the salt
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    // generates the salt (encryption) with a length of 32
    private static final String SALT = genSalt(32);

    public static DatagramSocket serverSocket;

    public static ServerUIHandler uiHandler;

    // this section passes everything to the server ui handler
    public static void setUiHandler(ServerUIHandler handler) {
        uiHandler = handler;
    }

    public static void handleError(String msg) {
        if (uiHandler != null) uiHandler.onError(msg);
        else System.out.println("Error" + msg);
    }
    public static void handleStop(String reason) {
        if (uiHandler != null) uiHandler.onStop(reason);
        else System.out.println("Disconnected" + reason);
    }
    public static void handleUserListUpdate(Map<String, String> userMap) {
        if (uiHandler != null) uiHandler.onUserListUpdate(userMap);
        else System.out.println(userMap);
    }
    public static void handleVCListUpdate(Map<String, VCInfo> vcStatus, Map<String, String> userMap) {
        if (uiHandler != null) uiHandler.onVCListUpdate(vcStatus, userMap);
        else System.out.println(vcStatus);
    }
    public static void handleLog(String msg) {
        if (uiHandler != null) uiHandler.onLog(msg);
        else System.out.println(msg);
    }

    public static void runServer(int port, String authKey) throws Exception {
        // hashes the authKey if it was set
        if (!authKey.isEmpty()) {
            server(port, Hash.hash(authKey));
        } else {
            server(port, "");
        }
    }

    // for testing when the gui didnt exist yet
    public static void main(String[] args) throws Exception {
        Scanner portSc = new Scanner(System.in);
        System.out.println("Port:");
        int port = Integer.parseInt(portSc.nextLine());

        Scanner keySc = new Scanner(System.in);
        System.out.println("Set Auth Key:");
        String authKey = keySc.nextLine();

        if (!authKey.isEmpty()) {
            server(port, Hash.hash(authKey));
        } else {
            server(port, "");
        }
    }
    public static void server(int port, String authKeyHash) {
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            UDPServer.serverSocket = serverSocket;
            handleLog("Server started on Port " + port);
            // creates the different maps:
            // - userMap: username with the associated ip and port (udp servers do it this way)
            // - lastSeen: username with time when the last message was received
            // - vcStatus: username with the values of VCInfo, so if he is in the vc if he is muted or if he is deaf
            Map<String, String> userMap = new HashMap<>();
            Map<String, Long> lastSeen = new HashMap<>();
            Map<String, VCInfo> vcStatus = new HashMap<>();

            // maximum bytes of data the server can receive
            byte[] receiveData = new byte[1024];
            // map where the first value is the id of the client (ip and port) and the other is which message the server sent to it
            Map<String, String> lastSent = new HashMap<>(); // <clientID, lastMessageSent>

            // checks if a user isnt responding (internet not working for example) and then the server kicks him after 15s
            new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        Thread.sleep(5000); // check every 5 seconds
                        if (serverSocket.isClosed()) break;
                        long now = System.currentTimeMillis();
                        for (String clientID : new ArrayList<>(lastSeen.keySet())) {
                            if (now - lastSeen.get(clientID) > 15000) { // 15 sec timeout
                                String lastMessage = lastSent.get(clientID);
                                if (lastMessage.equals("auth|request")) {
                                    // dont kick
                                    lastSeen.put(clientID, now);
                                    continue;
                                }
                                String username = userMap.get(clientID);
                                if (username != null) {
                                    // remove user from basically everywhere
                                    userMap.remove(clientID);
                                    lastSeen.remove(clientID);
                                    vcStatus.remove(clientID);
                                    handleUserListUpdate(userMap);
                                    handleVCListUpdate(vcStatus, userMap);
                                    broadcast(serverSocket, userMap, "leave|" + username + " disconnected (timeout)");
                                    broadcastUserList(serverSocket, userMap);
                                }
                            }
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    handleStop("Unknown Error");
                    handleError("Error: " + e.getMessage());
                }
            }).start();

            while (!serverSocket.isClosed()) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                byte[] packetData = receivePacket.getData();
                int packetLength = receivePacket.getLength();
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                // clientID: <ip address>:<port>
                String clientID = clientAddress.toString() + ":" + clientPort;

                // check if it is a voice packet
                if (packetLength > 6) {
                    String header = new String(packetData, 0, 6);
                    if (header.equals("voice|")) {
                        // only broadcast to users in VC
                        broadcastVoice(serverSocket, userMap, vcStatus, clientID, Arrays.copyOf(packetData, packetLength));
                        continue; // skip the other stuff
                    }
                }

                String message = new String(packetData, 0, packetLength);
                handleLog("Received packet: " + message);

                // separate the type from the data
                String[] splitMessage = message.split("\\|", 2);
                String type = splitMessage[0];
                String data = splitMessage[1];

                if (type.equals("join")) {
                    if (!userMap.containsValue(data)) {
                        // if the username doesnt exist yet join the server
                        userMap.put(clientID, data);
                        lastSeen.put(clientID, System.currentTimeMillis());
                        vcStatus.put(clientID, new VCInfo(false, false, false));
                        if (!authKeyHash.isEmpty()) {
                            // ask user to authenticate
                            sendToSingle(serverSocket, clientID, "auth|request", lastSent);
                        } else {
                            handleLog("User " + data + " joined");
                            // send salt to client (key is the room password)
                            sendToSingle(serverSocket, clientID, "salt|" + UDPServer.SALT, lastSent);
                            broadcast(serverSocket, userMap, "join|" + "User " + data + " joined");
                            // broadcast lists and update own
                            broadcastUserList(serverSocket, userMap);
                            broadcastVCUsers(serverSocket, userMap, vcStatus);
                            handleUserListUpdate(userMap);
                            handleVCListUpdate(vcStatus, userMap);
                        }
                    } else {
                        // err| is only used here i think
                        sendToSingle(serverSocket, clientID, "err|" + "Username already in use", lastSent);
                    }
                } else if (type.equals("auth")) {
                    // checks if the auth key is correct
                    if (!Hash.verify(data, authKeyHash)) {
                        // kick user if wrong
                        handleLog("User " + userMap.get(clientID) + " got the auth key wrong");
                        sendToSingle(serverSocket, clientID, "auth|wrong", lastSent);
                        userMap.remove(clientID);
                        lastSeen.remove(clientID);
                        vcStatus.remove(clientID);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus, userMap);
                    } else {
                        // user joins and the lists are broadcasted
                        sendToSingle(serverSocket, clientID, "auth|ok", lastSent);
                        handleLog("User " + userMap.get(clientID) + " joined");
                        sendToSingle(serverSocket, clientID, "salt|" + UDPServer.SALT, lastSent);
                        broadcast(serverSocket, userMap, "join|" + "User " + userMap.get(clientID) + " joined");
                        broadcastUserList(serverSocket, userMap);
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus, userMap);
                    }
                } else if (type.equals("chat")) {
                    // add the username to the chat message (so the user cant send "fake" messages from other users)
                    String username = userMap.get(clientID);
                    lastSeen.put(clientID, System.currentTimeMillis());
                    handleLog(username + ": " + data);
                    broadcast(serverSocket, userMap, "chat|" + username + ": " + data);
                } else if (type.equals("leave")) {
                    // removes the user from all the lists
                    String username = userMap.get(clientID);
                    if (username != null) {
                        userMap.remove(clientID);
                        vcStatus.remove(clientID);
                        broadcast(serverSocket, userMap, "leave|" + "User " + username + " left");
                        broadcastUserList(serverSocket, userMap);
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus, userMap);
                    }
                } else if (type.equals("ping")) {
                    // update the lastSeen so user doesnt get kicked and respond so the user doesnt auto-disconnect
                    lastSeen.put(clientID, System.currentTimeMillis());
                    sendToSingle(serverSocket, clientID, "pong|", lastSent);
                } else if (type.equals("vc")) {
                    if (data.equals("join")) {
                        // puts the user in vc
                        vcStatus.put(clientID, new VCInfo(true, false, false));
                        broadcast(serverSocket, userMap, "User " + userMap.get(clientID) + " joined Voice call");
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus, userMap);
                    } else if (data.equals("leave")) {
                        // removes the user from vc (sets inVC to false)
                        vcStatus.put(clientID, new VCInfo(false, false, false));
                        broadcast(serverSocket, userMap, "User " + userMap.get(clientID) + " left Voice call");
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus, userMap);
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                    } else if (data.equals("mute")) {
                        // mutes/unmutes user
                        VCInfo info = vcStatus.get(clientID);
                        info.mute = !info.mute;
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus, userMap);
                    } else if (data.equals("deaf")) {
                        // deafs/undeafs user
                        VCInfo info = vcStatus.get(clientID);
                        info.deaf = !info.deaf;
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus, userMap);
                    }
                }
            }
        } catch (Exception e) {
            if (!serverSocket.isClosed()) handleError("Error: " + e.getMessage());
        }
    }
    public static void broadcast(DatagramSocket serverSocket, Map<String, String> userMap, String message) throws IOException {
        // broadcasts a message to all users
        byte[] sendData = message.getBytes();

        for (String clientID : userMap.keySet()) {
            String[] parts = clientID.split(":");
            InetAddress clientAddress = InetAddress.getByName(parts[0].replace("/", ""));
            int clientPort = Integer.parseInt(parts[1]);

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);
        }
    }
    public static void broadcastVoice(DatagramSocket serverSocket, Map<String, String> userMap, Map<String, VCInfo> vcStatus, String senderID, byte[] sendData) throws IOException {
        // broadcasts the bytes of the voice data to all users who are in vc
        for (String clientID : userMap.keySet()) {
            if (vcStatus.getOrDefault(clientID, new VCInfo(false, false, false)).inVC && !clientID.equals(senderID)) {
                // so it doesnt send to other users (not in vc) and so it doesnt send to itself
                String[] parts = clientID.split(":");
                InetAddress clientAddress = InetAddress.getByName(parts[0].replace("/", ""));
                int clientPort = Integer.parseInt(parts[1]);

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                serverSocket.send(sendPacket);
            }
        }
    }
    public static void broadcastVCUsers(DatagramSocket serverSocket, Map<String, String> userMap, Map<String, VCInfo> vcStatus) throws IOException {
        // broadcasts the users who are in vc with their "status" (mute and deaf) as json
        Gson gson = new Gson();

        List<Map<String, Object>> vcList = new ArrayList<>();
        for (Map.Entry<String, VCInfo> entry : vcStatus.entrySet()) {
            String clientID = entry.getKey();
            VCInfo info = entry.getValue();

            if (info.inVC) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("username", userMap.get(clientID)); // actual username
                obj.put("inVC", info.inVC);
                obj.put("mute", info.mute);
                obj.put("deaf", info.deaf);
                vcList.add(obj);
            }
        }

        // converts to json using googles gson (automatically escapes usernames as well)
        String json = gson.toJson(vcList);
        String message = "vcusers|" + json;

        // send to everyone
        broadcast(serverSocket, userMap, message);
    }
    public static void broadcastUserList(DatagramSocket serverSocket, Map<String, String> userMap) throws IOException {
        // broadcasts the list of all users as json (auto escaped)
        Gson gson = new Gson();
        String json = gson.toJson(userMap.values());
        broadcast(serverSocket, userMap, "users|" + json);
    }
    public static void sendToSingle(DatagramSocket serverSocket, String clientID, String message, Map<String, String> lastSent) throws IOException {
        // sends a message to a single client for example auth|request
        String[] parts = clientID.split(":");
        InetAddress clientAddress = InetAddress.getByName(parts[0].replace("/", ""));
        int clientPort = Integer.parseInt(parts[1]);

        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
        serverSocket.send(sendPacket);

        // puts the message in lastSent so the user doesnt get kicked when it is auth|request
        lastSent.put(clientID, message);
    }
    public static String genSalt(int length) {
        // generates a <length> digits long salt
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
    public static void stop(String reason) {
        // handles server stopping (by user mainly)
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }

        handleStop(reason);
    }
}