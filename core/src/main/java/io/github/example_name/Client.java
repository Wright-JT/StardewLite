package io.github.example_name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {

    public interface MessageListener {
        void onMessage(String message);
    }

    private final String host;
    private final int port;
    private final String username;
    private final MessageListener listener;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenThread;
    private volatile boolean connected = false;

    public Client(String host, int port, String username, MessageListener listener) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.listener = listener;
    }

    /**
     * Connect using a timeout so game doesn't freeze.
     */
    public boolean connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 3000); // 3-second timeout

            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send username to server
            out.println(username);

            connected = true;

            // Start message listening thread
            startListening();

            System.out.println("Client connected to " + host + ":" + port);
            return true;

        } catch (IOException e) {
            System.out.println("Client connect failed: " + e.getMessage());
            close();
            return false;
        }
    }

    /**
     * Listens for messages from the server in a background thread.
     */
    private void startListening() {
        listenThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    if (listener != null) {
                        listener.onMessage(line);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Client listen error: " + e.getMessage());
                }
            } finally {
                close();
            }
        }, "Client-Listen-Thread");

        listenThread.setDaemon(true);
        listenThread.start();
    }

    /** Sends a chat message including username prefix. */
    public void sendChatMessage(String text) {
        if (!connected || out == null || text == null || text.isEmpty()) return;
        sendRaw(username + ": " + text);
    }

    /** Sends a raw line to the server. */
    public void sendRaw(String line) {
        if (!connected || out == null || line == null) return;
        out.println(line);
        out.flush();
    }

    public void disconnect() {
        close();
    }

    private void close() {
        connected = false;

        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();

        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}

        in = null;
        out = null;
        socket = null;

        System.out.println("Client disconnected.");
    }

    public boolean isConnected() {
        return connected;
    }

    public String getUsername() {
        return username;
    }
}
