import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

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
    private String currentChatroom = "Jokes Chat";
	private String nickname;
	private boolean firstTime = true;
	private JLabel globalTitleLabel;
	
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
    	if (!((username.equals("user1") && password.equals("abcdef")) ||
    	      (username.equals("user2") && password.equals("123456")) ||
    	      (username.equals("user3") && password.equals("chatroom")))) {
    	    JOptionPane.showMessageDialog(this, "Invalid login credentials. Exiting...", "Error", JOptionPane.ERROR_MESSAGE);
    	    System.exit(1);
    	}

        // Set up the window
        setTitle("Chatroom Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        // Get the user's nickname
        while (true) {
            nickname = JOptionPane.showInputDialog(this, "Enter nickname:", "Nickname", JOptionPane.PLAIN_MESSAGE);
            if (nickname == null)
            	System.exit(0);
            if (isValidNickname(nickname)) {
                break;
            } else {
                JOptionPane.showMessageDialog(this, "Invalid nickname. Nickname must be between 3-15 characters long and contain only letters.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        // Initialize GUI components
        chatrooms = new ArrayList<String>();
        chatrooms.add("General Chat");
        chatrooms.add("Games Chat");
        chatrooms.add("Sports Chat");
        chatrooms.add("School Chat");
        chatrooms.add("Jokes Chat");

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
      
        // Create buttons for each chatroom and add them to the left panel
        for (final String chatroom : chatrooms) {
            JButton chatroomButton = new JButton(chatroom);
            chatroomButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            chatroomButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switchChatroom(chatroom);
                }
            });
            leftPanel.add(chatroomButton);
        }
        // Set up the font for the chat area
        Font chatFont = new Font("SansSerif", Font.PLAIN, 16);
        
        chatArea = new JTextArea();
        chatArea.setFont(chatFont);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        messageField = new JTextField();
        sendButton = new JButton("Send");
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        JPanel topRightPanel = new JPanel(new BorderLayout());
        topRightPanel.add(rightPanel, BorderLayout.CENTER);
        
        JButton checkUsersButton = new JButton("View Users");
        topRightPanel.add(checkUsersButton, BorderLayout.EAST);
        
        JButton changeNickName = new JButton("Change Nickname");
        topRightPanel.add(changeNickName, BorderLayout.WEST);
        
        // Increase the height of the top right panel and set preferred button sizes
        Dimension preferredSizeTopRightPanel = topRightPanel.getPreferredSize();
        preferredSizeTopRightPanel.height += 25; // increase the height by 25 pixels
        topRightPanel.setPreferredSize(preferredSizeTopRightPanel);
        
        Dimension preferredSizeUserButton = new Dimension(topRightPanel.getPreferredSize().width - 85, checkUsersButton.getPreferredSize().height);
        checkUsersButton.setPreferredSize(preferredSizeUserButton);
        
        Dimension preferredSizeChangeNickname = new Dimension(topRightPanel.getPreferredSize().width - 150, checkUsersButton.getPreferredSize().height);
        changeNickName.setPreferredSize(preferredSizeChangeNickname);
        
        // Set up the title label for the current chatroom
        JLabel titleLabel = new JLabel("My Chat App");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topRightPanel.add(titleLabel, BorderLayout.CENTER);
        Font labelFont = titleLabel.getFont();
        titleLabel.setFont(new Font(labelFont.getName(), Font.BOLD, 24));
        globalTitleLabel = titleLabel;
        
        // Add GUI components to the main window
        add(topRightPanel, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        
        // Add event listener for the send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
                sendMessage(message);
                messageField.setText("");
            }
        });

        // Add event listener for the "View Users" button
        checkUsersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	writer.println("viewUsers:");
        		writer.flush();
            }
        });

        // Add event listener for the "Change Nickname" button
        changeNickName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	String nickNameResult = ChangeNickname();
            	if (nickNameResult != null)
            	{
            		nickname = nickNameResult;
	            	writer.println("changeNickname:" + nickname);
	        		writer.flush();
            	}
            }
        });
        
        // Connect to the server
        try {
            socket = new Socket("localhost", 4269);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
            switchChatroom("General Chat");

            // Start listener thread for incoming messages
            Thread t = new Thread(new IncomingMessageHandler());
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };
    
    // Method to get the user's desired nickname
    private String ChangeNickname()
    {
    	String tempName = "";
    	while (true) {
    		tempName = JOptionPane.showInputDialog(this, "Enter nickname:", "Nickname", JOptionPane.PLAIN_MESSAGE);
            if (tempName == null)
            	return null;
            if (isValidNickname(tempName)) {
            	return tempName;
            } else {
                JOptionPane.showMessageDialog(this, "Invalid nickname. Nickname must be between 3-30 characters long and contain only letters.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
 // Method to validate a nickname
    private boolean isValidNickname(String nickname) {
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            // Nickname is null or not between 3-30 characters long
            return false;
        }
        for (char c : nickname.toCharArray()) {
            if (!Character.isLetter(c)) {
                // Nickname contains a non-letter character
                return false;
            }
        }
        // Nickname is valid
        return true;
    }

    // Method to show the input dialog box for viewing users in the current chatroom
    private void showInputDialogBox() {
    	writer.println("viewUsers:" + nickname);
		writer.flush();
        JOptionPane.showMessageDialog(null, "Yo whats good \n Yall crazy!", "Users In " + currentChatroom, JOptionPane.INFORMATION_MESSAGE);
    }

    // Method to send a message to the server
    private void sendMessage(String message) {
        writer.println(nickname + ":" + currentChatroom + ":" + message);
        writer.flush();
    }

    // Method to switch to a different chatroom, gets the messages of new room and tells server its leaving
    private void switchChatroom(String chatroom) {
    	if (currentChatroom.equals(chatroom))
    		return;
    	globalTitleLabel.setText(chatroom);
    	if (firstTime == false)
    	{
	    	writer.println("leftRoom:" + nickname + ":" + currentChatroom);
			writer.flush();
    	}
    	firstTime = false;
		currentChatroom = chatroom;
		chatArea.setText("");
		writer.println("checkingName:" + nickname);
		writer.flush();
		writer.println("get_messages;" + chatroom);
		writer.flush();
		writer.println("joinedNewRoom:" + chatroom);
		writer.flush();
	}

    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                // Continuously read messages from the server and handle them accordingly
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("viewUsers:"))
                    {
                        // If the message is a request to view all users in the current chat room,
                        // display a dialog box showing the list of users
                        String[] parts = message.split(":");
                        JOptionPane.showMessageDialog(null, parts[1].substring(0, parts[1].length() - 2), "Users In " + currentChatroom, JOptionPane.INFORMATION_MESSAGE);
                    }
                    else if (message.startsWith("get_messages;")) {
                        // If the message contains a chat room message, add it to the chat area
                        String[] parts = message.split(";");
                        String chatroom = parts[1];
                        String msg = (parts[2]); // Combines nickname and the user's text
                        if (currentChatroom.equals(chatroom))
                        {
                            chatArea.append(msg + "\n");
                        }
                    } 
                    else if (message.startsWith("get_username:")) 
                    {
                        // If the message is a request to check if a nickname is available, send a response to the server
                        writer.println("checkingName:" + nickname);
                    }
                    else if (message.startsWith("systemHello"))
                    {
                        // If the message is a system message, add it to the chat area if it is for the current chat room
                        String[] parts = message.split(":");
                        String chatroom = parts[1];
                        String msg = parts[2];
                        if (currentChatroom.equals(chatroom))
                            chatArea.append(msg + "\n");
                    }
                    else {
                        // If the message is a regular chat message, add it to the chat area if it is for the current chat room
                        String[] parts = message.split(":");
                        String chatroom = parts[1];
                        String msg = parts[2];
                        if (currentChatroom.equals(chatroom)) {
                            chatArea.append(parts[0] + ": " + msg + "\n");
                        }
                    }
                }
            } catch (IOException e) {
                // If an I/O exception occurs while reading messages from the server, print the stack trace
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        // Create a new instance of the ChatroomClient class and make it visible
        ChatroomClient client = new ChatroomClient();
        client.setVisible(true);
    }

}

