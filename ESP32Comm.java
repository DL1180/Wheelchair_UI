import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ESP32Comm extends Thread {

    private static final String PI_HOST = "100.105.213.10";
    private static final int PI_PORT = 5050;

    private final JLabel connectionLabel;
    private final JLabel motionLabel;
    private final JLabel obstacleLabel;
    private final JLabel communicationLabel;
    private final JLabel distanceLabel;

    private static final Color GREEN =
            new Color(22, 163, 74);

    private static final Color RED =
            new Color(220, 38, 38);

    private static final Color MUTED =
            new Color(100, 116, 139);

    public ESP32Comm(
            JLabel connectionLabel,
            JLabel motionLabel,
            JLabel obstacleLabel,
            JLabel communicationLabel,
            JLabel distanceLabel) {

        this.connectionLabel = connectionLabel;
        this.motionLabel = motionLabel;
        this.obstacleLabel = obstacleLabel;
        this.communicationLabel = communicationLabel;
        this.distanceLabel = distanceLabel;
    }

    @Override
    public void run() {

        while (!isInterrupted()) {

            try {

                Socket socket = new Socket();

                socket.connect(
                        new InetSocketAddress(
                                PI_HOST,
                                PI_PORT
                        ),
                        3000
                );

                setConnected(true);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()
                                )
                        );

                String line;

                while ((line = reader.readLine()) != null) {
                    processStatus(line);
                }

                socket.close();

            } catch (Exception e) {

                setConnected(false);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interrupted) {
                    interrupt();
                }
            }
        }
    }

    private void processStatus(String line) {

        if (!line.startsWith("STATUS,")) {
            return;
        }

        String[] parts = line.split(",");

        if (parts.length != 5) {
            return;
        }

        try {

            boolean motionAllowed =
                    parts[1].equals("1");

            boolean obstacleDetected =
                    parts[2].equals("1");

            boolean timeout =
                    parts[3].equals("1");

            int distanceMm =
                    Integer.parseInt(parts[4]);

            updateStatus(
                    motionAllowed,
                    obstacleDetected,
                    timeout,
                    distanceMm
            );

        } catch (Exception ignored) {
        }
    }

    private void setConnected(boolean connected) {

        SwingUtilities.invokeLater(() -> {

            if (connected) {

                connectionLabel.setText(
                        "ESP32: Connected"
                );

            } else {

                connectionLabel.setText(
                        "ESP32: Disconnected"
                );

                motionLabel.setText("Stopped");
                motionLabel.setForeground(RED);

                communicationLabel.setText(
                        "Disconnected"
                );

                communicationLabel.setForeground(
                        RED
                );
            }
        });
    }

    private void updateStatus(
            boolean motionAllowed,
            boolean obstacleDetected,
            boolean timeout,
            int distanceMm) {

        SwingUtilities.invokeLater(() -> {

            if (motionAllowed) {

                motionLabel.setText("Allowed");
                motionLabel.setForeground(GREEN);

            } else {

                motionLabel.setText("Stopped");
                motionLabel.setForeground(RED);
            }

            if (obstacleDetected) {

                obstacleLabel.setText(
                        "Obstacle Detected"
                );

                obstacleLabel.setForeground(RED);

            } else {

                obstacleLabel.setText("Clear");
                obstacleLabel.setForeground(GREEN);
            }

            if (timeout) {

                communicationLabel.setText(
                        "Timeout"
                );

                communicationLabel.setForeground(
                        RED
                );

            } else {

                communicationLabel.setText("Good");
                communicationLabel.setForeground(GREEN);
            }

            distanceLabel.setText(
                    distanceMm + " mm"
            );

            distanceLabel.setForeground(MUTED);
        });
    }
}