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
		String chatroom = parts[0];
		String msg = parts[1];

		if (!chatroomMessages.containsKey(chatroom)) {
			chatroomMessages.put(chatroom, new ArrayList<String>());
		}

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
				while ((message = reader.readLine()) != null) {
					if (message.startsWith("get_messages:")) {
						String chatroom = message.substring(13);
						List<String> messages = chatroomMessages.get(chatroom);
						if (messages != null) {
							for (String msg : messages) {
								writer.println(chatroom + ":" + msg);
								writer.flush();
							}
						}
					} else {
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
