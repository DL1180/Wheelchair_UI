import javax.swing.*;
import java.awt.*;

public class ESP32Comm extends Thread {

    private final JLabel connectionLabel;
    public ESP32Comm(JLabel connectionLabel) {
        this.connectionLabel = connectionLabel;}

    @Override
    public void run() {
        System.out.println("ESP32 communication thread started");
        setDisconnected();}

    private void setDisconnected() {
        SwingUtilities.invokeLater(() -> {
            connectionLabel.setText("ESP32: Disconnected");
            connectionLabel.setForeground(Color.RED);
        });}
}