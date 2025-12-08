package org.paul8711gamezz;

// networking imports
import java.net.*;

// other imports
import java.util.Arrays;
import java.util.Scanner;
import javax.sound.sampled.*;
import java.io.IOException;

import static org.paul8711gamezz.AESUnicode.*;

public class UDPClient {
    public static SourceDataLine speaker;
    public static boolean inVC = false;
    public static String KEY;
    public static String SALT;
    public static long lastServerPing = System.currentTimeMillis();

    public static void main(String[] args) throws SocketException, LineUnavailableException {
        // mic
        AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(micInfo);

        // speaker
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speaker.open(format);
        speaker.start();

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

            client_send(IP, port, clientSocket, "join", username);

            // wait for SK from server (blocking)
            long start = System.currentTimeMillis();
            long waitTimeoutMs = 10000; // 10s max wait for SK
            while (UDPClient.KEY == null) {
                try {
                    String data = client_receive(clientSocket);
                    Thread.sleep(50);

                    if (data != null) {
                        if (data.equals("auth|request")) {
                            Scanner authSc = new Scanner(System.in);
                            System.out.println("Enter Auth Key:");
                            String authKey = authSc.nextLine();
                            client_send(IP, port, clientSocket, "auth", authKey);
                        } else if (data.equals("auth|wrong")) {
                            System.out.println("Wrong Auth Key");
                            clientSocket.close();
                            System.exit(0);
                        } else if (data.equals("auth|ok")) {
                            System.out.println("Auth OK");
                        }
                    } else {
                        if (System.currentTimeMillis() - start > waitTimeoutMs) {
                            System.out.println("Disconnected (server timeout)");
                            clientSocket.close();
                            System.exit(0);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            startThreads(clientSocket, IP, port);

            Scanner msgScanner = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String message = msgScanner.nextLine();
                if (message.equalsIgnoreCase("/leave")) {
                    client_send(IP, port, clientSocket, "leave", username);
                    clientSocket.close();
                    System.exit(0);
                } else if (message.equalsIgnoreCase("/vc join")) {
                    inVC = true;
                    microphone.open(format);
                    microphone.start();
                    startVoiceThread(clientSocket, IP, port, microphone);
                    System.out.println("Joined Voice call");
                    client_send(IP, port, clientSocket, "vc", "join");
                    continue;
                } else if (message.equalsIgnoreCase("/vc leave")) {
                    inVC = false;
                    microphone.stop();
                    microphone.close();
                    System.out.println("Left Voice call");
                    client_send(IP, port, clientSocket, "vc", "leave");
                    continue;
                }
                client_send(IP, port, clientSocket, "chat", encrypt(message, UDPClient.SALT, UDPClient.KEY));
            }
        } else {
            System.out.println("Not connected");
        }
    }
    public static DatagramSocket client_connect(String serverIP, int serverPort) {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(5000);

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
    public static String client_receive(DatagramSocket clientSocket) throws SocketException {
        try {
            if (clientSocket.isClosed()) return null;
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                clientSocket.receive(receivePacket);
            } catch (SocketTimeoutException e) {
                return null;
            }
            byte[] packetData = receivePacket.getData();
            int packetLength = receivePacket.getLength();

            // check if it is a voice packet
            if (packetLength > 6) {
                String header = new String(packetData, 0, 6);
                if (header.equals("voice|")) {
                    byte[] audio = Arrays.copyOfRange(packetData, 6, packetLength);
                    byte[] decryptedAudio = decryptBytes(audio, UDPClient.SALT, UDPClient.KEY);

                    assert decryptedAudio != null;
                    speaker.write(decryptedAudio, 0,decryptedAudio.length);
                    return null;
                }
            }

            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

            String[] splitMessage = response.split("\\|", 2);
            String type = splitMessage[0];
            if (splitMessage.length >= 2) {
                String data = splitMessage[1];
                UDPClient.lastServerPing = System.currentTimeMillis();
                if (type.equals("chat")) {
                    String[] splitData = data.split(": ", 2);
                    String message = splitData[1];
                    String sender = splitData[0] + ": ";
                    return sender + decrypt(message, UDPClient.SALT, UDPClient.KEY);
                } else if (type.equals("sk")) {
                    String[] splitSK = data.split("\\|", 2);
                    UDPClient.SALT = splitSK[0];
                    UDPClient.KEY = splitSK[1];
                } else if (type.equals("err")) {
                    System.out.println(data);
                    clientSocket.close();
                    System.exit(0);
                } else if (type.equals("pong")) {
                    UDPClient.lastServerPing = System.currentTimeMillis();
                    return null;
                } else if (type.equals("auth")) {
                    return response;
                }
                return data;
            }
        } catch (SocketException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void startThreads(DatagramSocket clientSocket, String IP, int port) {
        Thread receiveThread = new Thread(() -> {
            while (true) {
                try {
                    String msg = client_receive(clientSocket);
                    if (msg != null) {
                        System.out.println("\n" + msg);
                        System.out.println(">");
                    }
                } catch (SocketException e) {
                    break;
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

        Thread timeoutThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(2000);

                    long now = System.currentTimeMillis();
                    if (now - lastServerPing > 15000) { // 15-second timeout
                        System.out.println("Disconnected (server timeout)");
                        clientSocket.close();
                        System.exit(0);
                    }
                }
            } catch (InterruptedException e) {
                // Thread interrupt
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }
    public static void startVoiceThread(DatagramSocket clientSocket, String serverIP, int serverPort, TargetDataLine microphone) {
        Thread voiceThread = new Thread(() -> {
            byte[] audioBuffer = new byte[512]; // the smaller, the lower latency, although if I make it smaller the client crashes
            String header = "voice|";
            byte[] headerBytes = header.getBytes();

            try {
                InetAddress serverAddress = InetAddress.getByName(serverIP);
                while (inVC) {
                    int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                    if (bytesRead > 0) {
                        byte[] audioData = Arrays.copyOf(audioBuffer, bytesRead);

                        byte[] encryptedAudio = encryptBytes(audioData, UDPClient.SALT, UDPClient.KEY);
                        assert encryptedAudio != null;
                        byte[] sendData = new byte[headerBytes.length + encryptedAudio.length];
                        System.arraycopy(headerBytes, 0, sendData, 0, headerBytes.length);
                        System.arraycopy(encryptedAudio, 0, sendData, headerBytes.length, encryptedAudio.length);

                        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                        clientSocket.send(packet);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        voiceThread.setDaemon(true);
        voiceThread.start();
    }
}

/*
TODO:
TUI
(mobile app?)
 */