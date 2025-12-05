package org.paul8711gamezz;

// networking imports
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;

// other imports
import java.util.Scanner;

public class UDPClient {
    public static void main(String[] args) {
        Scanner ipSc = new Scanner(System.in);
        System.out.println("IP:");
        String IP = ipSc.nextLine();

        Scanner portSc = new Scanner(System.in);
        System.out.println("Port:");
        int port = Integer.parseInt(portSc.nextLine());

        Scanner userSc = new Scanner(System.in);
        System.out.println("Enter Username:");
        String username = userSc.nextLine();

        DatagramSocket clientSocket = client_connect(IP, port);

        if (clientSocket != null) {
            System.out.println("Connected");

            Thread receiveThread = new Thread(() -> {
                while (true) {
                    try {
                        String msg = client_receive(clientSocket);
                        if (msg != null) {
                            System.out.println("\n" + msg);
                            System.out.println(">");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            receiveThread.setDaemon(true);
            receiveThread.start();

            Thread pingThread = new Thread(() -> {
                try {
                    while (true) {
                        Thread.sleep(5000); // every 5 seconds
                        client_send(IP, port, clientSocket, "ping", "");
                    }
                } catch (InterruptedException e) {
                    // thread interrupted on exit
                }
            });
            pingThread.setDaemon(true);
            pingThread.start();

            client_send(IP, port, clientSocket, "join", username);

            Scanner msgScanner = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String message = msgScanner.nextLine();
                if (message.equalsIgnoreCase("/leave")) {
                    client_send(IP, port, clientSocket, "leave", username);
                    clientSocket.close();
                    System.exit(0);
                }
                client_send(IP, port, clientSocket, "chat", message);
            }
        } else {
            System.out.println("Not connected");
        }
    }
    public static DatagramSocket client_connect(String serverIP, int serverPort) {
        try {
            DatagramSocket clientSocket = new DatagramSocket();

            InetAddress serverAddress = InetAddress.getByName(serverIP);
            InetAddress address = InetAddress.getByAddress(serverAddress.getAddress());

            // System.out.println("Address: " + address);
            // System.out.println("Port: " + serverPort);

            return clientSocket;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void client_send(String serverIP, int serverPort, DatagramSocket clientSocket, String type, String msg) {
        try {
            InetAddress serverAddress = InetAddress.getByName(serverIP);
            String message = type + "|" + msg;
            byte[] sendData = message.getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            clientSocket.send(sendPacket);
            // System.out.println("Message sent: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String client_receive(DatagramSocket clientSocket) {
        try {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            // System.out.println("Received message: " + response);

            String[] splitMessage = response.split("\\|", 2);
            String type = splitMessage[0];
            String data = splitMessage[1];
            /*
            UNUSED
            switch (type) {
                case "join" -> System.out.println(data);
                case "chat" -> System.out.println(data);
                case "users" -> System.out.println(data);
                case "leave" -> System.out.println(data);
            }
             */
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}