import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class IpSettingGUI extends JFrame {
    private JTextField ipField;
    private JButton saveButton;
    private String extensionPath;

    public IpSettingGUI() {
        setTitle("X-TICKET IP 설정");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 바탕화면 경로 확인
        String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
        String targetPath = desktopPath + File.separator + "달서프로그램" + File.separator + "X-TICKET_크롬_확장프로그램";
        
        File extensionDir = new File(targetPath);
        
        if (!extensionDir.exists() || !extensionDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, 
                "바탕화면에 '달서프로그램/X-TICKET_크롬_확장프로그램' 폴더를 찾을 수 없습니다.\n경로: " + targetPath,
                "오류", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
        }

        extensionPath = targetPath;

        // UI 구성
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // 안내 문구
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel noticeLabel = new JLabel("<html><center>저장 후 확장프로그램을 새로고침 해주세요.</center></html>");
        noticeLabel.setFont(new Font(noticeLabel.getFont().getName(), Font.PLAIN, 12));
        noticeLabel.setForeground(Color.BLACK);
        mainPanel.add(noticeLabel, gbc);

        // IP 입력 레이블
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("IP 주소 입력:"), gbc);

        // IP 입력 필드
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        ipField = new JTextField(15);
        ipField.setText("100.7.163.");
        mainPanel.add(ipField, gbc);

        // 저장 버튼
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        saveButton = new JButton("저장");
        saveButton.addActionListener(new SaveButtonListener());
        mainPanel.add(saveButton, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // 안내 메시지
        JLabel infoLabel = new JLabel("확장프로그램 경로: " + extensionPath);
        infoLabel.setFont(new Font(infoLabel.getFont().getName(), Font.PLAIN, 10));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private class SaveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String ipAddress = ipField.getText().trim();
            
            // IP 주소 유효성 검사 (간단한 형식 검사)
            if (!isValidIpAddress(ipAddress)) {
                JOptionPane.showMessageDialog(IpSettingGUI.this,
                    "올바른 IP 주소를 입력해주세요.",
                    "입력 오류",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // config.content.js 파일 수정
                updateConfigFile("config.content.js", 
                    "const DALSEO_SERVER_API_BASE_URL = 'http://" + ipAddress + ":8081';");

                // config.module.js 파일 수정
                updateConfigFile("config.module.js",
                    "export const DALSEO_SERVER_API_BASE_URL = 'http://" + ipAddress + ":8081';");

                // manifest.json 파일 수정
                updateManifestJson(ipAddress);

                JOptionPane.showMessageDialog(IpSettingGUI.this,
                    "설정이 성공적으로 저장되었습니다!",
                    "완료",
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(IpSettingGUI.this,
                    "파일 수정 중 오류가 발생했습니다: " + ex.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void updateConfigFile(String fileName, String newContent) throws IOException {
        Path filePath = Paths.get(extensionPath, fileName);
        
        if (!Files.exists(filePath)) {
            throw new IOException("파일을 찾을 수 없습니다: " + filePath);
        }

        // 파일 전체를 newContent로 덮어쓰기
        Files.write(filePath, newContent.getBytes("UTF-8"));
    }

    private void updateManifestJson(String ipAddress) throws IOException {
        Path filePath = Paths.get(extensionPath, "manifest.json");
        
        if (!Files.exists(filePath)) {
            throw new IOException("파일을 찾을 수 없습니다: " + filePath);
        }

        // 파일 전체 읽기
        String content = new String(Files.readAllBytes(filePath), "UTF-8");
        
        // JSON 파싱
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
        
        // host_permissions 배열을 새로 생성하여 덮어쓰기
        JsonArray hostPermissions = new JsonArray();
        hostPermissions.add("https://admin3.xticket.kr:9090/main/mainWrap.do");
        hostPermissions.add("http://" + ipAddress + ":8081/*");
        jsonObject.add("host_permissions", hostPermissions);
        
        // JSON을 다시 문자열로 변환
        String updatedContent = gson.toJson(jsonObject);
        
        // 전체 내용을 파일에 다시 쓰기
        Files.write(filePath, updatedContent.getBytes("UTF-8"));
    }

    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // 간단한 IP 주소 형식 검사 (IPv4)
        String ipPattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                          "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                          "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                          "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        
        return Pattern.matches(ipPattern, ip);
    }
}

