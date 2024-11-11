package com.passwordmanager;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Properties;

public class PasswordManagerGUI {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static SecretKey aesKey;
    private static IvParameterSpec ivParameterSpec;
    private static final String[] commonWords = {
            "apple!@#$%^^&(", "oran#$%^&ge", "blue@#%^&*", "!@##$$green",
            "c^%$$at", "do%$@@$$g", "st)(*&^^%$#ar", "moo@##%#n",
            "ra!@@@))+^&<>in", "su~~^&@&)_+n"
    };
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pwm";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Veera@pandi"; // Replace with your actual password
    private static final String KEY_FILE = "aes_key.bin";
    private static final String IV_FILE = "aes_iv.bin";
    private static final String EMAIL_HOST = "smtp.gmail.com";
    private static final String EMAIL_PORT = "587"; // Set to 587 for TLS
    private static final String SENDER_EMAIL = "passkey2manager@gmail.com"; // Replace with your email
    private static final String SENDER_PASSWORD = "your_password_here"; // Replace with your password

    static {
        try {
            loadOrGenerateKeyAndIV();
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Error initializing encryption parameters or database driver", e);
        }
    }

    public static void main(String[] args) {
        // Set the look and feel to a dark theme
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("control", new Color(100, 100, 100));
            UIManager.put("info", new Color(100, 100, 100));
            UIManager.put("menu", new Color(100, 100, 100));
            UIManager.put("text", Color.WHITE);
            UIManager.put("nimbusBase", new Color(50, 50, 50));
            UIManager.put("nimbusBlueGrey", new Color(50, 50, 50));
            UIManager.put("nimbusLightBackground", new Color(50, 50, 50));
            UIManager.put("nimbusFocus", new Color(100, 100, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }

        showLoginRegisterScreen();
    }

    private static void showLoginRegisterScreen() {
        JFrame frame = new JFrame("Password Manager - Login/Register");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setUndecorated(true);
        frame.getContentPane().setBackground(new Color(238, 238, 238));
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        JLabel titleLabel = new JLabel("Password Manager");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(0, 123, 255));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        JMenuItem item1 = new JMenuItem("Option 1");
        JMenuItem item2 = new JMenuItem("Option 2");
        menu.add(item1);
        menu.add(item2);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        gbc.gridy = 0;
        gbc.gridwidth = 2;
        frame.add(titleLabel, gbc);

        // Load lock and key image
        ImageIcon lockIcon = new ImageIcon("path_to_lock_and_key_image.png"); // Replace with actual image path
        JLabel imageLabel = new JLabel(lockIcon);
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        frame.add(imageLabel, gbc);

        JButton loginButton = createStyledButton("Login");
        JButton registerButton = createStyledButton("Register");

        loginButton.addActionListener(e -> {
            frame.dispose();
            loginUser(false);
        });

        registerButton.addActionListener(e -> {
            frame.dispose();
            registerUser();
        });

        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        JButton forgotPasswordButton = createStyledButton("Forgot Password");
        forgotPasswordButton.addActionListener(e -> {
            frame.dispose();
            loginUser(true);
        });
        loginPanel.add(loginButton);
        loginPanel.add(forgotPasswordButton);

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        frame.add(loginPanel, gbc);

        gbc.gridy = 3;
        frame.add(registerButton, gbc);

        frame.setVisible(true);
    }

