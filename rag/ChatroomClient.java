import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

class ChatroomClient extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private List<String> chatrooms;
    private Map<String, List<String>> chatroomMessages;
    private String currentChatroom;

    public ChatroomClient() {
        // Prompt user to log in
        String username = JOptionPane.showInputDialog(this, "Enter username:", "Login", JOptionPane.PLAIN_MESSAGE);
        JPasswordField passwordField = new JPasswordField();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Enter password:"), BorderLayout.NORTH);
        panel.add(passwordField, BorderLayout.CENTER);
        JOptionPane.showConfirmDialog(this, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String password = new String(passwordField.getPassword());

        // Check if username and password are valid
        if (!username.equals("bob") || !password.equals("123")) {
            JOptionPane.showMessageDialog(this, "Invalid login credentials. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        setTitle("Chatroom Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);

        // Initialize GUI components
        chatrooms = new ArrayList<String>();
        chatroomMessages = new HashMap<String, List<String>>();
        chatrooms.add("General Chat");
        chatroomMessages.put("General Chat", new ArrayList<String>());
        chatrooms.add("Games Chat");
        chatroomMessages.put("Games Chat", new ArrayList<String>());
        chatrooms.add("Sports Chat");
        chatroomMessages.put("ports Chat", new ArrayList<String>());
        chatrooms.add("School Chat");
        chatroomMessages.put("School Chat", new ArrayList<String>());
        chatrooms.add("Jokes Chat");
        chatroomMessages.put("Jokes Chat", new ArrayList<String>());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        for (String chatroom : chatrooms) {
            JButton chatroomButton = new JButton(chatroom);
            chatroomButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switchChatroom(chatroom);
                }
            });
            leftPanel.add(chatroomButton);
        }

        chatArea = new JTextArea();
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        messageField = new JTextField();
        sendButton = new JButton("Send");
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add GUI components to main window
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        // Add event listener for send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
                sendMessage(message);
                messageField.setText("");
            }
        });

        // Connect to server
        try {
            socket = new Socket("localhost", 9090);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
            chatArea.append("Connected to server.\n");
            switchChatroom("bob");

            // Start listener thread for incoming messages
            Thread t = new Thread(new IncomingMessageHandler());
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        writer.println(currentChatroom + ":" + message);
        writer.flush();
    }

    private void switchChatroom(String chatroom) {
        currentChatroom = chatroom;
        chatArea.setText("");
        for (String msg : chatroomMessages.get(chatroom)) {
            chatArea.append(msg + "\n");
        }
    }

    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = reader.readLine()) != null) {
                    String[] parts = message.split(":");
                    String chatroom = parts[0];
                    String msg = parts[1];
                    chatroomMessages.get(chatroom).add(msg);
                    if (chatroom.equals(currentChatroom)) {
                        chatArea.append(msg + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatroomClient client = new ChatroomClient();
        client.setVisible(true);
    }
}


