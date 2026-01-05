package org.paul8711gamezz;

// imports

import com.formdev.flatlaf.FlatDarkLaf;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.paul8711gamezz.helpers.ClientUIHandler;
import org.paul8711gamezz.helpers.ServerUIHandler;
import org.paul8711gamezz.helpers.UpdateManager;
import org.paul8711gamezz.helpers.VCInfo;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GUI {
    private static int portNumber;
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to setup FaL");
        }

        JFrame frame = new JFrame("GUI-Chatroom");

        Image icon = Toolkit.getDefaultToolkit().getImage(GUI.class.getResource("/icon.png"));
        frame.setIconImage(icon);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout(10, 10));

        // Check for updates in the background
        Executors.newSingleThreadExecutor().submit(() -> checkForUpdates());

        JPanel cards = new JPanel(new CardLayout());

        // panel configs for rooms
        JPanel roomJoin = new JPanel();
        JPanel chat = new JPanel();
        JPanel server = new JPanel();
        // serverConfig
        JPanel serverConfig = new JPanel();
        JPasswordField authKey = new JPasswordField(15);
        JPanel authKeyRow = createInputRow("Key:", authKey);

        // back button action
        ActionListener backAction = e -> {
            CardLayout cl = (CardLayout)(cards.getLayout());
            cl.show(cards, "selectScreen");

            clearForm(roomJoin);
            clearForm(serverConfig);
            authKeyRow.setVisible(false);
            clearForm(chat);
            clearForm(server);
        };

        // ===========================
        // ====== SELECT SCREEN ======
        // ===========================
        JPanel selectScreen = new JPanel();
        selectScreen.setLayout(new BoxLayout(selectScreen, BoxLayout.Y_AXIS)); // vertical layout

        // title
        JLabel titleLabel1 = new JLabel("GUI-Chatroom", SwingConstants.CENTER);
        titleLabel1.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel1.setFont(new Font("Arial", Font.BOLD, 30));
        selectScreen.add(titleLabel1);

        // spacing
        selectScreen.add(Box.createVerticalStrut(10)); // in px

        // description
        JLabel descLabel = new JLabel("An end-to-end encrypted Graphical User Interface Chatroom", SwingConstants.CENTER);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        selectScreen.add(descLabel);

        // spacing
        selectScreen.add(Box.createVerticalStrut(76));

        // join btn
        JButton joinButton1 = new JButton("Join");
        joinButton1.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectScreen.add(joinButton1);

        // spacing
        selectScreen.add(Box.createVerticalStrut(30)); // in px

        // host btn
        JButton hostButton1 = new JButton("Host");
        hostButton1.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectScreen.add(hostButton1);

        selectScreen.add(Box.createVerticalStrut(50));

        JLabel versionLabel = new JLabel("v1.2", SwingConstants.CENTER);
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        selectScreen.add(versionLabel);

        selectScreen.add(Box.createVerticalStrut(10));

        JLabel creditLabel = new JLabel("by Paul8711gamezz", SwingConstants.CENTER);
        creditLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        creditLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        selectScreen.add(creditLabel);

        cards.add(selectScreen, "selectScreen");

        joinButton1.addActionListener(e -> {
            CardLayout cl = (CardLayout)(cards.getLayout());
            cl.show(cards, "roomJoin");
        });

        hostButton1.addActionListener(e -> {
            CardLayout cl = (CardLayout)(cards.getLayout());
            cl.show(cards, "serverConfig");
        });

        // ===========================
        // ======= JOIN SCREEN =======
        // ===========================
        roomJoin.setLayout(new BoxLayout(roomJoin, BoxLayout.Y_AXIS)); // vertical layout

        roomJoin.add(createTopBar("Join Room", backAction));

        // spacing
        roomJoin.add(Box.createVerticalStrut(50)); // in px

        // Main vertical container
        JPanel inputPanel1 = new JPanel();
        inputPanel1.setLayout(new BoxLayout(inputPanel1, BoxLayout.Y_AXIS));
        inputPanel1.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create input fields
        JTextField ip = new JTextField(15);
        JTextField port1 = new JTextField(15);
        JTextField username = new JTextField(15);

        inputPanel1.add(createInputRow("IP:", ip));
        inputPanel1.add(Box.createVerticalStrut(10));
        inputPanel1.add(createInputRow("Port:", port1));
        inputPanel1.add(Box.createVerticalStrut(10));
        inputPanel1.add(createInputRow("Username:", username));

        inputPanel1.setMaximumSize(inputPanel1.getPreferredSize());

        roomJoin.add(inputPanel1);

        roomJoin.add(Box.createVerticalStrut(20));

        // join btn
        JButton joinButton2 = new JButton("Join");
        joinButton2.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomJoin.add(joinButton2);

        cards.add(roomJoin, "roomJoin");


        // ===========================
        // ======= HOST SCREEN =======
        // ===========================
        serverConfig.setLayout(new BoxLayout(serverConfig, BoxLayout.Y_AXIS)); // vertical layout

        serverConfig.add(createTopBar("Server Configuration", backAction));

        // spacing
        serverConfig.add(Box.createVerticalStrut(50)); // in px

        // Main vertical container
        JPanel inputPanel2 = new JPanel();
        inputPanel2.setLayout(new BoxLayout(inputPanel2, BoxLayout.Y_AXIS));
        inputPanel2.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create input fields
        JTextField port2 = new JTextField(15);
        JCheckBox requiresAuth = new JCheckBox("Requires Auth");
        authKeyRow.setVisible(true);

        requiresAuth.addActionListener(e -> {
            authKeyRow.setVisible(requiresAuth.isSelected());
            inputPanel2.revalidate();
            inputPanel2.repaint();
        });

        inputPanel2.add(createInputRow("Port:", port2));
        inputPanel2.add(Box.createVerticalStrut(10));
        inputPanel2.add(requiresAuth);
        inputPanel2.add(Box.createVerticalStrut(10));
        inputPanel2.add(authKeyRow);

        inputPanel2.setMaximumSize(inputPanel2.getPreferredSize());

        authKeyRow.setVisible(false);

        serverConfig.add(inputPanel2);

        serverConfig.add(Box.createVerticalStrut(20));

        // host btn
        JButton hostButton2 = new JButton("Host");
        hostButton2.setAlignmentX(Component.CENTER_ALIGNMENT);
        serverConfig.add(hostButton2);

        cards.add(serverConfig, "serverConfig");

        // ===========================
        // ======= CHAT SCREEN =======
        // ===========================
        chat.setLayout(new BoxLayout(chat, BoxLayout.Y_AXIS)); // vertical layout

        JPanel topBar1 = new JPanel(new BorderLayout());
        topBar1.setPreferredSize(new Dimension(Integer.MAX_VALUE, 35));
        topBar1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // LEFT: Back button with padding
        JPanel leftPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton backButton1 = new JButton("Leave");
        leftPanel1.add(backButton1);
        topBar1.add(leftPanel1, BorderLayout.WEST);

        // CENTER: title
        JLabel titleLabel2 = new JLabel("Chatroom", SwingConstants.CENTER);
        titleLabel2.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel2.setPreferredSize(new Dimension(titleLabel2.getPreferredSize().width, 25));
        topBar1.add(titleLabel2, BorderLayout.CENTER);

        // ===== RIGHT SPACER =====
        JPanel rightSpacer1 = new JPanel();
        rightSpacer1.setPreferredSize(leftPanel1.getPreferredSize());
        topBar1.add(rightSpacer1, BorderLayout.EAST);


        JPanel infoPanel1 = new JPanel();
        infoPanel1.setLayout(new BoxLayout(infoPanel1, BoxLayout.X_AXIS));

        // Checkbox (fixed position)
        JCheckBox showAddrBox1 = new JCheckBox("Show connection details");
        showAddrBox1.setFont(new Font("Arial", Font.BOLD, 12));
        infoPanel1.add(showAddrBox1);
        infoPanel1.add(Box.createVerticalStrut(10));

        // Stacked IP + Port
        JPanel infoTextPanel1 = new JPanel();
        infoTextPanel1.setLayout(new BoxLayout(infoTextPanel1, BoxLayout.X_AXIS));

        JLabel ipLabel1 = new JLabel("IP: ");
        JLabel portLabel1 = new JLabel("Port: ");
        ipLabel1.setFont(new Font("Arial", Font.BOLD, 12));
        portLabel1.setFont(new Font("Arial", Font.BOLD, 12));

        infoTextPanel1.add(ipLabel1);
        infoTextPanel1.add(Box.createHorizontalStrut(20));
        infoTextPanel1.add(portLabel1);
        infoTextPanel1.add(Box.createHorizontalStrut(10));
        infoPanel1.add(infoTextPanel1);

        // Hide text initially
        infoTextPanel1.setVisible(false);


        showAddrBox1.addActionListener(e -> {
            infoTextPanel1.setVisible(showAddrBox1.isSelected());
            chat.revalidate();
            chat.repaint();
        });

        joinButton2.addActionListener(e -> { // this is actually the join button in the room join screen but if i dont put it here an error happens
            String ipText = ip.getText();
            String portText = port1.getText();
            String userText = username.getText();
            if (!ipText.isEmpty() && !portText.isEmpty() && !userText.isEmpty()) {
                try {
                    int portNumber = Integer.parseInt(portText);
                    new Thread(() -> {
                        try {
                            UDPClient.runClient(ipText, portNumber, userText);
                        } catch (LineUnavailableException ex) {
                            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            CardLayout cl = (CardLayout)(cards.getLayout());
                            cl.show(cards, "selectScreen");
                        }
                    }).start();
                    CardLayout cl = (CardLayout)(cards.getLayout());
                    cl.show(cards, "chat");
                    ipLabel1.setText("IP: " + ipText);
                    portLabel1.setText("Port: " + portNumber);
                } catch (NumberFormatException ex) {
                    System.err.println("Port is not an integer");
                    JOptionPane.showMessageDialog(roomJoin, "Please enter a valid Integer for the Port.", "Invalid Port", JOptionPane.ERROR_MESSAGE);
                }
            } else if (ipText.isEmpty()) {
                JOptionPane.showMessageDialog(roomJoin, "Please enter an IP", "Invalid IP", JOptionPane.ERROR_MESSAGE);
            } else if (portText.isEmpty()) {
                JOptionPane.showMessageDialog(roomJoin, "Please enter a Port", "Invalid Port", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(roomJoin, "Please enter a Username", "Invalid Username", JOptionPane.ERROR_MESSAGE);
            }
        });

        chat.add(topBar1);

        chat.add(infoPanel1, BorderLayout.WEST);

        JPanel mainPanel1 = new JPanel(new BorderLayout(10, 10));
        mainPanel1.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== CHAT PANEL =====
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.setPreferredSize(new Dimension(550, 300));

        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5)); // bottom spacing
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // some padding

        JTextField chatInput = new JTextField();
        JButton sendButton = new JButton("Send");

        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        mainPanel1.add(chatPanel, BorderLayout.CENTER);

        sendButton.addActionListener(e -> {
            String msg = chatInput.getText();
            if (!msg.isEmpty()) {
                UDPClient.sendMessage(msg);
                chatInput.setText("");
            }
        });
        chatInput.addActionListener(e -> {
            String msg = chatInput.getText();
            if (!msg.isEmpty()) {
                UDPClient.sendMessage(msg);
                chatInput.setText("");
            }
        });

        // ===== RIGHT PANEL (Users + VC Users) =====
        JPanel rightPanel2 = new JPanel();
        rightPanel2.setLayout(new BorderLayout(5, 5));
        rightPanel2.setPreferredSize(new Dimension(250, 300));

        // Top: User list
        JPanel userPanel1 = new JPanel(new BorderLayout());
        userPanel1.setBorder(BorderFactory.createTitledBorder("Users"));
        DefaultListModel<String> userListModel1 = new DefaultListModel<>();
        JList<String> userList1 = new JList<>(userListModel1);
        JScrollPane userScroll1 = new JScrollPane(userList1);
        userPanel1.add(userScroll1, BorderLayout.CENTER);

        // Bottom: VC Users
        JPanel vcPanel1 = new JPanel();
        vcPanel1.setLayout(new BoxLayout(vcPanel1, BoxLayout.Y_AXIS));
        vcPanel1.setBorder(BorderFactory.createTitledBorder("Voice Call"));

        DefaultListModel<String> vcUserListModel1 = new DefaultListModel<>();
        JList<String> vcList1 = new JList<>(vcUserListModel1);
        JScrollPane vcScroll1 = new JScrollPane(vcList1);
        vcScroll1.setPreferredSize(new Dimension(230, 120));
        vcPanel1.add(vcScroll1);
        vcPanel1.add(Box.createVerticalStrut(5));

        // VC Buttons
        JPanel vcButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        JButton joinButton = new JButton("Join");
        JButton leaveButton = new JButton("Leave");
        JButton muteButton = new JButton("Mute");
        JButton deafButton = new JButton("Deaf");
        vcButtonsPanel.add(joinButton);
        vcPanel1.add(vcButtonsPanel);

        // ===== BUTTON ACTIONS =====
        joinButton.addActionListener(e -> {
            UDPClient.sendMessage("/vc join");
            vcButtonsPanel.removeAll();
            vcButtonsPanel.add(leaveButton);
            vcButtonsPanel.add(muteButton);
            vcButtonsPanel.add(deafButton);
            vcButtonsPanel.revalidate();
            vcButtonsPanel.repaint();
        });

        leaveButton.addActionListener(e -> {
            UDPClient.sendMessage("/vc leave");
            muteButton.setText("Mute");
            deafButton.setText("Deaf");
            vcButtonsPanel.removeAll();
            vcButtonsPanel.add(joinButton);
            vcButtonsPanel.revalidate();
            vcButtonsPanel.repaint();
        });

        muteButton.addActionListener(e -> {
            UDPClient.sendMessage("/vc mute");
            muteButton.setText(muteButton.getText().equals("Mute") ? "Unmute" : "Mute");
        });

        deafButton.addActionListener(e -> {
            UDPClient.sendMessage("/vc deaf");
            deafButton.setText(deafButton.getText().equals("Deaf") ? "Undeaf" : "Deaf");
        });

        backButton1.addActionListener(e -> {
            muteButton.setText("Mute");
            deafButton.setText("Deaf");
            vcButtonsPanel.removeAll();
            vcButtonsPanel.add(joinButton);
            vcButtonsPanel.revalidate();
            vcButtonsPanel.repaint();
            CardLayout cl = (CardLayout)(cards.getLayout());
            cl.show(cards, "selectScreen");
            UDPClient.disconnect("Closed by User");

            clearForm(roomJoin);
            clearForm(serverConfig);
            authKeyRow.setVisible(false);
            clearForm(chat);
            clearForm(server);
        });

        // Combine top & bottom in right panel
        rightPanel2.add(userPanel1, BorderLayout.CENTER);
        rightPanel2.add(vcPanel1, BorderLayout.SOUTH);

        mainPanel1.add(rightPanel2, BorderLayout.EAST);

        // Add main panel to server panel
        chat.add(mainPanel1);

        cards.add(chat, "chat");

        // ===========================
        // ====== SERVER SCREEN ======
        // ===========================
        server.setLayout(new BoxLayout(server, BoxLayout.Y_AXIS)); // vertical layout

        JPanel topBar2 = new JPanel(new BorderLayout());
        topBar2.setPreferredSize(new Dimension(Integer.MAX_VALUE, 35));
        topBar2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // LEFT: Back button with padding
        JPanel leftPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton backButton2 = new JButton("Stop");
        backButton2.addActionListener(e -> {
            CardLayout cl = (CardLayout)(cards.getLayout());
            cl.show(cards, "selectScreen");
            UDPServer.stop("Server stopped by user");

            clearForm(roomJoin);
            clearForm(serverConfig);
            authKeyRow.setVisible(false);
            clearForm(chat);
            clearForm(server);
        });
        leftPanel2.add(backButton2);
        topBar2.add(leftPanel2, BorderLayout.WEST);

        // CENTER: title
        JLabel titleLabel3 = new JLabel("Server", SwingConstants.CENTER);
        titleLabel3.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel3.setPreferredSize(new Dimension(titleLabel3.getPreferredSize().width, 25));
        topBar2.add(titleLabel3, BorderLayout.CENTER);

        // ===== RIGHT SPACER =====
        JPanel rightSpacer2 = new JPanel();
        rightSpacer2.setPreferredSize(leftPanel2.getPreferredSize());
        topBar2.add(rightSpacer1, BorderLayout.EAST);


        JPanel infoPanel2 = new JPanel();
        infoPanel2.setLayout(new BoxLayout(infoPanel2, BoxLayout.X_AXIS));

        // Checkbox (fixed position)
        JCheckBox showAddrBox2 = new JCheckBox("Show connection details");
        showAddrBox2.setFont(new Font("Arial", Font.BOLD, 12));
        infoPanel2.add(showAddrBox2);
        infoPanel2.add(Box.createVerticalStrut(10));

        // Stacked IP + Port
        JPanel infoTextPanel2 = new JPanel();
        infoTextPanel2.setLayout(new BoxLayout(infoTextPanel2, BoxLayout.X_AXIS));

        JLabel ipLabel2 = new JLabel("IP: ");
        JLabel portLabel2 = new JLabel("Port: ");
        ipLabel2.setFont(new Font("Arial", Font.BOLD, 12));
        portLabel2.setFont(new Font("Arial", Font.BOLD, 12));

        infoTextPanel2.add(ipLabel2);
        infoTextPanel2.add(Box.createHorizontalStrut(20));
        infoTextPanel2.add(portLabel2);
        infoTextPanel2.add(Box.createHorizontalStrut(10));
        infoPanel2.add(infoTextPanel2);

        // Hide text initially
        infoTextPanel2.setVisible(false);


        showAddrBox2.addActionListener(e -> {
            infoTextPanel2.setVisible(showAddrBox2.isSelected());
            server.revalidate();
            server.repaint();
        });

        server.add(topBar2);

        server.add(infoPanel2, BorderLayout.WEST);

        JPanel mainPanel2 = new JPanel(new BorderLayout(10, 10));
        mainPanel2.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ===== LOG PANEL =====
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        logPanel.setPreferredSize(new Dimension(550, 300));

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        mainPanel2.add(logPanel, BorderLayout.CENTER);

        // ===== RIGHT PANEL (Users + VC Users) =====
        JPanel rightPanel5 = new JPanel();
        rightPanel5.setLayout(new BorderLayout(5, 5));
        rightPanel5.setPreferredSize(new Dimension(250, 300));

        // Top: User list
        JPanel userPanel2 = new JPanel(new BorderLayout());
        userPanel2.setBorder(BorderFactory.createTitledBorder("Users"));
        DefaultListModel<String> userListModel2 = new DefaultListModel<>();
        JList<String> userList2 = new JList<>(userListModel2);
        JScrollPane userScroll2 = new JScrollPane(userList2);
        userPanel2.add(userScroll2, BorderLayout.CENTER);

        // Bottom: VC Users
        JPanel vcPanel2 = new JPanel(new BorderLayout());
        vcPanel2.setBorder(BorderFactory.createTitledBorder("Voice Call"));
        DefaultListModel<String> vcUserListModel2 = new DefaultListModel<>();
        JList<String> vcList2 = new JList<>(vcUserListModel2);
        JScrollPane vcScroll2 = new JScrollPane(vcList2);
        vcPanel2.add(vcScroll2, BorderLayout.CENTER);

        // Combine top & bottom in right panel
        rightPanel5.add(userPanel2, BorderLayout.CENTER);
        rightPanel5.add(vcPanel2, BorderLayout.SOUTH);

        mainPanel2.add(rightPanel5, BorderLayout.EAST);

        // Add main panel to server panel
        server.add(mainPanel2);

        cards.add(server, "server");

        frame.add(cards);

        hostButton2.addActionListener(e -> {
            String portText = port2.getText();
            if (!portText.isEmpty()) {
                try {
                    portNumber = Integer.parseInt(portText);
                    if (requiresAuth.isSelected()) {
                        String authKeyText = authKey.getText();
                        new Thread(() -> {
                            try {
                                UDPServer.runServer(portNumber, authKeyText);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                                CardLayout cl = (CardLayout)(cards.getLayout());
                                cl.show(cards, "selectScreen");
                            }
                        }).start();
                    } else {
                        new Thread(() -> {
                            try {
                                UDPServer.runServer(portNumber, "");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                                CardLayout cl = (CardLayout)(cards.getLayout());
                                cl.show(cards, "selectScreen");
                            }
                        }).start();
                    }
                    CardLayout cl = (CardLayout)(cards.getLayout());
                    cl.show(cards, "server");
                    userListModel2.clear();
                    vcUserListModel2.clear();
                    ipLabel2.setText("IP: " + InetAddress.getLocalHost().getHostAddress());
                    portLabel2.setText("Port: " + portNumber);
                } catch (NumberFormatException ex) {
                    System.out.println("Port is not an integer");
                    JOptionPane.showMessageDialog(serverConfig, "Please enter a valid Integer for the Port", "Invalid Port", JOptionPane.ERROR_MESSAGE);
                } catch (UnknownHostException ex) {
                    System.out.println("Error when getting IP");
                    JOptionPane.showMessageDialog(serverConfig, "Error when getting IP: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(serverConfig, "Please enter a Port", "Invalid Port", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ===========================
        // ======== UIHandler ========
        // ===========================
        UDPClient.setUiHandler(new ClientUIHandler() {
            @Override
            public void onError(String msg) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Error: " + msg, "Error", JOptionPane.ERROR_MESSAGE));
            }
            @Override
            public void onDisconnect(String reason) {
                muteButton.setText("Mute");
                deafButton.setText("Deaf");
                vcButtonsPanel.removeAll();
                vcButtonsPanel.add(joinButton);
                vcButtonsPanel.revalidate();
                vcButtonsPanel.repaint();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Disconnected: " + reason, "Disconnected", JOptionPane.WARNING_MESSAGE));
                CardLayout cl = (CardLayout)(cards.getLayout());
                cl.show(cards, "roomJoin");
            }
            @Override
            public void onAuthRequest(Consumer<String> callback) {
                SwingUtilities.invokeLater(() -> {
                    String key = JOptionPane.showInputDialog(frame, "Enter Auth Key:");
                    if (key != null) callback.accept(key);
                });
            }
            @Override
            public void onAuthCorrect() {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Auth OK");
                });
            }
            @Override
            public void onMessage(String msg) {
                chatArea.append(msg + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
            @Override
            public void onVCListUpdate(String VCUsers) {
                Gson gson = new Gson();

                // Define the type for a List of Map<String, Object>
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();

                List<Map<String, Object>> vcList = gson.fromJson(VCUsers, listType);

                // Clear old list
                vcUserListModel1.clear();

                // Populate VC user list
                for (Map<String, Object> userObj : vcList) {
                    String username = (String) userObj.get("username");
                    boolean mute = Boolean.TRUE.equals(userObj.get("mute"));
                    boolean deaf = Boolean.TRUE.equals(userObj.get("deaf"));

                    String display = username;
                    if (mute) display += " [M]";
                    if (deaf) display += " [D]";

                    vcUserListModel1.addElement(display);
                }
            }
            @Override
            public void onUserListUpdate(String users) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<String>>(){}.getType();

                List<String> userList = gson.fromJson(users, type);

                List<String> sortedList = new ArrayList<>(userList);
                Collections.sort(sortedList);
                userListModel1.clear();
                for (String user : sortedList) {
                    userListModel1.addElement(user);
                }
            }
        });

        UDPServer.setUiHandler(new ServerUIHandler() {
            @Override
            public void onError(String msg) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Error: " + msg, "Error", JOptionPane.ERROR_MESSAGE));
            }
            @Override
            public void onStop(String reason) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Disconnected: " + reason, "Disconnected", JOptionPane.WARNING_MESSAGE));
                CardLayout cl = (CardLayout)(cards.getLayout());
                cl.show(cards, "selectScreen");
            }
            @Override
            public void onUserListUpdate(Map<String, String> userMap) {
                userListModel2.clear();
                List<String> sortedUsers = new ArrayList<>(userMap.values());
                Collections.sort(sortedUsers);
                for (String username : sortedUsers) {
                    userListModel2.addElement(username);
                }
            }
            public void onVCListUpdate(Map<String, VCInfo> vcStatus, Map<String, String> userMap) {
                vcUserListModel2.clear();
                List<Map.Entry<String, VCInfo>> sortedEntries = new ArrayList<>(vcStatus.entrySet());
                sortedEntries.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));
                for (Map.Entry<String, VCInfo> entry : sortedEntries) {
                    String clientID = entry.getKey();
                    VCInfo status = entry.getValue();
                    if (status.inVC) {
                        String display = userMap.get(clientID);
                        if (status.mute) display += " [M]";
                        if (status.deaf) display += " [D]";
                        vcUserListModel2.addElement(display);
                    }
                }
            }
            public void onLog(String msg) {
                logArea.append(msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });

        // show frame
        frame.setLocationRelativeTo(null); // center screen
        frame.setVisible(true);
    }
    private static JPanel createTopBar(String title, ActionListener backAction) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 35));
        topBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // LEFT: Back button with padding
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton backButton = new JButton("Back");
        if (backAction != null) backButton.addActionListener(backAction);
        leftPanel.add(backButton);
        topBar.add(leftPanel, BorderLayout.WEST);

        // CENTER: title
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel.setPreferredSize(new Dimension(titleLabel.getPreferredSize().width, 25));
        topBar.add(titleLabel, BorderLayout.CENTER);

        // RIGHT: spacing to keep title centered
        JPanel rightSpacer = new JPanel();
        rightSpacer.setPreferredSize(leftPanel.getPreferredSize());
        topBar.add(rightSpacer, BorderLayout.EAST);

        return topBar;
    }
    private static JPanel createInputRow(String labelText, JTextField field) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));

        JLabel label = new JLabel(labelText);

        // fix label width so all rows align perfectly
        Dimension labelSize = new Dimension(80, label.getPreferredSize().height);
        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);
        label.setMaximumSize(labelSize);

        row.add(label);
        row.add(Box.createHorizontalStrut(10));
        row.add(field);

        // keep the row at a nice narrow width
        row.setMaximumSize(new Dimension(300, row.getPreferredSize().height));

        return row;
    }
    private static void clearForm(Component c) {
        if (c instanceof JTextField txt) {
            txt.setText("");
        } else if (c instanceof JTextArea area) {
            area.setText("");
        } else if (c instanceof JPasswordField pass) {
            pass.setText("");
        } else if (c instanceof JCheckBox check) {
            check.setSelected(false);
        } else if (c instanceof JRadioButton radio) {
            radio.setSelected(false);
        }

        // Recursively clear children
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                clearForm(child);
            }
        }
    }

    private static void checkForUpdates() {
        UpdateManager.checkForUpdate((latestVersion, localVersion, releaseJson) -> {
            SwingUtilities.invokeLater(() -> {
                Object[] options = {"Download Now", "Later"};
                int choice = JOptionPane.showOptionDialog(
                        null,
                        "A new version is available: " + latestVersion,
                        "Update Available",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                if (choice == 0) { // Download Now
                    showDownloadProgress(latestVersion, localVersion, releaseJson);
                }
            });
        });
    }

    private static void showDownloadProgress(String latestVersion, String oldVersion, JsonObject releaseJson) {
        JDialog progressDialog = new JDialog((Frame) null, "Downloading Update", true);
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        progressDialog.setLayout(new BorderLayout());
        progressDialog.add(new JLabel("Downloading version " + latestVersion + "..."), BorderLayout.NORTH);
        progressDialog.add(progressBar, BorderLayout.CENTER);
        progressDialog.setSize(400, 120);
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setResizable(false);

        Executors.newSingleThreadExecutor().submit(() -> {
            boolean success = UpdateManager.downloadUpdate(latestVersion, oldVersion, releaseJson, percent -> {
                SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
            });

            SwingUtilities.invokeLater(() -> {
                progressDialog.dispose();
                if (success) UpdateManager.restartJar(latestVersion);
                else JOptionPane.showMessageDialog(null,
                        "Failed to download update.", "Update Failed", JOptionPane.ERROR_MESSAGE);
            });
        });

        progressDialog.setVisible(true);
    }
}