    private static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        button.setBackground(new Color(0, 123, 255));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 105, 217));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 123, 255));
            }
        });

        return button;
    }

    private static void loginUser(boolean isForgotPassword) {
        if (isForgotPassword) {
            forgotPassword();
        } else {
            String email = JOptionPane.showInputDialog("Enter your email:");
            if (email == null || email.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Email is required!");
                return;
            }

            try {
                String storedPassword = getUserPasswordFromDB(email);
                if (storedPassword == null) {
                    JOptionPane.showMessageDialog(null, "No account found for this email.");
                    return;
                }

                String enteredPassword = JOptionPane.showInputDialog("Enter your password:");
                if (enteredPassword == null || enteredPassword.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Password is required!");
                    return;
                }

                if (storedPassword.equals(encrypt(enteredPassword))) {
                    JOptionPane.showMessageDialog(null, "Login successful!");
                    showMainMenu(email);
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid email or password.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error logging in: " + e.getMessage());
            }
        }
    }

    private static void forgotPassword() {
        String email = JOptionPane.showInputDialog("Enter your email to reset password:");
        if (email == null || email.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Email is required!");
            return;
        }

        try {
            if (!isEmailExists(email)) {
                JOptionPane.showMessageDialog(null, "No account found for this email.");
                return;
            }

            String otp = generateOTP();
            boolean isSent = sendEmailOTP(email, otp);

            if (isSent) {
                String enteredOTP = JOptionPane.showInputDialog("Enter the OTP sent to your email:");
                if (otp.equals(enteredOTP)) {
                    String newPassword = JOptionPane.showInputDialog("Enter your new password:");
                    if (newPassword != null && !newPassword.trim().isEmpty()) {
                        String encryptedPassword = encrypt(newPassword);
                        updatePasswordInDB(email, encryptedPassword);
                        JOptionPane.showMessageDialog(null, "Password reset successfully!");
                        showLoginRegisterScreen();
                    } else {
                        JOptionPane.showMessageDialog(null, "New password is required!");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid OTP.");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Failed to send OTP. Try again later.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error during password reset: " + e.getMessage());
        }
    }

    private static boolean sendEmailOTP(String recipientEmail, String otp) {
        Properties props = new Properties();
        props.put("mail.smtp.host", EMAIL_HOST);
        props.put("mail.smtp.port", EMAIL_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Password Manager OTP");
            message.setText("Your OTP for resetting the password is: " + otp);

            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String generateOTP() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(otp);
    }

    private static boolean isEmailExists(String email) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }

    private static void registerUser() {
        String email = JOptionPane.showInputDialog("Enter your email:");
        if (email == null || email.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Email is required!");
            return;
        }

        String password = JOptionPane.showInputDialog("Enter your password:");
        if (password == null || password.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Password is required!");
            return;
        }

        try {
            String encryptedPassword = encrypt(password);
            storeUserInDB(email, encryptedPassword);
            JOptionPane.showMessageDialog(null, "Registration successful!");
            showLoginRegisterScreen();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error registering user: " + e.getMessage());
        }
    }

    private static void storeUserInDB(String email, String encryptedPassword) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO users (email, password) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.setString(2, encryptedPassword);
                stmt.executeUpdate();
            }
        }
    }

    private static String getUserPasswordFromDB(String email) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT password FROM users WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
        }
        return null;
    }

    private static void showMainMenu(String email) {
        JFrame frame = new JFrame("Password Manager - Main Menu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());
        frame.setBackground(new Color(30, 30, 30));

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> {
            frame.dispose();
            showLoginRegisterScreen();
        });
        menu.add(logoutItem);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        Font font = new Font("Arial", Font.PLAIN, 16);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 1));
        buttonPanel.setBackground(new Color(30, 30, 30));

        JButton storeButton = new JButton("Store Password");
        JButton retrieveButton = new JButton("Retrieve Password");
        JButton changeButton = new JButton("Change Password");
        JButton generateButton = new JButton("Generate and Store Password");
        JButton exitButton = new JButton("Exit");

        storeButton.setFont(font);
        retrieveButton.setFont(font);
        changeButton.setFont(font);
        generateButton.setFont(font);
        exitButton.setFont(font);

        storeButton.addActionListener(e -> storePassword(email));
        retrieveButton.addActionListener(e -> retrievePassword());
        changeButton.addActionListener(e -> changePassword());
        generateButton.addActionListener(e -> generateAndStorePassword(email));
        exitButton.addActionListener(e -> System.exit(0));

        buttonPanel.add(storeButton);
        buttonPanel.add(retrieveButton);
        buttonPanel.add(changeButton);
        buttonPanel.add(generateButton);
        buttonPanel.add(exitButton);

        frame.add(buttonPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static void storePassword(String email) {
        String site = JOptionPane.showInputDialog("Enter site name:");
        if (site == null || site.trim().isEmpty()) return;

        try {
            if (siteExistsInDB(site)) {
                JOptionPane.showMessageDialog(null, "Password for this site already exists!");
                return;
            }

            int option = JOptionPane.showOptionDialog(null, "Do you want to generate a password?", "Generate Password", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            String password = "";

            if (option == JOptionPane.YES_OPTION) {
                password = generateEasyPassword();
                JOptionPane.showMessageDialog(null, "Generated password: " + password);
            } else if (option == JOptionPane.NO_OPTION) {
                password = JOptionPane.showInputDialog("Enter password:");
                if (password == null || password.trim().isEmpty()) return;
            }

            String encryptedPassword = encrypt(password);
            storePasswordInDB(site, encryptedPassword, email);
            JOptionPane.showMessageDialog(null, "Password stored successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error storing password: " + e.getMessage());
        }
    }

    private static void retrievePassword() {
        String site = JOptionPane.showInputDialog("Enter site name:");
        if (site == null || site.trim().isEmpty()) return;

        try {
            String encryptedPassword = getPasswordFromDB(site);
            if (encryptedPassword == null) {
                JOptionPane.showMessageDialog(null, "No password found for this site.");
                return;
            }
            String decryptedPassword = decrypt(encryptedPassword);
            JOptionPane.showMessageDialog(null, "Password for " + site + ": " + decryptedPassword);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error retrieving password: " + e.getMessage());
        }
    }

    private static void changePassword() {
        String site = JOptionPane.showInputDialog("Enter site name:");
        if (site == null || site.trim().isEmpty()) return;

        try {
            if (!siteExistsInDB(site)) {
                JOptionPane.showMessageDialog(null, "No password found for this site.");
                return;
            }

            int option = JOptionPane.showOptionDialog(null, "Do you want to generate a new password?", "Generate Password", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            String newPassword = "";

            if (option == JOptionPane.YES_OPTION) {
                newPassword = generateEasyPassword();
                JOptionPane.showMessageDialog(null, "Generated new password: " + newPassword);
            } else if (option == JOptionPane.NO_OPTION) {
                newPassword = JOptionPane.showInputDialog("Enter new password:");
                if (newPassword == null || newPassword.trim().isEmpty()) return;
            }

            String encryptedPassword = encrypt(newPassword);
            updatePasswordInDB(site, encryptedPassword);
            JOptionPane.showMessageDialog(null, "Password changed successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error changing password: " + e.getMessage());
        }
    }

    private static void generateAndStorePassword(String email) {
        String site = JOptionPane.showInputDialog("Enter site name:");
        if (site == null || site.trim().isEmpty()) return;

        String generatedPassword = generateEasyPassword();
        JOptionPane.showMessageDialog(null, "Generated password: " + generatedPassword);

        try {
            String encryptedPassword = encrypt(generatedPassword);
            storePasswordInDB(site, encryptedPassword, email);
            JOptionPane.showMessageDialog(null, "Password stored successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error storing password: " + e.getMessage());
        }
    }

    private static String generateEasyPassword() {
        Random random = new Random();
        String word = commonWords[random.nextInt(commonWords.length)];
        int number = random.nextInt(100);
        return word + number;
    }

    private static boolean siteExistsInDB(String site) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT COUNT(*) FROM passwords WHERE site = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, site);
                ResultSet rs = stmt.executeQuery(); {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }

    private static void storePasswordInDB(String site, String encryptedPassword, String email) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String selectSql = "SELECT user_id FROM users WHERE email = ?";
            int userId = -1;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, email);
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    userId = rs.getInt("user_id");
                } else {
                    System.out.println("User not found!");
                    return;
                }
            }

            String insertSql = "INSERT INTO passwords (site, user_id, password) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, site);
                insertStmt.setInt(2, userId);
                insertStmt.setString(3, encryptedPassword);
                insertStmt.executeUpdate();
                System.out.println("Password stored successfully.");
            }
        }
    }

    private static String getPasswordFromDB(String site) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT password FROM passwords WHERE site = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, site);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
        }
        return null;
    }

    private static void updatePasswordInDB(String site, String encryptedPassword) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "UPDATE passwords SET password = ? WHERE site = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, encryptedPassword);
                stmt.setString(2, site);
                stmt.executeUpdate();
            }
        }
    }

    private static void loadOrGenerateKeyAndIV() {
        try {
            Path keyPath = Paths.get(KEY_FILE);
            Path ivPath = Paths.get(IV_FILE);

            if (Files.exists(keyPath) && Files.exists(ivPath)) {
                byte[] keyBytes = Files.readAllBytes(keyPath);
                byte[] ivBytes = Files.readAllBytes(ivPath);

                aesKey = new SecretKeySpec(keyBytes, "AES");
                ivParameterSpec = new IvParameterSpec(ivBytes);
            } else {
                generateKeyAndIV();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading or generating key/IV", e);
        }
    }

    private static void generateKeyAndIV() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            aesKey = keyGen.generateKey();
            ivParameterSpec = new IvParameterSpec(new byte[16]);

            Files.write(Paths.get(KEY_FILE), aesKey.getEncoded());
            Files.write(Paths.get(IV_FILE), ivParameterSpec.getIV());
        } catch (Exception e) {
            throw new RuntimeException("Error generating key and IV", e);
        }
    }

    private static String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivParameterSpec);
            byte[] encryptedData = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    private static String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, ivParameterSpec);
            byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }
}
