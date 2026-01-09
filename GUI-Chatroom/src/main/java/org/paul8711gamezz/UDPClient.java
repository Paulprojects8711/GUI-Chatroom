package org.paul8711gamezz;

import com.google.gson.JsonObject;
import org.paul8711gamezz.helpers.ClientUIHandler;
import org.paul8711gamezz.helpers.MicrophoneItem;
import org.paul8711gamezz.helpers.SpeakerItem;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static org.paul8711gamezz.helpers.AESUnicode.*;
import static org.paul8711gamezz.helpers.DefaultKeyManager.getDefaultKey;

public class UDPClient {
    public static boolean inVC = false;
    public static String KEY;
    public static String SALT;
    public static String DEFAULT_KEY = "";
    public static long lastServerPing = System.currentTimeMillis();
    public static boolean authRequestReceived = false;

    public static boolean mute = false;
    public static boolean deaf = false;

    public static DatagramSocket clientSocket;
    public static String IP;
    public static int port;
    public static String username;

    // audio stuff
    public static TargetDataLine microphone;
    public static SourceDataLine speaker;
    public static AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);;
    public static boolean micAvailable;
    public static boolean speakerAvailable;

    public static ClientUIHandler uiHandler;

    // this section just passes everything to the client ui handler
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
            // if special type of message (like /leave) do a different action than broadcasting to the rest of the members
            if (msg.equalsIgnoreCase("/leave")) {
                client_send(IP, port, clientSocket, "leave", username);
                handleDisconnect("Closed by User");
                clientSocket.close();
                return;
            } else if (msg.equalsIgnoreCase("/vc join")) {
                if (GUI.audioIO) {
                    inVC = true;
                    microphone.open(format);
                    microphone.start();
                    startVoiceThread(clientSocket, IP, port, microphone);
                    handleMessage("Joined Voice Call");
                    client_send(IP, port, clientSocket, "vc", "join");
                    return;
                }
            } else if (msg.equalsIgnoreCase("/vc leave") && inVC) {
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
            // otherwise just broadcast like normal
            client_send(IP, port, clientSocket, "chat", encrypt(msg, UDPClient.SALT, UDPClient.KEY.isEmpty() ? UDPClient.DEFAULT_KEY : UDPClient.KEY));
        } catch (Exception e) {
            handleError(e.getMessage());
        }
    }

    public static void runClient(String IP, int port, String username) {
        // mic
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        microphone = null;

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            try {
                microphone = (TargetDataLine) mixer.getLine(micInfo);
                microphone.open(format);
                micAvailable = true;
                break; // success
            } catch (LineUnavailableException | IllegalArgumentException ignored) {
                // try next mixer
            }
        }

        if (microphone == null) {
            micAvailable = false;
        }

        // speaker
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        speaker = null;

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            try {
                speaker = (SourceDataLine) mixer.getLine(speakerInfo);
                speaker.open(format);
                speaker.start();
                speakerAvailable = true;
                break; // success
            } catch (LineUnavailableException | IllegalArgumentException ignored) {
                // try next mixer
            }
        }

        if (speaker == null) {
            speakerAvailable = false;
        }

        UDPClient.IP = IP;
        UDPClient.port = port;
        UDPClient.username = username;

        try {
            clientSocket = client_connect(IP, port);
            if (clientSocket == null) {
                // i dont think that this can actually happen but ill leave it in
                handleDisconnect("Could not connect to server");
            }

            JsonObject keyData = getDefaultKey();
            if (keyData != null) {
                // sets the default key (if you dont set a room password)
                UDPClient.DEFAULT_KEY = keyData.get("key").getAsString();
            } else {
                handleError("Could not download key");
            }
            client_send(IP, port, clientSocket, "join", username);

            // wait for salt from server
            long start = System.currentTimeMillis();
            long waitTimeoutMs = 10000; // 10s max wait for salt
            while (UDPClient.SALT == null && !clientSocket.isClosed()) {
                try {
                    String data = client_receive(clientSocket);
                    Thread.sleep(50);

                    if (data != null) {
                        // if the server responds with something other than the salt it is probably one of those
                        switch (data) {
                            case "auth|request" -> {
                                // so we dont timeout while waiting for the password
                                authRequestReceived = true;
                                start = System.currentTimeMillis();
                                if (uiHandler != null) {
                                    uiHandler.onAuthRequest(authKey -> {
                                        UDPClient.KEY = authKey;
                                        client_send(IP, port, clientSocket, "auth", authKey);
                                    });
                                }
                            }
                            case "auth|wrong" -> {
                                // self-explanatory
                                authRequestReceived = false;
                                start = System.currentTimeMillis();
                                disconnect("Wrong Auth Key");
                            }
                            case "auth|ok" -> {
                                // also self-explanatory
                                authRequestReceived = false;
                                if (uiHandler != null) {
                                    uiHandler.onAuthCorrect();
                                }
                            }
                        }
                    } else {
                        if (!authRequestReceived && System.currentTimeMillis() - start > waitTimeoutMs && !clientSocket.isClosed()) {
                            // if the server takes longer than 10 sec to respond disconnect (timeout)
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

    // used when the gui didnt exist
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

            // wait for SK from server
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
        // the ip and port are actually completely useless as parameters but i guess if you want to print them???
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            // sets timeout to 5000 ms (5s)
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
        // function used for sending stuff to the server
        try {
            // packet format: type|msg for example: chat|hi (because chat message with content hi)
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
        // function used to receive stuff from the server
        try {
            if (clientSocket.isClosed()) return null;
            // maximum size of packet you can receive (anything afterwards will just be gone, breaking the encryption in the process)
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
                // if you are not deaf and the type is voice write the bytes to the speaker
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

            // split the message at the | so you can get the type and data separately
            String[] splitMessage = response.split("\\|", 2);
            String type = splitMessage[0];
            if (splitMessage.length >= 2) {
                String data = splitMessage[1];
                // logs the last time a message from the server was received for timeout reasons
                UDPClient.lastServerPing = System.currentTimeMillis();
                if (type.equals("chat")) {
                    String[] splitData = data.split(": ", 2);
                    String message = splitData[1];
                    String sender = splitData[0] + ": ";
                    // only decrypts the message not the sender
                    return sender + decrypt(message, UDPClient.SALT, UDPClient.KEY.isEmpty() ? UDPClient.DEFAULT_KEY : UDPClient.KEY);
                } else if (type.equals("salt")) {
                    // sets the salt
                    UDPClient.SALT = data;
                    if (UDPClient.KEY == null) UDPClient.KEY = "";
                } else if (type.equals("err")) {
                    // error with username or something else
                    clientSocket.close();
                    if (data.equals("Username already in use")) {
                        handleDisconnect(data);
                    } else {
                        handleError(data);
                    }
                } else if (type.equals("pong")) {
                    // server response to clients ping
                    UDPClient.lastServerPing = System.currentTimeMillis();
                    return null;
                } else if (type.equals("auth")) {
                    // returns auth|request or auth|ok or auth|wrong
                    return response;
                } else if (type.equals("vcusers")) {
                    // updates the vc user list in the gui
                    handleVCUsersUpdate(data);
                    return null;
                } else if (type.equals("users")) {
                    // updates the normal user list in the gui
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
        // receiveThread for receiving stuff from the server
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

        // pings the server every 5 seconds (server response: pong|)
        Thread pingThread = new Thread(() -> {
            try {
                while (!clientSocket.isClosed()) {
                    Thread.sleep(5000); // every 5 seconds
                    if (clientSocket.isClosed()) break;
                    client_send(IP, port, clientSocket, "ping", "");
                }
            } catch (InterruptedException e) {
                // thread interrupted on exit, we dont care about that
            }
        });
        pingThread.setDaemon(true);
        pingThread.start();

        // checks if the time of the last server response is bigger than 15 seconds, if true disconnects the client
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
                // thread interrupted on exit, we still dont care about that
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }
    public static void startVoiceThread(DatagramSocket clientSocket, String serverIP, int serverPort, TargetDataLine microphone) {
        // spams server (and clients) with "voice|bytes of the audio" packets
        Thread voiceThread = new Thread(() -> {
            byte[] audioBuffer = new byte[512]; // the smaller, the lower latency, although if I make it smaller the client crashes (i think)
            String header = "voice|";
            byte[] headerBytes = header.getBytes();

            try {
                InetAddress serverAddress = InetAddress.getByName(serverIP);
                // if you are in vc and not mute (obviously) send the audio from the mic
                while (inVC && !mute && !clientSocket.isClosed()) {
                    int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                    if (bytesRead > 0) {
                        // copies the array so that it has the wanted 512 bytes as well
                        byte[] audioData = Arrays.copyOf(audioBuffer, bytesRead);

                        // encrypts the audio with the function that works for bytes
                        byte[] encryptedAudio = encryptBytes(audioData, UDPClient.SALT, UDPClient.KEY.isEmpty() ? UDPClient.DEFAULT_KEY : UDPClient.KEY);
                        assert encryptedAudio != null;
                        byte[] sendData = new byte[headerBytes.length + encryptedAudio.length];
                        // offsets the header bytes from the audio bytes
                        System.arraycopy(headerBytes, 0, sendData, 0, headerBytes.length);
                        System.arraycopy(encryptedAudio, 0, sendData, headerBytes.length, encryptedAudio.length);

                        // requires custom send function because the other one requires string and this one works with bytes
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
        // unforced disconnect (no timeouts or something like that)
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

    public static List<SpeakerItem> getSpeakers() {
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        SourceDataLine speaker;

        List<SpeakerItem> speakers = new ArrayList<>();

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            try {
                speaker = (SourceDataLine) mixer.getLine(speakerInfo);
                speaker.open(format);
                speakers.add(new SpeakerItem(mixerInfo.getName(), mixerInfo));
            } catch (LineUnavailableException | IllegalArgumentException ignored) {
                // try next mixer
            }
        }

        return speakers;
    }
    public static List<MicrophoneItem> getMicrophones() {
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        TargetDataLine microphone;

        List<MicrophoneItem> microphones = new ArrayList<>();

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            try {
                microphone = (TargetDataLine) mixer.getLine(micInfo);
                microphone.open(format);
                microphones.add(new MicrophoneItem(mixerInfo.getName(), mixerInfo));
            } catch (LineUnavailableException | IllegalArgumentException ignored) {
                // try next mixer
            }
        }

        return microphones;
    }
}