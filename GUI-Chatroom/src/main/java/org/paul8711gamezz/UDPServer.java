package org.paul8711gamezz;

import org.paul8711gamezz.helpers.ServerUIHandler;
import org.paul8711gamezz.helpers.Hash;
import org.paul8711gamezz.helpers.VCInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.security.SecureRandom;

public class UDPServer {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    private static final String SALT = genSK(32);
    private static final String KEY = genSK(32);

    public static DatagramSocket serverSocket;

    public static ServerUIHandler uiHandler;

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
    public static void handleVCListUpdate(Map<String, VCInfo> vcStatus) {
        if (uiHandler != null) uiHandler.onVCListUpdate(vcStatus);
        else System.out.println(vcStatus);
    }
    public static void handleLog(String msg) {
        if (uiHandler != null) uiHandler.onLog(msg);
        else System.out.println(msg);
    }

    public static void runServer(int port, String authKey) throws Exception {
        if (!authKey.isEmpty()) {
            server(port, Hash.hash(authKey));
        } else {
            server(port, "");
        }
    }

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
            Map<String, String> userMap = new HashMap<>();
            Map<String, Long> lastSeen = new HashMap<>();
            Map<String, VCInfo> vcStatus = new HashMap<>();

            byte[] receiveData = new byte[1024];
            Map<String, String> lastSent = new HashMap<>(); // <clientID, lastMessageSent>

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
                                    userMap.remove(clientID);
                                    lastSeen.remove(clientID);
                                    vcStatus.remove(clientID);
                                    handleUserListUpdate(userMap);
                                    handleVCListUpdate(vcStatus);
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

                String[] splitMessage = message.split("\\|", 2);
                String type = splitMessage[0];
                String data = splitMessage[1];

                if (type.equals("join")) {
                    if (!userMap.containsValue(data)) {
                        userMap.put(clientID, data);
                        lastSeen.put(clientID, System.currentTimeMillis());
                        if (!authKeyHash.isEmpty()) {
                            sendToSingle(serverSocket, clientID, "auth|request", lastSent);
                        } else {
                            handleLog("User" + data + " joined");
                            sendToSingle(serverSocket, clientID, "sk|" + UDPServer.SALT + "|" + UDPServer.KEY, lastSent);
                            broadcast(serverSocket, userMap, "join|" + "User " + data + " joined");
                            broadcastUserList(serverSocket, userMap);
                            handleUserListUpdate(userMap);
                            handleVCListUpdate(vcStatus);
                        }
                    } else {
                        sendToSingle(serverSocket, clientID, "err|" + "Username already in use", lastSent);
                    }
                } else if (type.equals("auth")) {
                    if (!Hash.verify(data, authKeyHash)) {
                        handleLog("User " + userMap.get(clientID) + " got the auth key wrong");
                        sendToSingle(serverSocket, clientID, "auth|wrong", lastSent);
                        userMap.remove(clientID);
                        lastSeen.remove(clientID);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus);
                    } else {
                        sendToSingle(serverSocket, clientID, "auth|ok", lastSent);
                        handleLog("User " + userMap.get(clientID) + " joined");
                        sendToSingle(serverSocket, clientID, "sk|" + UDPServer.SALT + "|" + UDPServer.KEY, lastSent);
                        broadcast(serverSocket, userMap, "join|" + "User " + userMap.get(clientID) + " joined");
                        broadcastUserList(serverSocket, userMap);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus);
                    }
                } else if (type.equals("chat")) {
                    String username = userMap.get(clientID);
                    lastSeen.put(clientID, System.currentTimeMillis());
                    handleLog(username + ": " + data);
                    broadcast(serverSocket, userMap, "chat|" + username + ": " + data);
                } else if (type.equals("leave")) {
                    String username = userMap.get(clientID);
                    if (username != null) {
                        userMap.remove(clientID);
                        vcStatus.remove(clientID);
                        broadcast(serverSocket, userMap, "leave|" + "User " + username + " left");
                        broadcastUserList(serverSocket, userMap);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus);
                    }
                } else if (type.equals("ping")) {
                    lastSeen.put(clientID, System.currentTimeMillis());
                    sendToSingle(serverSocket, clientID, "pong|", lastSent);
                } else if (type.equals("vc")) {
                    if (data.equals("join")) {
                        vcStatus.put(clientID, new VCInfo(true, false, false));
                        broadcast(serverSocket, userMap, "User " + userMap.get(clientID) + " joined Voice call");
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus);
                    } else if (data.equals("leave")) {
                        vcStatus.put(clientID, new VCInfo(false, false, false));
                        broadcast(serverSocket, userMap, "User " + userMap.get(clientID) + " left Voice call");
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus);
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                    } else if (data.equals("mute")) {
                        VCInfo info = vcStatus.get(clientID);
                        info.mute = !info.mute;
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus);
                    } else if (data.equals("deaf")) {
                        VCInfo info = vcStatus.get(clientID);
                        info.deaf = !info.deaf;
                        broadcastVCUsers(serverSocket, userMap, vcStatus, lastSent);
                        handleUserListUpdate(userMap);
                        handleVCListUpdate(vcStatus);
                    }
                }
            }
        } catch (Exception e) {
            if (!serverSocket.isClosed()) handleError("Error: " + e.getMessage());
        }
    }
    public static void broadcast(DatagramSocket serverSocket, Map<String, String> userMap, String message) throws IOException {
        // answer creation
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
        for (String clientID : userMap.keySet()) {
            if (vcStatus.getOrDefault(clientID, new VCInfo(false, false, false)).inVC && !clientID.equals(senderID)) {
                String[] parts = clientID.split(":");
                InetAddress clientAddress = InetAddress.getByName(parts[0].replace("/", ""));
                int clientPort = Integer.parseInt(parts[1]);

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                serverSocket.send(sendPacket);
            }
        }
    }
    public static void broadcastVCUsers(DatagramSocket serverSocket, Map<String, String> userMap, Map<String, VCInfo> vcStatus, Map<String, String> lastSent) throws IOException {
        StringBuilder sb = new StringBuilder(); // sb stands for shitbull (bullshit in reverse)
        sb.append("vcstatus|");

        for (Map.Entry<String, VCInfo> entry : vcStatus.entrySet()) {
            String user = entry.getKey();
            VCInfo info = entry.getValue();

            // only include if in vc
            sb.append(user)
                    .append(",")
                    .append(info.inVC)
                    .append(",")
                    .append(info.mute)
                    .append(",")
                    .append(info.deaf)
                    .append(";");
        }
        for (String clientID : userMap.keySet()) {
            if (vcStatus.getOrDefault(clientID, new VCInfo(false, false, false)).inVC) {

                sendToSingle(serverSocket, clientID, sb.toString(), lastSent);
            }
        }
    }
    public static void broadcastUserList(DatagramSocket serverSocket, Map<String, String> userMap) throws IOException {
        String list = String.join(",", userMap.values());
        broadcast(serverSocket, userMap, "users|" + list);
    }
    public static void sendToSingle(DatagramSocket serverSocket, String clientID, String message, Map<String, String> lastSent) throws IOException {
        String[] parts = clientID.split(":");
        InetAddress clientAddress = InetAddress.getByName(parts[0].replace("/", ""));
        int clientPort = Integer.parseInt(parts[1]);

        byte[] sendData = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
        serverSocket.send(sendPacket);

        lastSent.put(clientID, message);
    }
    public static String genSK(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
    public static void stop(String reason) {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}

        handleStop(reason);
    }
}