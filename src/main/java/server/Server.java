package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import server.repository.Database;

/**
 * Servidor com GUI para monitoramento simples (lista de clientes + console por IP).
 */
public class Server {

    private static final int PORTA = 21212;

    private static ServerGui gui = new ServerGui();
    private static final Map<String, List<String>> messages = new ConcurrentHashMap<>();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        // Inicializa DB
        Database.initialize();

        // Start GUI
        SwingUtilities.invokeLater(() -> gui.init());

        // Start server accept loop in background thread
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
                System.out.println("Servidor iniciado e ouvindo na porta " + PORTA);

                while (true) {
                    try {
                        System.out.println("Aguardando conexão de um novo cliente...");
                        Socket clientSocket = serverSocket.accept();
                        String clientIp = clientSocket.getInetAddress().getHostAddress();
                        System.out.println("Cliente conectado: " + clientIp);

                        // Start client handler thread
                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        Thread clientThread = new Thread(clientHandler);
                        clientThread.start();

                    } catch (IOException e) {
                        System.err.println("Erro ao aceitar conexão do cliente: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
                System.err.println("Erro fatal ao iniciar o servidor na porta " + PORTA + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, "Server-Accept-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    // Métodos usados por ClientHandler
    public static void registerClient(String ip) {
        messages.putIfAbsent(ip, Collections.synchronizedList(new ArrayList<>()));
        gui.addClient(ip);
    }

    public static void unregisterClient(String ip) {
        gui.removeClient(ip);
    }

    public static void logMessage(String ip, String direction, String message) {
        String entry = String.format("[%s] %s: %s", sdf.format(new Date()), direction, message);
        messages.computeIfAbsent(ip, k -> Collections.synchronizedList(new ArrayList<>())).add(entry);
        gui.appendMessage(ip, entry);
    }

    // Classe implementando a GUI
    private static class ServerGui {
        private JFrame frame;
        private DefaultListModel<String> clientsModel;
        private JList<String> clientsList;
        private JTextArea consoleArea;

        void init() {
            frame = new JFrame("Servidor - Monitor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(new Dimension(800, 480));
            frame.setLocationRelativeTo(null);

            // Tabbed pane
            JTabbedPane tabs = new JTabbedPane();

            // Clients tab
            clientsModel = new DefaultListModel<>();
            clientsList = new JList<>(clientsModel);
            clientsList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            clientsList.setBackground(new Color(34,34,34));
            clientsList.setForeground(Color.WHITE);
            clientsList.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

            JScrollPane clientsScroll = new JScrollPane(clientsList);
            JPanel clientsPanel = new JPanel(new BorderLayout());
            clientsPanel.add(clientsScroll, BorderLayout.CENTER);

            // Console tab
            consoleArea = new JTextArea();
            consoleArea.setEditable(false);
            consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            consoleArea.setBackground(new Color(8,8,8));
            consoleArea.setForeground(Color.WHITE);
            consoleArea.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            JScrollPane consoleScroll = new JScrollPane(consoleArea);
            JPanel consolePanel = new JPanel(new BorderLayout());
            consolePanel.add(consoleScroll, BorderLayout.CENTER);

            tabs.addTab("Clientes", clientsPanel);
            tabs.addTab("Console", consolePanel);

            frame.getContentPane().add(tabs, BorderLayout.CENTER);

            // Dark theme
            frame.getContentPane().setBackground(new Color(24,24,24));
            tabs.setBackground(new Color(34,34,34));

            frame.setVisible(true);
        }

        void addClient(String ip) {
            SwingUtilities.invokeLater(() -> {
                if (clientsModel.contains(ip)) return;
                clientsModel.addElement(ip);
            });
        }

        void removeClient(String ip) {
            SwingUtilities.invokeLater(() -> {
                clientsModel.removeElement(ip);
            });
        }

        void appendMessage(String ip, String entry) {
            SwingUtilities.invokeLater(() -> {
                String current = consoleArea.getText();
                String line = String.format("%s - %s\n", ip, entry);
                consoleArea.append(line);
                // Auto-scroll
                consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
            });
        }
    }
}