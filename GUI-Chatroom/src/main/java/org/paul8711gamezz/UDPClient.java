package org.paul8711gamezz;

// networking imports
import org.paul8711gamezz.helpers.ClientUIHandler;

import java.net.*;

// other imports
import java.util.Arrays;
import java.util.Scanner;
import javax.sound.sampled.*;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static org.paul8711gamezz.helpers.AESUnicode.*;
import static org.paul8711gamezz.helpers.DefaultKeyManager.getDefaultKey;

public class UDPClient {
    public static SourceDataLine speaker;
    public static boolean inVC = false;
    public static String KEY;
    public static String SALT;
    public static String DEFAULT_KEY = "";
    public static long lastServerPing = System.currentTimeMillis();

    public static boolean mute = false;
    public static boolean deaf = false;

    public static DatagramSocket clientSocket;
    public static String IP;
    public static int port;
    public static String username;

    public static TargetDataLine microphone;
    public static AudioFormat format;


    public static ClientUIHandler uiHandler;

    public static void setUiHandler(ClientUIHandler handler) {
        uiHandler = handler;
    }

    public static void handleError(String msg) {
        if (uiHandler != null) uiHandler.onError(msg);
        else System.out.println("Error" + msg);
    }
    public static void handleDisconnect(String reason) {
        UDPClient.KEY = null;
        UDPClient.SALT = null;
        if (uiHandler != null) uiHandler.onDisconnect(reason);
        else System.out.println("Disconnected" + reason);
    }
    public static void handleMessage(String msg) {
        if (uiHandler != null) uiHandler.onMessage(msg);
        else System.out.println(msg);
    }
    public static void handleVCUsersUpdate(String VCUsers) {
        if (uiHandler != null) uiHandler.onVCListUpdate(VCUsers);
        else System.out.println(VCUsers);
    }
    public static void handleUsersUpdate(String users) {
        if (uiHandler != null) uiHandler.onUserListUpdate(users);
        else System.out.println(users);
    }
    public static void sendMessage(String msg) {
        try {
            if (msg.equalsIgnoreCase("/leave")) {
                client_send(IP, port, clientSocket, "leave", username);
                handleDisconnect("Closed by User");
                clientSocket.close();
                return;
            } else if (msg.equalsIgnoreCase("/vc join")) {
                inVC = true;
                microphone.open(format);
                microphone.start();
                startVoiceThread(clientSocket, IP, port, microphone);
                handleMessage("Joined Voice Call");
                client_send(IP, port, clientSocket, "vc", "join");
                return;
            } else if (msg.equalsIgnoreCase("/vc leave")) {
                inVC = false;
                mute = false;
                deaf = false;
                microphone.stop();
                microphone.close();
                handleMessage("Left Voice Call");
                client_send(IP, port, clientSocket, "vc", "leave");
                return;
            } else if (msg.equalsIgnoreCase("/vc mute") && inVC) {
                mute = !mute;
                if (mute) {
                    handleMessage("You have muted yourself");
                    client_send(IP, port, clientSocket, "vc", "mute");
                } else {
                    handleMessage("You have unmuted yourself");
                    client_send(IP, port, clientSocket, "vc", "mute");
                }
                return;
            } else if (msg.equalsIgnoreCase("/vc deaf") && inVC) {
                deaf = !deaf;
                if (deaf) {
                    handleMessage("You have deafened yourself");
                    client_send(IP, port, clientSocket, "vc", "deaf");
                } else {
                    handleMessage("You have undeafened yourself");
                    client_send(IP, port, clientSocket, "vc", "deaf");
                }
                return;
            }
            client_send(IP, port, clientSocket, "chat", encrypt(msg, UDPClient.SALT, UDPClient.KEY.isEmpty() ? UDPClient.DEFAULT_KEY : UDPClient.KEY));
        } catch (Exception e) {
            handleError(e.getMessage());
        }
    }

    public static void runClient(String IP, int port, String username) throws LineUnavailableException {
        // mic
        format = new AudioFormat(44100.0f, 16, 1, true, false);
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(micInfo);

        // speaker
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speaker.open(format);
        speaker.start();

        UDPClient.IP = IP;
        UDPClient.port = port;
        UDPClient.username = username;

        try {
            clientSocket = client_connect(IP, port);
            if (clientSocket == null) {
                handleError("Could not connect to server");
            }

            JsonObject keyData = getDefaultKey();
            if (keyData != null) {
                UDPClient.DEFAULT_KEY = keyData.get("key").getAsString();
            } else {
                handleError("Could not download key");
            }
            client_send(IP, port, clientSocket, "join", username);

            // wait for salt from server (blocking)
            long start = System.currentTimeMillis();
            long waitTimeoutMs = 10000; // 10s max wait for salt
            while (UDPClient.SALT == null && !clientSocket.isClosed()) {
                try {
                    String data = client_receive(clientSocket);
                    Thread.sleep(50);

                    if (data != null) {
                        switch (data) {
                            case "auth|request" -> {
                                start = System.currentTimeMillis();
                                if (uiHandler != null) {
                                    uiHandler.onAuthRequest(authKey -> {
                                        UDPClient.KEY = authKey;
                                        client_send(IP, port, clientSocket, "auth", authKey);
                                    });
                                }
                            }
                            case "auth|wrong" -> {
                                start = System.currentTimeMillis();
                                disconnect("Wrong Auth Key");
                            }
                            case "auth|ok" -> {
                                if (uiHandler != null) {
                                    uiHandler.onAuthCorrect();
                                }
                            }
                        }
                    } else {
                        if (System.currentTimeMillis() - start > waitTimeoutMs && !clientSocket.isClosed()) {
                            disconnect("Server timeout");
                            break;
                        }
                    }
                } catch (Exception e) {
                    handleError(e.getMessage());
                }
            }

            startThreads(clientSocket, IP, port);
        } catch (Exception e) {
            handleError(e.getMessage());
        }
    }

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

