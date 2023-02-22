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
    private List<PrintWriter> clientOutputStreams;
	private Map<String, List<String>> chatroomMessages;

    public ChatroomServer() {
        startServer();
    }

    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(4269);
            clientOutputStreams = new ArrayList<PrintWriter>();
            chatroomMessages = new HashMap<String, List<String>>(); // initialize chatroomMessages here

            // Create chat rooms
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

            while (true) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                clientOutputStreams.add(writer);

                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
		String[] parts = message.split(":");
		String nickname = parts[0];
		String chatroom = parts[1];
		String msg = (nickname + ": " + parts[2]); // The actual message is parts[2]

		List<String> messages = chatroomMessages.get(chatroom);
		messages.add(msg);
		
		for (PrintWriter writer : clientOutputStreams) {
			writer.println(message);
			writer.flush();
		}
	}


	private class ClientHandler implements Runnable {
		private BufferedReader reader;
		private PrintWriter writer;
		private String nickname = null;
		
		public ClientHandler(Socket clientSocket) {
			try {
				reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				writer = new PrintWriter(clientSocket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			String message;
			try {
			    String nicknameResult = reader.readLine();
				String[] partsFirst = nicknameResult.split(":");
				nickname = partsFirst[1];
				writer.flush();
				
				while ((message = reader.readLine()) != null) {
					if (message.startsWith("get_messages;")) {
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
						String[] parts = message.split(":"); //parts[1] is the chat room name
						String msg = ("systemHello:" + parts[1] + ":" + nickname + " has joined the chat.");
						List<String> messages = chatroomMessages.get(parts[1]);
						messages.add(nickname + " has joined the chat.");
						
						for (PrintWriter writer : clientOutputStreams) {
							writer.println(msg);
							writer.flush();
						}
					}
					else if (message.startsWith("checkingName:"))
					{}
					else if (message.startsWith("leftRoom:"))
					{
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
					else {
						broadcastMessage(message);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

    public static void main(String[] args) {
        ChatroomServer server = new ChatroomServer();
    }
}
