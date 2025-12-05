package org.paul8711gamezz;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class UDPServer {
    public static void main(String[] args) {
        Scanner portSc = new Scanner(System.in);
        System.out.println("Port:");
        int port = Integer.parseInt(portSc.nextLine());

        server(port);
    }
    public static void server(int port) {
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            System.out.println("Server started on Port " + port);
            Map<String, String> userMap = new HashMap<>();
            Map<String, Long> lastSeen = new HashMap<>();

            byte[] receiveData = new byte[1024];

            new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(5000); // check every 5 seconds
                        long now = System.currentTimeMillis();
                        for (String clientID : new ArrayList<>(lastSeen.keySet())) {
                            if (now - lastSeen.get(clientID) > 15000) { // 15 sec timeout
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
                    userMap.put(clientID, data);
                    System.out.println("User " + data + " joined");
                    broadcast(serverSocket, userMap, "join|" + "User " + data + " joined");
                    broadcastUserList(serverSocket, userMap);
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
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
}