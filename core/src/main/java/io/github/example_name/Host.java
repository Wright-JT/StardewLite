package io.github.example_name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple LAN chat server.
 *
 * Usage (rough idea):
 *   Host host = new Host(5000, msg -> chat.addMessage(msg));
 *   host.start();
 *
 * Any client that connects to this machine's local IP on the same port
 * can send and receive chat messages that are broadcast to everyone.
 */
public class Host {

    /** Callback interface for messages received by the server. */
    public interface MessageListener {
        void onMessageReceived(String message);
    }

    private final int port;
    private final MessageListener listener;

    private volatile boolean running = false;
    private Thread serverThread;
    private ServerSocket serverSocket;

    private final List<ClientHandler> clients =
        Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a Host chat server on the given port.
     *
     * @param port      TCP port to listen on (e.g., 5000).
     * @param listener  callback for messages that arrive at the server.
     */
    public Host(int port, MessageListener listener) {
        this.port = port;
        this.listener = listener;
    }

    /**
     * Convenience constructor with default port 5000.
     */
    public Host(MessageListener listener) {
        this(5000, listener);
    }

    /**
     * Starts the server on a background thread.
     */
    public synchronized void start() {
        if (running) return;

        running = true;
        serverThread = new Thread(this::runServer, "ChatHost-ServerThread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Stops the server and disconnects all clients.
     */
    public synchronized void stop() {
        running = false;

        // Close the server socket to break out of accept()
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }

        // Close all clients
        synchronized (clients) {
            for (ClientHandler ch : clients) {
                ch.close();
            }
            clients.clear();
        }
    }

    /**
     * Returns whether the server is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Broadcasts a message to all connected clients.
     * You can also call this from your game to send "System" messages.
     */
    public void broadcast(String message) {
        synchronized (clients) {
            // Use a copy to avoid ConcurrentModificationException
            List<ClientHandler> snapshot = new ArrayList<>(clients);
            for (ClientHandler ch : snapshot) {
                ch.send(message);
            }
        }

        // Also notify local listener (e.g., to show in host's chat box)
        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }

    // -------------------------------------------------------------------------
    // INTERNAL SERVER LOOP
    // -------------------------------------------------------------------------

    private void runServer() {
        try (ServerSocket server = new ServerSocket(port)) {
            this.serverSocket = server;
            System.out.println("[Host] Chat server started on port " + port);

            while (running) {
                try {
                    Socket socket = server.accept();
                    if (!running) {
                        socket.close();
                        break;
                    }

                    ClientHandler handler = new ClientHandler(socket);
                    synchronized (clients) {
                        clients.add(handler);
                    }

                    Thread t = new Thread(handler, "ChatHost-ClientHandler");
                    t.setDaemon(true);
                    t.start();

                    System.out.println("[Host] Client connected: " + socket.getInetAddress());
                } catch (IOException e) {
                    if (running) {
                        System.out.println("[Host] Error accepting client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[Host] Failed to start server on port " + port + ": " + e.getMessage());
        } finally {
            running = false;
            System.out.println("[Host] Server stopped.");
        }
    }

    // -------------------------------------------------------------------------
    // CLIENT HANDLER
    // -------------------------------------------------------------------------

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private volatile boolean connected = true;

        ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("[Host] Error setting up client I/O: " + e.getMessage());
                connected = false;
                close();
            }
        }

        @Override
        public void run() {
            if (!connected) return;

            try {
                // Optional: send a welcome message
                send("System: Welcome to the chat!");

                String line;
                while (connected && (line = in.readLine()) != null) {
                    // `line` already contains "username: text" from the client
                    String msg = "[Client " + socket.getInetAddress().getHostAddress() + "]: " + line;

                    // Broadcast to all *other* clients (not back to the sender)
                    synchronized (clients) {
                        List<ClientHandler> snapshot = new ArrayList<>(clients);
                        for (ClientHandler ch : snapshot) {
                            if (ch != this) {      // <- key change: do not echo to sender
                                ch.send(msg);
                            }
                        }
                    }

                    // Also tell the host / game
                    if (listener != null) {
                        listener.onMessageReceived(msg);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.out.println("[Host] Client error: " + e.getMessage());
                }
            } finally {
                close();
            }
        }

        void send(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }

        void close() {
            connected = false;
            synchronized (clients) {
                clients.remove(this);
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}
            System.out.println("[Host] Client disconnected.");
        }
    }
}
