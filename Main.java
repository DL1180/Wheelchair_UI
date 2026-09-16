import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class Main {

    public static void main(String[] args) {

        JFrame frame = new JFrame("Wheelchair Interface");
        frame.setSize(500, 850);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(null);
        mainPanel.setBounds(15, 15, 455, 780);
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new LineBorder(Color.BLACK, 2));

        JButton manualButton = new JButton("Manual");
        manualButton.setBounds(30, 25, 100, 38);
        manualButton.setBackground(new Color(0, 90, 200));
        manualButton.setForeground(Color.WHITE);
        manualButton.setBorder(
                new LineBorder(new Color(0, 90, 200), 2));
        manualButton.setFocusPainted(false);

        JButton autoButton = new JButton("Auto");
        autoButton.setBounds(145, 25, 100, 38);
        autoButton.setBackground(new Color(0, 90, 200));;
        autoButton.setForeground(Color.WHITE);
        autoButton.setBorder(
                new LineBorder(new Color(0, 90, 200), 2));
        autoButton.setFocusPainted(false);

        JLabel batteryLabel = new JLabel("Battery: 80%");
        batteryLabel.setFont(
                new Font("Arial", Font.BOLD, 18));
        batteryLabel.setForeground(
                new Color(0, 150, 0));
        batteryLabel.setBounds(300, 20, 140, 30);

        JLabel rangeLabel = new JLabel("Est. Range: 25 mi");
        rangeLabel.setFont(
                new Font("Arial", Font.PLAIN, 14));
        rangeLabel.setBounds(300, 48, 120, 20);

        JLabel statusLabel = new JLabel(
                "Status: Ready",
                SwingConstants.CENTER);
        statusLabel.setFont(
                new Font("Arial", Font.BOLD, 18));
        statusLabel.setForeground(
                new Color(0, 150, 0));
        statusLabel.setBounds(80, 105, 280, 30);

        //esp32 addition 
        JLabel esp32Label = new JLabel(
                "ESP32: Disconnected",
                SwingConstants.CENTER);

        esp32Label.setFont(
                new Font("Arial", Font.BOLD, 15));
        esp32Label.setForeground(Color.RED);
        esp32Label.setBounds(80, 125, 280, 25);
        //end

        JButton navigationButton = new JButton(
                "<html><center>"
                        + "<span style='font-size:30px;'>Navigation</span>"
                        + "<br><br>"
                        + "<span style='font-size:14px;'>Current Location: Kitchen</span>"
                        + "</center></html>");
        navigationButton.setBounds(40, 170, 370, 220);
        navigationButton.setBackground(Color.WHITE);
        navigationButton.setBorder(
                new LineBorder(Color.BLACK, 2));

        JButton batteryInfoButton = new JButton(
                "<html><center>Battery<br>Information</center></html>");
        batteryInfoButton.setFont(
                new Font("Arial", Font.BOLD, 22));
        batteryInfoButton.setBounds(40, 440, 170, 140);
        batteryInfoButton.setBackground(Color.WHITE);
        batteryInfoButton.setBorder(
                new LineBorder(Color.BLACK, 2));

        JButton settingsButton = new JButton("Settings");
        settingsButton.setFont(
                new Font("Arial", Font.BOLD, 24));
        settingsButton.setBounds(240, 440, 170, 140);
        settingsButton.setBackground(Color.WHITE);
        settingsButton.setBorder(
                new LineBorder(Color.BLACK, 2));

        JButton sosButton = new JButton("SOS");
        sosButton.setFont(
                new Font("Arial", Font.BOLD, 34));
        sosButton.setBounds(40, 630, 170, 90);
        sosButton.setForeground(Color.WHITE);
        sosButton.setBackground(Color.RED);

        JButton stopButton = new JButton("STOP");
        stopButton.setFont(
                new Font("Arial", Font.BOLD, 34));
        stopButton.setBounds(240, 630, 170, 90);
        stopButton.setForeground(Color.RED);
        stopButton.setBackground(Color.WHITE);
        stopButton.setBorder(
                new LineBorder(Color.RED, 2));

        manualButton.addActionListener(e -> {
            statusLabel.setText("Status: Manual Mode");
            statusLabel.setForeground(
                    new Color(0, 90, 200));
        });

        autoButton.addActionListener(e -> {
            statusLabel.setText("Status: Autonomous Mode");
            statusLabel.setForeground(
                    new Color(0, 150, 0));
        });

        navigationButton.addActionListener(e -> {
            statusLabel.setText("Status: Navigation Selected");
            statusLabel.setForeground(
                    new Color(0, 90, 200));
        });

        batteryInfoButton.addActionListener(e -> {
            statusLabel.setText("Status: Battery Info Selected");
            statusLabel.setForeground(
                    new Color(0, 150, 0));
        });

        settingsButton.addActionListener(e -> {
            statusLabel.setText("Status: Settings Selected");
            statusLabel.setForeground(
                    new Color(0, 90, 200));
        });

        sosButton.addActionListener(e -> {
            statusLabel.setText("Status: SOS Activated");
            statusLabel.setForeground(Color.RED);
        });

        stopButton.addActionListener(e -> {
            statusLabel.setText("Status: Emergency Stop");
            statusLabel.setForeground(Color.RED);
        });

        mainPanel.add(manualButton);
        mainPanel.add(autoButton);
        mainPanel.add(batteryLabel);
        mainPanel.add(rangeLabel);
        mainPanel.add(statusLabel);
        mainPanel.add(navigationButton);
        mainPanel.add(batteryInfoButton);
        mainPanel.add(settingsButton);
        mainPanel.add(sosButton);
        mainPanel.add(stopButton);
        mainPanel.add(esp32Label); //new
        frame.add(mainPanel);
        frame.setVisible(true);
        
        //esp comm
        ESP32Comm esp32 =
        new ESP32Comm(esp32Label);
        esp32.start();
    }
}