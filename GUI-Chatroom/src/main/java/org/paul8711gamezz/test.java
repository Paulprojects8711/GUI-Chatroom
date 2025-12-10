package org.paul8711gamezz;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

public class test {
    public static void main(String[] args) {
        // Set FlatLaf Look & Feel
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Main frame
        JFrame frame = new JFrame("Enhanced FlatLaf GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new BorderLayout(10, 10));

        // ---------- Top Label ----------
        JLabel label = new JLabel("Interactive GUI Example", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        frame.add(label, BorderLayout.NORTH);

        // ---------- Center Panel ----------
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        frame.add(centerPanel, BorderLayout.CENTER);

        // Input boxes
        JTextField input1 = new JTextField(20);
        JTextField input2 = new JTextField(20);
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("Input 1:"));
        inputPanel.add(input1);
        inputPanel.add(new JLabel("Input 2:"));
        inputPanel.add(input2);
        centerPanel.add(inputPanel);

        // Button that only works when both input boxes are filled
        JButton multiInputButton = new JButton("Submit Both Inputs");
        multiInputButton.addActionListener(e -> {
            String text1 = input1.getText().trim();
            String text2 = input2.getText().trim();
            if (!text1.isEmpty() && !text2.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Both inputs submitted:\n" + text1 + " , " + text2);
            } else {
                JOptionPane.showMessageDialog(frame, "Please fill both input boxes!");
            }
        });
        centerPanel.add(multiInputButton);

        centerPanel.add(Box.createVerticalStrut(20)); // spacing

        // Checkboxes
        JCheckBox cbOption1 = new JCheckBox("Option 1");
        JCheckBox cbOption2 = new JCheckBox("Option 2");
        JPanel cbPanel = new JPanel(new FlowLayout());
        cbPanel.add(cbOption1);
        cbPanel.add(cbOption2);
        centerPanel.add(cbPanel);

        // Button to check selected checkboxes
        JButton checkButton = new JButton("Check Selected Options");
        JTextArea cbOutput = new JTextArea(3, 40);
        cbOutput.setEditable(false);
        JScrollPane cbScroll = new JScrollPane(cbOutput);

        checkButton.addActionListener(e -> {
            cbOutput.setText(""); // clear previous
            if (cbOption1.isSelected()) cbOutput.append("Option 1 selected\n");
            if (cbOption2.isSelected()) cbOutput.append("Option 2 selected\n");
            if (!cbOption1.isSelected() && !cbOption2.isSelected())
                cbOutput.setText("No options selected");
        });

        centerPanel.add(checkButton);
        centerPanel.add(cbScroll);

        // ---------- Dynamic Input Box Appearing ----------
        JCheckBox cbDynamic = new JCheckBox("Enable extra input");
        JTextField dynamicInput = new JTextField(15);
        dynamicInput.setVisible(false); // hidden by default

        cbDynamic.addActionListener(e -> dynamicInput.setVisible(cbDynamic.isSelected()));

        JPanel dynamicPanel = new JPanel(new FlowLayout());
        dynamicPanel.add(cbDynamic);
        dynamicPanel.add(dynamicInput);
        centerPanel.add(dynamicPanel);

        // Button to submit dynamic input
        JButton dynamicSubmit = new JButton("Submit Dynamic Input");
        dynamicSubmit.addActionListener(e -> {
            if (cbDynamic.isSelected() && !dynamicInput.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Dynamic input: " + dynamicInput.getText());
            } else {
                JOptionPane.showMessageDialog(frame, "Please enable checkbox and enter something!");
            }
        });
        centerPanel.add(dynamicSubmit);

        // ---------- Make Frame Visible ----------
        frame.setLocationRelativeTo(null); // center screen
        frame.setVisible(true);
    }
}
