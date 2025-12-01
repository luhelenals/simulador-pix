package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import common.validator.Validator;
import java.util.Set;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;

public class Client {

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private String token; // Armazena o token de sessão do usuário logado

    // GUI components
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel cards;
    private JTextArea consoleArea; // exibe mensagens cruas enviadas/recebidas

    public Client(String host, int port) {
        this.connection = new Connection(host, port);
        this.objectMapper = new ObjectMapper();
        this.token = null;
    }

    /**
     * Inicializa e exibe a GUI. Mantém toda a lógica dentro desta classe.
     */
    public void start() {
        SwingUtilities.invokeLater(this::buildAndShowGui);
    }

    private void buildAndShowGui() {
        // apply a dark UI theme for dialogs and common components
        setDarkUI();

        frame = new JFrame("Cliente PIX - GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // smaller window to fit only buttons comfortably
        frame.setSize(640, 360);
        frame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        // dark background for cards
        cards.setBackground(new Color(34,34,34));

        cards.add(buildConnectionPanel(), "connection");
        cards.add(buildPublicPanel(), "public");
        cards.add(buildAuthPanel(), "auth");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.6);

        split.setTopComponent(cards);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(34,34,34));
        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleArea.setBackground(new Color(24,24,24));
        consoleArea.setForeground(new Color(220,220,220));
        consoleArea.setCaretColor(new Color(220,220,220));
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(60,60,60)), "Console (JSON raw send/receive)");
        tb.setTitleColor(new Color(200,200,200));
        consoleScroll.setBorder(tb);

        // Only show the raw JSON console in the bottom area now
        bottom.add(consoleScroll, BorderLayout.CENTER);

        split.setBottomComponent(bottom);

        frame.setContentPane(split);
        frame.setVisible(true);
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(34,34,34));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);

        JLabel hostLabel = new JLabel("Host:");
        hostLabel.setForeground(Color.WHITE);
        JTextField hostField = new JTextField(20);
        hostField.setText("localhost");
        hostField.setBackground(new Color(60,63,65));
        hostField.setForeground(Color.WHITE);
        JLabel portLabel = new JLabel("Porta:");
        portLabel.setForeground(Color.WHITE);
        JTextField portField = new JTextField(6);
        portField.setText("9000");
        portField.setBackground(new Color(60,63,65));
        portField.setForeground(Color.WHITE);

        JButton connectBtn = new JButton("Conectar");
        connectBtn.setBackground(new Color(60,63,65));
        connectBtn.setForeground(Color.WHITE);
        connectBtn.setFocusPainted(false);
        connectBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            int port;
            try { port = Integer.parseInt(portField.getText().trim()); }
            catch (NumberFormatException ex) { JOptionPane.showMessageDialog(frame, "Porta inválida"); return; }

            appendConsole("[ACTION] connect -> {\"host\": \""+host+"\", \"port\": "+port+"}");

            // recreate connection object? We keep existing Connection but attempt to connect using provided host/port
            // For simplicity, create a new Connection locally and try to connect using a temporary socket call.
            // However the class's connection fields are final, so we will attempt to connect using the existing Connection
            // which was constructed at startup; advise user to restart if different host/port needed.

            new Thread(() -> {
                boolean ok = connection.connect();
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        appendConsole("[INFO] Conectado ao servidor " + host + ":" + port);
                        cardLayout.show(cards, "public");
                    } else {
                        appendConsole("[ERROR] Falha ao conectar");
                        JOptionPane.showMessageDialog(frame, "Falha ao conectar ao servidor. Verifique se ele está rodando.");
                    }
                });
            }).start();
        });

        c.gridx=0; c.gridy=0; panel.add(hostLabel, c);
        c.gridx=1; panel.add(hostField, c);
        c.gridx=0; c.gridy=1; panel.add(portLabel, c);
        c.gridx=1; panel.add(portField, c);
        c.gridx=0; c.gridy=2; c.gridwidth=2; panel.add(connectBtn, c);

        return panel;
    }

    private JPanel buildPublicPanel() {
        // Use GridLayout so buttons are equally distributed
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        panel.setBackground(new Color(34,34,34));

        JButton criarBtn = new JButton("Criar Conta");
        criarBtn.addActionListener(this::onCriarConta);
        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(this::onLogin);
        JButton sairBtn = new JButton("Desconectar");
        sairBtn.addActionListener(e -> {
            connection.disconnect();
            appendConsole("[INFO] Desconectado");
            cardLayout.show(cards, "connection");
        });

        for (JButton b : new JButton[]{criarBtn, loginBtn, sairBtn}) {
            b.setBackground(new Color(60,63,65));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            panel.add(b);
        }

        return panel;
    }

    private JPanel buildAuthPanel() {
        // Grid layout to distribute buttons evenly (3x3)
        JPanel panel = new JPanel(new GridLayout(3, 3, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        panel.setBackground(new Color(34,34,34));

        JButton verSaldoBtn = new JButton("Ver Saldo e Dados");
        verSaldoBtn.addActionListener(e -> runRequest("usuario_ler", null, true));

        JButton depositarBtn = new JButton("Depositar");
        depositarBtn.addActionListener(this::onDepositar);

        JButton pixBtn = new JButton("Fazer PIX");
        pixBtn.addActionListener(this::onFazerPix);

        JButton extratoBtn = new JButton("Ver Extrato");
        extratoBtn.addActionListener(this::onVerExtrato);

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            ObjectNode req = objectMapper.createObjectNode();
            req.put("operacao", "usuario_logout");
            req.put("token", token);
            runRawRequest(req.toString());
            token = null;
            cardLayout.show(cards, "public");
        });

        JButton atualizarBtn = new JButton("Editar Conta");
        atualizarBtn.addActionListener(this::onAtualizar);

        JButton deletarBtn = new JButton("Deletar Conta");
        deletarBtn.addActionListener(e -> {
            ObjectNode req = objectMapper.createObjectNode();
            req.put("operacao", "usuario_deletar");
            req.put("token", token);
            runRawRequest(req.toString());
            token = null;
            cardLayout.show(cards, "public");
        });

        JButton[] buttons = new JButton[]{verSaldoBtn, depositarBtn, pixBtn, extratoBtn, atualizarBtn, deletarBtn, logoutBtn};
        for (JButton b : buttons) {
            b.setBackground(new Color(60,63,65));
            b.setForeground(Color.WHITE);
            b.setFocusPainted(false);
            panel.add(b);
        }

        // fill remaining cells to keep grid shape
        int remaining = 9 - buttons.length;
        for (int i=0;i<remaining;i++) panel.add(new JLabel());

        return panel;
    }

    // ---------- GUI action handlers ----------
    private void onCriarConta(ActionEvent e) {
        JTextField nome = new JTextField();
        JFormattedTextField cpf = createCpfField();
        JPasswordField senha = new JPasswordField();
        Object[] fields = {"Nome:", nome, "CPF:", cpf, "Senha:", senha};
        int option = JOptionPane.showConfirmDialog(frame, fields, "Criar Conta", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            ObjectNode req = objectMapper.createObjectNode();
            req.put("operacao", "usuario_criar");
            req.put("nome", nome.getText());
            req.put("cpf", cpf.getText());
            req.put("senha", new String(senha.getPassword()));
            runRawRequest(req.toString());
        }
    }

    private void onLogin(ActionEvent e) {
        JFormattedTextField cpf = createCpfField();
        JPasswordField senha = new JPasswordField();
        Object[] fields = {"CPF:", cpf, "Senha:", senha};
        int option = JOptionPane.showConfirmDialog(frame, fields, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            ObjectNode req = objectMapper.createObjectNode();
            req.put("operacao", "usuario_login");
            req.put("cpf", cpf.getText());
            req.put("senha", new String(senha.getPassword()));

            runRequestWithTokenHandling(req.toString());
        }
    }

    private void onDepositar(ActionEvent e) {
        String valorStr = JOptionPane.showInputDialog(frame, "Valor a depositar:");
        if (valorStr == null) return;
        try {
            double valor = Double.parseDouble(valorStr.replace(',', '.'));
            ObjectNode req = objectMapper.createObjectNode();
            req.put("operacao", "depositar");
            req.put("token", token);
            req.put("valor_enviado", valor);
            runRawRequest(req.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Valor inválido");
        }
    }

    private void onFazerPix(ActionEvent e) {
        JFormattedTextField cpf = createCpfField();
        JTextField valor = new JTextField();
        Object[] fields = {"CPF destino:", cpf, "Valor:", valor};
        int opt = JOptionPane.showConfirmDialog(frame, fields, "Fazer PIX", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                double v = Double.parseDouble(valor.getText().replace(',', '.'));
                ObjectNode req = objectMapper.createObjectNode();
                req.put("operacao", "transacao_criar");
                req.put("token", token);
                req.put("cpf_destino", cpf.getText());
                req.put("valor", v);
                runRawRequest(req.toString());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Valor inválido");
            }
        }
    }

    private void onVerExtrato(ActionEvent e) {
        // Use date pickers (spinners) so user can pick dates; default to today
        SpinnerDateModel modelInicial = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        JSpinner dataInicialSpinner = new JSpinner(modelInicial);
        dataInicialSpinner.setEditor(new JSpinner.DateEditor(dataInicialSpinner, "yyyy-MM-dd"));

        SpinnerDateModel modelFinal = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        JSpinner dataFinalSpinner = new JSpinner(modelFinal);
        dataFinalSpinner.setEditor(new JSpinner.DateEditor(dataFinalSpinner, "yyyy-MM-dd"));

        Object[] fields = {"Data inicial:", dataInicialSpinner, "Data final:", dataFinalSpinner};
        int opt = JOptionPane.showConfirmDialog(frame, fields, "Ver Extrato", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String di = sdf.format((Date) dataInicialSpinner.getValue());
            String df = sdf.format((Date) dataFinalSpinner.getValue());
            String diIso = di + "T00:00:00Z";
            String dfIso = df + "T23:59:59Z";

            ObjectNode req = objectMapper.createObjectNode();
            req.put("operacao", "transacao_ler");
            req.put("token", token);
            req.put("data_inicial", diIso);
            req.put("data_final", dfIso);

            // obter dados do usuário e extrato para formatar
            new Thread(() -> {
                String dadosUsuarioJson = runRawRequestBlocking(objectMapper.createObjectNode().put("operacao", "usuario_ler").put("token", token).toString());
                String responseJson = runRawRequestBlocking(req.toString());
                if (responseJson != null && dadosUsuarioJson != null) {
                    try {
                        JsonNode resp = objectMapper.readTree(responseJson);
                        JsonNode respUsuario = objectMapper.readTree(dadosUsuarioJson);
                        SwingUtilities.invokeLater(() -> formatAndShowExtrato(resp, respUsuario));
                    } catch (JsonProcessingException ex) {
                        appendConsole("[ERROR] falha ao parsear JSON: " + ex.getMessage());
                    }
                }
            }).start();
        }
    }

    private void onAtualizar(ActionEvent e) {
        JTextField nome = new JTextField();
        JPasswordField senha = new JPasswordField();
        Object[] fields = {"Novo nome (deixar em branco para manter):", nome, "Nova senha (deixar em branco para manter):", senha};
        int opt = JOptionPane.showConfirmDialog(frame, fields, "Atualizar conta", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            ObjectNode req = objectMapper.createObjectNode();
            req.put("operacao", "usuario_atualizar");
            req.put("token", token);
            ObjectNode node = objectMapper.createObjectNode();
            if (!nome.getText().isEmpty()) node.put("nome", nome.getText());
            if (senha.getPassword().length > 0) node.put("senha", new String(senha.getPassword()));
            req.set("usuario", node);
            runRawRequest(req.toString());
        }
    }

    // ---------- network helpers ----------
    private void runRequest(String operacao, ObjectNode extraNode, boolean showFormatted) {
        ObjectNode req = objectMapper.createObjectNode();
        req.put("operacao", operacao);
        if (token != null) req.put("token", token);
        if (extraNode != null) req.setAll(extraNode);
        runRawRequest(req.toString(), showFormatted);
    }

    private void runRequestWithTokenHandling(String jsonRequest) {
        // send and if response contains token, store it and switch to auth panel
        new Thread(() -> {
            // validate request before sending
            try {
                Validator.validateClient(jsonRequest);
            } catch (Exception vex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                        "Mensagem inválida (não enviada): " + vex.getMessage(), "Erro de validação", JOptionPane.ERROR_MESSAGE));
                appendConsole("[ERROR] mensagem inválida não enviada: " + vex.getMessage());
                return;
            }

            appendConsole("OUT> " + jsonRequest);
            String resp = connection.sendRequest(jsonRequest);
            appendConsole("IN> " + resp);
            if (resp != null) {
                try {
                    JsonNode node = Validator.validateServer(resp);
                    SwingUtilities.invokeLater(() -> {
                        // If server returned user data, show only the user data popup
                        if (node.has("usuario")) {
                            showHtmlPopup("Dados do Usuário", htmlWrap(formatUsuarioHtml(node.get("usuario"))), new Dimension(360, 200));
                        } else {
                            // simple confirmation: show a small OK dialog with only the info
                            JOptionPane.showMessageDialog(frame, node.path("info").asText(), "Mensagem", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                    if (node.path("status").asBoolean(false) && node.has("token")) {
                        this.token = node.get("token").asText();
                        SwingUtilities.invokeLater(() -> cardLayout.show(cards, "auth"));
                    }
                } catch (Exception ex) {
                    handleMalformedServerMessage(resp, jsonRequest, ex);
                }
            }
        }).start();
    }

    private void runRawRequest(String requestJson) {
        runRawRequest(requestJson, false);
    }

    private void runRawRequest(String requestJson, boolean showFormatted) {
        new Thread(() -> {
            // validate request before sending
            try {
                Validator.validateClient(requestJson);
            } catch (Exception vex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                        "Mensagem inválida (não enviada): " + vex.getMessage(), "Erro de validação", JOptionPane.ERROR_MESSAGE));
                appendConsole("[ERROR] mensagem inválida não enviada: " + vex.getMessage());
                return;
            }

            appendConsole("OUT> " + requestJson);
            String resp = connection.sendRequest(requestJson);
            appendConsole("IN> " + resp);
            if (resp != null) {
                try {
                    JsonNode node = Validator.validateServer(resp);

                    SwingUtilities.invokeLater(() -> {
                        // If caller requested formatted display and server returned user data, show only that data
                        if (showFormatted && node.has("usuario")) {
                            showHtmlPopup("Dados do Usuário", htmlWrap(formatUsuarioHtml(node.get("usuario"))), new Dimension(360, 200));
                        } else {
                            // otherwise, treat as a simple confirmation and show an OK dialog with only the info
                            JOptionPane.showMessageDialog(frame, node.path("info").asText(), "Mensagem", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });

                    // token handling for login
                    if (node.path("status").asBoolean(false) && node.has("token")) {
                        this.token = node.get("token").asText();
                        SwingUtilities.invokeLater(() -> cardLayout.show(cards, "auth"));
                    }
                } catch (Exception ex) {
                    handleMalformedServerMessage(resp, requestJson, ex);
                }
            }
        }).start();
    }

    private String runRawRequestBlocking(String requestJson) {
        // validate request before sending
        try {
            Validator.validateClient(requestJson);
        } catch (Exception vex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                    "Mensagem inválida (não enviada): " + vex.getMessage(), "Erro de validação", JOptionPane.ERROR_MESSAGE));
            appendConsole("[ERROR] mensagem inválida não enviada: " + vex.getMessage());
            return null;
        }

        appendConsole("OUT> " + requestJson);
        String resp = connection.sendRequest(requestJson);
        appendConsole("IN> " + resp);

        if (resp != null) {
            try {
                Validator.validateServer(resp);
            } catch (Exception ex) {
                handleMalformedServerMessage(resp, requestJson, ex);
                return null;
            }
        }

        return resp;
    }

    // ---------- UI helpers ----------
    private void appendConsole(String text) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(text + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    /**
     * Trata mensagens mal formatadas ou inválidas vindas do servidor:
     * - mostra um popup informando o erro
     * - envia uma mensagem de erro (`operacao: erro_servidor`) de volta ao servidor
     * @param serverResponse a string de resposta recebida (bruta)
     * @param requestJson a requisição que foi enviada originalmente (bruta)
     * @param e a exceção que ocorreu ao processar a resposta
     */
    private void handleMalformedServerMessage(String serverResponse, String requestJson, Exception e) {
        String operacaoEnviada = "desconhecida";
        try {
            JsonNode reqNode = objectMapper.readTree(requestJson);
            operacaoEnviada = reqNode.path("operacao").asText("desconhecida");
        } catch (Exception ex) {
            // ignora: operacaoEnviada fica 'desconhecida'
        }

        String mensagem = e.getMessage() == null ? "Resposta inválida do servidor." : e.getMessage();

        // mostra popup no EDT
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                "Erro ao processar mensagem do servidor: " + mensagem,
                "Erro do servidor",
                JOptionPane.ERROR_MESSAGE));

        // prepara e envia resposta de erro para o servidor
        ObjectNode resposta = objectMapper.createObjectNode();
        resposta.put("operacao", "erro_servidor");
        resposta.put("operacao_enviada", operacaoEnviada);
        resposta.put("info", "Servidor enviou mensagem inválida: " + mensagem);

        appendConsole("OUT> " + resposta.toString());
        try {
            String resp = connection.sendRequest(resposta.toString());
            appendConsole("IN> " + resp);
        } catch (Exception ex) {
            appendConsole("[ERROR] falha ao enviar erro ao servidor: " + ex.getMessage());
        }
    }

    private String formatBasicResponse(JsonNode node) {
        String info = node.path("info").asText("");
        boolean status = node.path("status").asBoolean(false);
        StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(status).append("\n");
        sb.append("Info: ").append(info).append("\n");
        if (node.has("usuario")) {
            JsonNode u = node.get("usuario");
            sb.append("\n--- USUÁRIO ---\n");
            sb.append("Nome: ").append(u.path("nome").asText("-")) .append("\n");
            sb.append("CPF: ").append(u.path("cpf").asText("-")) .append("\n");
            sb.append(String.format("Saldo: R$ %.2f\n", u.path("saldo").asDouble(0.0)));
        }
        return sb.toString();
    }

    // Helpers for HTML formatting
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>")
                .replace("\"", "&quot;");
    }

    private String htmlWrap(String body) {
        // dark background and light text by default; caller can override alignment/styles inside body
        return "<html><body style='font-family:Arial,monospace; font-size:12px; background:#2b2b2b; color:#e6e6e6; margin:12px;'>" + body + "</body></html>";
    }

    private void setDarkUI() {
        UIManager.put("Panel.background", new Color(34,34,34));
        UIManager.put("OptionPane.background", new Color(34,34,34));
        UIManager.put("OptionPane.messageForeground", new Color(230,230,230));
        UIManager.put("Button.background", new Color(60,63,65));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("ToolTip.background", new Color(60,63,65));
        UIManager.put("TextArea.background", new Color(24,24,24));
    }

    private String formatBasicResponseHtml(JsonNode node) {
        String info = escapeHtml(node.path("info").asText(""));
        boolean status = node.path("status").asBoolean(false);
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Resposta</h3>");
        sb.append("<p><strong>Status:</strong> ").append(status).append("</p>");
        sb.append("<p><strong>Info:</strong> ").append(info).append("</p>");
        if (node.has("usuario")) {
            JsonNode u = node.get("usuario");
            sb.append("<h4>Usuário</h4>");
            sb.append("<ul>");
            sb.append("<li><strong>Nome:</strong> ").append(escapeHtml(u.path("nome").asText("-"))).append("</li>");
            sb.append("<li><strong>CPF:</strong> ").append(escapeHtml(u.path("cpf").asText("-"))).append("</li>");
            sb.append("<li><strong>Saldo:</strong> R$ ").append(String.format("%.2f", u.path("saldo").asDouble(0.0))).append("</li>");
            sb.append("</ul>");
        }
        return sb.toString();
    }

    private String formatUsuarioHtml(JsonNode usuario) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='text-align:center;'>");
        sb.append("<h3 style='margin-bottom:6px;'>Dados do Usuário</h3>");
        sb.append("<p style='margin:4px 0;'><strong>Nome:</strong> " ).append(escapeHtml(usuario.path("nome").asText("-"))).append("</p>");
        sb.append("<p style='margin:4px 0;'><strong>CPF:</strong> ").append(escapeHtml(usuario.path("cpf").asText("-"))).append("</p>");
        sb.append("<p style='margin:4px 0;'><strong>Saldo:</strong> R$ ").append(String.format("%.2f", usuario.path("saldo").asDouble(0.0))).append("</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private JFormattedTextField createCpfField() {
        try {
            MaskFormatter mf = new MaskFormatter("###.###.###-##");
            mf.setPlaceholderCharacter('_');
            JFormattedTextField f = new JFormattedTextField(mf);
            f.setColumns(14);
            f.setBackground(new Color(60,63,65));
            f.setForeground(Color.WHITE);
            return f;
        } catch (ParseException ex) {
            JFormattedTextField f = new JFormattedTextField();
            f.setColumns(14);
            return f;
        }
    }

    private void formatAndShowExtrato(JsonNode resp, JsonNode respUsuario) {
        if (!resp.path("status").asBoolean(false)) {
            showHtmlPopup("Extrato - Erro", htmlWrap("<p><strong>Erro:</strong> " + escapeHtml(resp.path("info").asText()) + "</p>"));
            return;
        }
        JsonNode transacoes = resp.path("transacoes");
        StringBuilder sb = new StringBuilder();
        if (transacoes == null || transacoes.size() == 0) {
            sb.append("<p>Não foram encontradas transações para esse período.</p>");
        } else {
            sb.append("<table style='width:100%; border-collapse:collapse;'>");
                sb.append("<tr style='background:#3a3a3a;color:#e6e6e6;'><th style='padding:6px;border:1px solid #4a4a4a;color:#e6e6e6;'>Tipo</th><th style='padding:6px;border:1px solid #4a4a4a;color:#e6e6e6;'>Data</th><th style='padding:6px;border:1px solid #4a4a4a;color:#e6e6e6;'>Valor</th><th style='padding:6px;border:1px solid #4a4a4a;color:#e6e6e6;'>Contra</th></tr>");
            for (JsonNode t : transacoes) {
                String id = t.path("id").asText();
                double valor = t.path("valor_enviado").asDouble();
                JsonNode e = t.path("usuario_enviador");
                JsonNode r = t.path("usuario_recebedor");
                String cpfE = e.path("cpf").asText();
                String cpfR = r.path("cpf").asText();
                String criado = t.path("criado_em").asText();
                if (criado.length() >= 10) criado = criado.substring(0,10);

                String tipo = "";
                if (cpfR.equals(cpfE)) tipo = "DEPOSITO";
                else if (respUsuario.path("usuario").path("cpf").asText().equals(cpfR)) tipo = "RECEBIMENTO";
                else tipo = "ENVIO";

                String tipoColor = "#FFFFFF";
                if (tipo.equals("DEPOSITO")) tipoColor = "#2a7ae2"; // blue
                else if (tipo.equals("ENVIO")) tipoColor = "#e02a2a"; // red
                else if (tipo.equals("RECEBIMENTO")) tipoColor = "#2ae07a"; // green

                String contra = "";
                if (!tipo.equals("DEPOSITO")) {
                    if (tipo.equals("RECEBIMENTO")) {
                        contra = "Origem: " + escapeHtml(e.path("nome").asText()) + " (" + escapeHtml(cpfE) + ")";
                    } else {
                        contra = "Destino: " + escapeHtml(r.path("nome").asText()) + " (" + escapeHtml(cpfR) + ")";
                    }
                } else {
                    contra = "-";
                }

                sb.append("<tr>");
                sb.append("<td style='padding:6px;border:1px solid #ddd;'><strong style='color:").append(tipoColor).append(";'>").append(tipo).append("</strong><br/><small>ID: ").append(escapeHtml(id)).append("</small></td>");
                sb.append("<td style='padding:6px;border:1px solid #ddd;'>").append(escapeHtml(criado)).append("</td>");
                sb.append("<td style='padding:6px;border:1px solid #ddd;'>R$ ").append(String.format("%.2f", valor)).append("</td>");
                sb.append("<td style='padding:6px;border:1px solid #ddd;'>").append(contra).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

        showHtmlPopup("Extrato", htmlWrap(sb.toString()), new Dimension(560, 320));
    }

    /**
     * Exibe um popup modal com conteúdo HTML.
     */
    private void showHtmlPopup(String title, String html) {
        showHtmlPopup(title, html, new Dimension(480, 280));
    }

    private void showHtmlPopup(String title, String html, Dimension size) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(frame, title, true);
            dialog.getContentPane().setBackground(new Color(43,43,43));
            JEditorPane pane = new JEditorPane("text/html", html);
            pane.setEditable(false);
            pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            pane.setBackground(new Color(43,43,43));
            pane.setForeground(new Color(230,230,230));
            pane.setCaretPosition(0);
            JScrollPane scroll = new JScrollPane(pane);
            scroll.getViewport().setBackground(new Color(43,43,43));
            scroll.setPreferredSize(size);
            // dialog decorations
            dialog.getContentPane().add(scroll);
            dialog.pack();
            dialog.setResizable(false);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }

    // ---------- remaining main-like entry ----------
    public static void main(String[] args) {
        String host = "localhost";
        int port = 21212;
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }

        Client client = new Client(host, port);
        client.start();
    }
}