        clientSocket = client_connect(IP, port);

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
                    mute = false;
                    deaf = false;
                    microphone.stop();
                    microphone.close();
                    System.out.println("Left Voice call");
                    client_send(IP, port, clientSocket, "vc", "leave");
                    continue;
                } else if (message.equalsIgnoreCase("/vc mute") && inVC) {
                    mute = !mute;
                    if (mute) {
                        System.out.println("You have muted yourself");
                        client_send(IP, port, clientSocket, "vc", "mute");
                    } else {
                        System.out.println("You have unmuted yourself");
                        client_send(IP, port, clientSocket, "vc", "mute");
                    }
                    continue;
                } else if (message.equalsIgnoreCase("/vc deaf") && inVC) {
                    deaf = !deaf;
                    if (deaf) {
                        System.out.println("You have deafened yourself");
                        client_send(IP, port, clientSocket, "vc", "deaf");
                    } else {
                        System.out.println("You have undeafened yourself");
                        client_send(IP, port, clientSocket, "vc", "deaf");
                    }
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
            handleError(e.getMessage());
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
            handleError("Error: "+ e.getMessage());
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
                if (header.equals("voice|") && !deaf) {
                    byte[] audio = Arrays.copyOfRange(packetData, 6, packetLength);
                    byte[] decryptedAudio = decryptBytes(audio, UDPClient.SALT, UDPClient.KEY.isEmpty() ? UDPClient.DEFAULT_KEY : UDPClient.KEY);

                    assert decryptedAudio != null;
                    speaker.write(decryptedAudio, 0, decryptedAudio.length);
                    return null;
                } else if (header.equals("voice|")) {
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
                return sender + decrypt(message, UDPClient.SALT, UDPClient.KEY.isEmpty() ? UDPClient.DEFAULT_KEY : UDPClient.KEY);
                } else if (type.equals("salt")) {
                    UDPClient.SALT = data;
                    if (UDPClient.KEY == null) UDPClient.KEY = "";
                } else if (type.equals("err")) {
                    clientSocket.close();
                    handleError(data);
                } else if (type.equals("pong")) {
                    UDPClient.lastServerPing = System.currentTimeMillis();
                    return null;
                } else if (type.equals("auth")) {
                    return response;
                } else if (type.equals("vcusers")) {
                    handleVCUsersUpdate(data);
                    return null;
                } else if (type.equals("users")) {
                    handleUsersUpdate(data);
                    return null;
                }
                return data;
            }
        } catch (SocketException e) {
            if (!clientSocket.isClosed()) {
                handleError(e.getMessage());
            }
        } catch (Exception e) {
            handleError(e.getMessage());
        }
        return null;
    }
    public static void startThreads(DatagramSocket clientSocket, String IP, int port) {
        Thread receiveThread = new Thread(() -> {
            while (!clientSocket.isClosed()) {
                try {
                    String msg = client_receive(clientSocket);
                    if (msg != null) {
                        handleMessage(msg);
                    }
                } catch (SocketException e) {
                    break;
                } catch (Exception e) {
                    handleError(e.getMessage());
                }
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();

        Thread pingThread = new Thread(() -> {
            try {
                while (!clientSocket.isClosed()) {
                    Thread.sleep(5000); // every 5 seconds
                    if (clientSocket.isClosed()) break;
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
                while (!clientSocket.isClosed()) {
                    Thread.sleep(2000);

                    long now = System.currentTimeMillis();
                    if (now - lastServerPing > 15000) { // 15-second timeout
                        disconnect("Server timeout");
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
                while (inVC && !mute && !clientSocket.isClosed()) {
                    int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                    if (bytesRead > 0) {
                        byte[] audioData = Arrays.copyOf(audioBuffer, bytesRead);

                        byte[] encryptedAudio = encryptBytes(audioData, UDPClient.SALT, UDPClient.KEY.isEmpty() ? UDPClient.DEFAULT_KEY : UDPClient.KEY);
                        assert encryptedAudio != null;
                        byte[] sendData = new byte[headerBytes.length + encryptedAudio.length];
                        System.arraycopy(headerBytes, 0, sendData, 0, headerBytes.length);
                        System.arraycopy(encryptedAudio, 0, sendData, headerBytes.length, encryptedAudio.length);

                        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                        clientSocket.send(packet);
                    }
                }
            } catch (Exception e) {
                handleError(e.getMessage());
            }
        });
        voiceThread.setDaemon(true);
        voiceThread.start();
    }
    public static void disconnect(String reason) {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                UDPClient.KEY = null;
                UDPClient.SALT = null;
                client_send(IP, port, clientSocket, "leave", username);
                clientSocket.close();
            }
        } catch (Exception ignored) {}

        handleDisconnect(reason);
    }
}

/*
TODO:
 - mobile app
 - automatic updates
 - port to python
 */