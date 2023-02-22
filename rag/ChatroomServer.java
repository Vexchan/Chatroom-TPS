import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatroomServer {
    // A list of output streams for all connected clients
    private List<PrintWriter> clientOutputStreams;
    
    // A map that stores a list of messages for each chat room
    private Map<String, List<String>> chatroomMessages;
    
    // A list of client handlers for all connected clients
    private List<ClientHandler> clients;

    public ChatroomServer() {
        // Constructor for the ChatroomServer class, which starts the server
        startServer();
    }

    private void startServer() {
        try {
            // Create a new server socket for the chat room system
            ServerSocket serverSocket = new ServerSocket(4269);
            
            // Initialize the list of output streams and the map of chat room messages
            clientOutputStreams = new ArrayList<PrintWriter>();
            chatroomMessages = new HashMap<String, List<String>>();
            
            // Create chat rooms and add them to the map of chat room messages
            List<String> generalChatMessages = new ArrayList<String>();
            chatroomMessages.put("General Chat", generalChatMessages);

            List<String> gamesChatMessages = new ArrayList<String>();
            chatroomMessages.put("Games Chat", gamesChatMessages);

            List<String> sportsChatMessages = new ArrayList<String>();
            chatroomMessages.put("Sports Chat", sportsChatMessages);

            List<String> schoolChatMessages = new ArrayList<String>();
            chatroomMessages.put("School Chat", schoolChatMessages);

            List<String> jokesChatMessages = new ArrayList<String>();
            chatroomMessages.put("Jokes Chat", jokesChatMessages);
            
            // Initialize the list of client handlers
            clients = new ArrayList<ClientHandler>();
            
            // Accept new client connections and create a new client handler thread for each one
            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                // Create a new output stream for the client and add it to the list of output streams
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                clientOutputStreams.add(writer);
                
                // Create a new client handler for the client and add it to the list of client handlers
                ClientHandler client = new ClientHandler(clientSocket);
                clients.add(client);
                
                // Start a new thread for the client handler
                Thread t = new Thread(client);
                t.start();
            }
        } catch (IOException e) {
            // Print the stack trace if an I/O exception occurs
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        // Split the message into parts: the nickname, the chat room, and the message itself
        String[] parts = message.split(":");
        String nickname = parts[0];
        String chatroom = parts[1];
        String msg = (nickname + ": " + parts[2]); // The actual message is parts[2]

        // Add the message to the list of messages for the chat room
        List<String> messages = chatroomMessages.get(chatroom);
        messages.add(msg);
        
        // Send the message to all connected clients by writing it to their output streams
        for (PrintWriter writer : clientOutputStreams) {
            writer.println(message);
            writer.flush();
        }
    }


    private class ClientHandler implements Runnable {
        private BufferedReader reader;
        private PrintWriter writer;
        private String nickname = null;
        private String currChatroom;
        
        public ClientHandler(Socket clientSocket) {
            try {
                // Create a new input stream and output stream for the client
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream());
            } catch (IOException e) {
                // Print the stack trace if an I/O exception occurs
                e.printStackTrace();
            }
        }
                    
        @Override
        public void run() {
            String message;
            try {
                // Read the nickname from the client and set it as the current nickname
                String nicknameResult = reader.readLine();
                String[] partsFirst = nicknameResult.split(":");
                nickname = partsFirst[1];
                writer.flush();
                
                // Continuously read messages from the client and handle them accordingly
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("get_messages;")) {
                        // If the message is a request for chat room messages, send them to the client
                        String[] parts = message.split(";"); //parts[1] is the chat room name
                        String chatroom = parts[1];
                        List<String> messages = chatroomMessages.get(chatroom);
                        if (messages != null) {
                            for (String msg : messages) {
                                writer.println("get_messages;" + chatroom + ";" + msg);
                                writer.flush();
                            }
                        }
                    } 
                    else if (message.startsWith("joinedNewRoom:"))
                    {
                        // If the message is a notification that the client has joined a new chat room, 
                        // add the client to the new chat room and send a notification to all connected clients
                        String[] parts = message.split(":"); //parts[1] is the chat room name
                        currChatroom = parts[1];
                        String msg = ("systemHello:" + parts[1] + ":" + nickname + " has joined the chat.");
                        List<String> messages = chatroomMessages.get(parts[1]);
                        messages.add(nickname + " has joined the chat.");
                        
                        for (PrintWriter writer : clientOutputStreams) {
                            writer.println(msg);
                            writer.flush();
                        }
                    }
                    else if (message.startsWith("checkingName:"))
                    {
                        // If the message is a request to check if a nickname is available, do nothing as it is already done in a 
                    	// previous segment
                    }
                    else if (message.startsWith("leftRoom:"))
                    {
                        // If the message is a notification that the client has left a chat room,
                        // remove the client from the chat room and send a notification to all connected clients
                        String[] parts = message.split(":"); //parts[2] is the chat room name
                        String msg = ("systemHello:" + parts[2] + ":" + nickname + " has left the chat.");
                        List<String> messages = chatroomMessages.get(parts[2]);
                        if (messages != null)
                            messages.add(nickname + " has left the chat.");
                        
                        for (PrintWriter writer : clientOutputStreams) {
                            writer.println(msg);
                            writer.flush();
                        }
                    }
                    else if (message.startsWith("viewUsers:"))
                    {
                        // If the message is a request to view all users in the current chat room,
                        // send a list of all connected users in the current chat room to the client
                        String msg = "viewUsers:";
                        for (ClientHandler client : clients) {
                            if (client.nickname != null && client.currChatroom != null && client.currChatroom.equals(currChatroom)) {
                                msg += client.nickname + ", ";
                            }
                        }
                        writer.println(msg);
                        writer.flush();
                    }
                    else if (message.startsWith("changeNickname:"))
                    {
                        // If the message is a request to change the
                        // client's nickname, update the nickname
                        String[] parts = message.split(":"); //parts[1] is our new nickname
                        if (parts[1] != null)
                            nickname = parts[1];
                    }
                    else {
                        // If the message is a regular chat message, broadcast it to all connected clients
                        broadcastMessage(message);
                    }
                }
            } catch (IOException e) {
                // If an I/O exception occurs while reading messages from the client, disconnect the client as they have left
                System.out.println("Client disconnected: " + e.getMessage());
                for (int i = 0; i < clients.size(); i++) {
                    ClientHandler client = clients.get(i);
                    if (client == this) {
                        clients.remove(i);
                        break;
                    }
                }
                clientOutputStreams.remove(writer);
            }
        }
    }

    public static void main(String[] args) {
        // Create a new instance of the ChatroomServer class to start the server
        ChatroomServer server = new ChatroomServer();
    }
}