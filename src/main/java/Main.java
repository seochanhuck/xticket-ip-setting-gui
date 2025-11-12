import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            IpSettingGUI gui = new IpSettingGUI();
            gui.setVisible(true);
        });
    }
}
