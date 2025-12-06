package org.paul8711gamezz;

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
            System.out.println("Server started on Port " + port);
            Map<String, String> userMap = new HashMap<>();
            Map<String, Long> lastSeen = new HashMap<>();

            byte[] receiveData = new byte[1024];
            Map<String, String> lastSent = new HashMap<>(); // <clientID, lastMessageSent>

            new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(5000); // check every 5 seconds
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
                                    broadcast(serverSocket, userMap, "leave|" + username + " disconnected (timeout)");
                                    broadcastUserList(serverSocket, userMap);
                                }
                            }
                        }
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Received message: " + message);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String clientID = clientAddress.toString() + ":" + clientPort;

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
                            System.out.println("User " + data + " joined");
                            sendToSingle(serverSocket, clientID, "sk|" + UDPServer.SALT + "|" + UDPServer.KEY, lastSent);
                            broadcast(serverSocket, userMap, "join|" + "User " + data + " joined");
                            broadcastUserList(serverSocket, userMap);
                        }
                    } else {
                        sendToSingle(serverSocket, clientID, "err|" + "Username already in use", lastSent);
                    }
                } else if (type.equals("auth")) {
                    if (!Hash.verify(data, authKeyHash)) {
                        sendToSingle(serverSocket, clientID, "auth|wrong", lastSent);
                        userMap.remove(clientID);
                        lastSeen.remove(clientID);
                    } else {
                        sendToSingle(serverSocket, clientID, "auth|ok", lastSent);
                        System.out.println("User " + userMap.get(clientID) + " joined");
                        sendToSingle(serverSocket, clientID, "sk|" + UDPServer.SALT + "|" + UDPServer.KEY, lastSent);
                        broadcast(serverSocket, userMap, "join|" + "User " + userMap.get(clientID) + " joined");
                        broadcastUserList(serverSocket, userMap);
                    }
                } else if (type.equals("chat")) {
                    String username = userMap.get(clientID);
                    lastSeen.put(clientID, System.currentTimeMillis());
                    System.out.println(username + ": " + data);
                    broadcast(serverSocket, userMap, "chat|" + username + ": " + data);
                } else if (type.equals("leave")) {
                    String username = userMap.get(clientID);
                    if (username != null) {
                        userMap.remove(clientID);
                        broadcast(serverSocket, userMap, "leave|" + "User " + username + " left");
                        broadcastUserList(serverSocket, userMap);
                    }
                } else if (type.equals("ping")) {
                    lastSeen.put(clientID, System.currentTimeMillis());
                    sendToSingle(serverSocket, clientID, "pong|", lastSent);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
}