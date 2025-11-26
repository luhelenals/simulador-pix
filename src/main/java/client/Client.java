package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.InputMismatchException;
import java.util.Scanner;

import static common.util.RespostaManager.criarResposta;

public class Client {

    private final Connection connection;
    private final Scanner scanner;
    private final ObjectMapper objectMapper;
    private String token; // Armazena o token de sessão do usuário logado

    public Client(String host, int port) {
        this.connection = new Connection(host, port);
        this.scanner = new Scanner(System.in);
        this.objectMapper = new ObjectMapper();
        this.token = null;
    }

    public void start() {
        if (!connection.connect()) {
            return; // Encerra se não conseguir conectar
        }

        boolean running = true;
        while (running) {
            if (token == null) {
                showMenuDeslogado();
            } else {
                showMenuLogado();
            }

            int escolha = scanner.nextInt();
            scanner.nextLine(); // Consome a nova linha

            if (token == null) {
                running = handleEscolhaDeslogado(escolha);
            } else {
                running = handleEscolhaLogado(escolha);
            }
        }
        connection.disconnect();
    }

    // --- MENUS ---
    private void showMenuDeslogado() {
        System.out.println("\n--- BANCO DISTRIBUÍDO ---");
        System.out.println("1. Criar Conta");
        System.out.println("2. Login");
        System.out.println("3. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private void showMenuLogado() {
        System.out.println("\n--- ÁREA DO CLIENTE ---");
        System.out.println("1. Ver Saldo e Dados");
        System.out.println("2. Depositar");
        System.out.println("3. Fazer PIX (Transferência)");
        System.out.println("4. Ver extrato");
        System.out.println("5. Logout");
        System.out.println("6. Editar conta");
        System.out.println("7. Deletar conta");
        System.out.print("Escolha uma opção: ");
    }

    // --- LÓGICA DE ESCOLHA ---
    private boolean handleEscolhaDeslogado(int escolha) {
        switch (escolha) {
            case 1: criarConta(); break;
            case 2: login(); break;
            case 3: return false; // Sair do loop
            default: System.out.println("Opção inválida.");
        }
        return true;
    }

    private boolean handleEscolhaLogado(int escolha) {
        switch (escolha) {
            case 1: verSaldo(); break;
            case 2: depositar(); break;
            case 3: fazerPix(); break;
            case 4: verExtrato(); break;
            case 5: logout(); break;
            case 6: update(); break;
            case 7: delete(); break;
            default: System.out.println("Opção inválida.");
        }
        return true;
    }

    // --- MÉTODOS DE OPERAÇÃO ---
    private void criarConta() {
        System.out.print("Nome completo: ");
        String nome = scanner.nextLine();
        System.out.print("CPF (formato 000.000.000-00): ");
        String cpf = scanner.nextLine();
        System.out.print("Senha (mínimo 6 caracteres): ");
        String senha = scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_criar");
        request.put("nome", nome);
        request.put("cpf", cpf);
        request.put("senha", senha);

        processarResposta(request.toString());
    }

    private void login() {
        System.out.print("CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_login");
        request.put("cpf", cpf);
        request.put("senha", senha);

        String responseJson = connection.sendRequest(request.toString());
        System.out.println("Resposta do servidor: " + responseJson);
        if (responseJson != null) {
            try {
                JsonNode response = objectMapper.readTree(responseJson);
                System.out.println("Servidor: " + response.get("info").asText());
                if (response.get("status").asBoolean()) {
                    this.token = response.get("token").asText(); // Armazena o token
                    System.out.println("Token recebido do servidor: " + token);
                }
                else {
                    System.out.println("Erro ao realizar o login: " + response.get("info").asText());
                }
            } catch (JsonProcessingException e) {
                System.err.println("Erro: " + e.getMessage());

                ObjectNode resposta = objectMapper.createObjectNode();
                resposta.put("operacao", "erro_servidor");
                resposta.put("operacao_enviada", "usuario_login");
                resposta.put("info", e.getMessage());
            }
        }
    }

    private void logout() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_logout");
        request.put("token", this.token);

        processarResposta(request.toString());
        this.token = null; // Limpa o token localmente
    }

    private void verSaldo() {
        String responseJson = obterDadosUsuario();

        if (responseJson != null) {
            try {
                JsonNode response = objectMapper.readTree(responseJson);
                System.out.println("Servidor: " + response.get("info").asText());
                if (response.get("status").asBoolean()) {
                    JsonNode usuario = response.get("usuario");
                    System.out.println("\nDADOS DO USUARIO");
                    System.out.println("  Nome: " + usuario.get("nome").asText());
                    System.out.println("  CPF: " + usuario.get("cpf").asText());
                    System.out.printf("  Saldo: R$ %.2f\n", usuario.get("saldo").asDouble());
                }
            } catch (JsonProcessingException e) {
                System.err.println("Erro: " + e.getMessage());

                System.err.println("Erro: " + e.getMessage());

                ObjectNode resposta = objectMapper.createObjectNode();
                resposta.put("operacao", "erro_servidor");
                resposta.put("operacao_enviada", "usuario_login");
                resposta.put("info", e.getMessage());
            }
        }
    }

    private String obterDadosUsuario() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_ler");
        request.put("token", this.token);

        return connection.sendRequest(request.toString());
    }

    private void delete() {
        System.out.print("Deletando este usuário");

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_deletar");
        request.put("token", this.token);

        processarResposta(request.toString());
        logout();
    }

    private void update() {
        System.out.print("Novo nome (deixar em branco para manter): ");
        String nome = scanner.nextLine();
        System.out.print("Nova senha (deixar em branco para manter): ");
        String senha = scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_atualizar");
        request.put("token", this.token);

        ObjectNode node = objectMapper.createObjectNode();
        if(!nome.isEmpty()) node.put("nome", nome);
        if(!senha.isEmpty()) node.put("senha", senha);

        request.put("usuario", node);

        processarResposta(request.toString());
    }

    private void depositar() {
        System.out.print("Valor a depositar: ");
        try {
            double valor = scanner.nextDouble();
            scanner.nextLine();

            ObjectNode request = objectMapper.createObjectNode();
            request.put("operacao", "depositar");
            request.put("token", this.token);
            request.put("valor_enviado", valor);

            processarResposta(request.toString());

        } catch (InputMismatchException e) {
            System.err.println("Erro: digite um valor numérico válido (casas decimais com vírgula).");
            scanner.nextLine();

        } catch (Exception e) {
            System.err.println("Erro inesperado ao ler a entrada: " + e.getMessage());
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
        }
    }

    private void fazerPix() {
        System.out.print("CPF de destino: ");
        String cpfDestino = scanner.nextLine();
        System.out.print("Valor a transferir: ");
        double valor = scanner.nextDouble();
        scanner.nextLine();

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "transacao_criar");
        request.put("token", this.token);
        request.put("cpf_destino", cpfDestino);
        request.put("valor", valor);

        processarResposta(request.toString());
    }

    private void verExtrato() {
        System.out.print("Data inicial (yyyy-mm-dd): ");
        String dataInicialInput = scanner.nextLine().strip();
        String dataInicial = dataInicialInput + "T00:00:00Z";

        System.out.print("Data final (yyyy-mm-dd, deixar em branco para hoje): ");
        String dataFinalInput = scanner.nextLine().strip();

        String dataFinal;

        if (dataFinalInput.isBlank()) {
            dataFinal = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        } else {
            dataFinal = dataFinalInput + "T23:59:59Z";
        }

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "transacao_ler");
        request.put("token", this.token);
        request.put("data_inicial", dataInicial);
        request.put("data_final", dataFinal);

        String dadosUsuarioJson = obterDadosUsuario();
        String responseJson = connection.sendRequest(request.toString());

        if (responseJson != null && dadosUsuarioJson != null) {
            try {
                JsonNode response = objectMapper.readTree(responseJson);
                System.out.println("Servidor: " + response.get("info").asText());

                JsonNode responseUsuario = objectMapper.readTree(dadosUsuarioJson);
                JsonNode usuario = responseUsuario.get("usuario");
                String cpfUsuario = usuario.get("cpf").asText();

                if (response.get("status").asBoolean()) {
                    JsonNode transacoesNode = response.get("transacoes");
                    if (transacoesNode.isEmpty())
                        System.out.println("Não foram encontradas transações para esse período.");
                    else {
                        for (JsonNode transacao : transacoesNode) {
                            String id = transacao.get("id").asText();
                            double valor = transacao.get("valor_enviado").asDouble();

                            JsonNode enviadorNode = transacao.get("usuario_enviador");
                            String nome_enviador = enviadorNode.get("nome").asText();
                            String cpf_enviador = enviadorNode.get("cpf").asText();

                            JsonNode recebedorNode = transacao.get("usuario_recebedor");
                            String nome_recebedor = recebedorNode.get("nome").asText();
                            String cpf_recebedor = recebedorNode.get("cpf").asText();

                            String criado = transacao.get("criado_em").asText().substring(0, 10);

                            enum TipoTransacao {
                                RECEBIMENTO,
                                ENVIO,
                                DEPOSITO
                            };

                            TipoTransacao tipo = null;
                            if (cpf_recebedor.equals(cpf_enviador)) tipo = TipoTransacao.DEPOSITO;
                            else if (cpf_recebedor.equals(cpfUsuario)) tipo = TipoTransacao.RECEBIMENTO;
                            else tipo = TipoTransacao.ENVIO;

                            String mensagem =
                                    "\n==========================================\n" +
                                    tipo.name() +
                                    "\nID: " + id +
                                    "\nData: " + criado +
                                    "\nValor: R$" +  valor;

                            if (tipo.equals(TipoTransacao.DEPOSITO)) {
                                mensagem += "\n==========================================\n";
                            }
                            else {
                                mensagem +=
                                        (tipo.equals(TipoTransacao.RECEBIMENTO) ? "\nOrigem: " : "\nDestino: ") +
                                        "\n  Nome: " + (tipo.equals(TipoTransacao.RECEBIMENTO) ? nome_enviador : nome_recebedor) +
                                        "\n  CPF: " + (tipo.equals(TipoTransacao.RECEBIMENTO) ? cpf_enviador : cpf_recebedor) +
                                        "\n==========================================\n";
                            }

                            System.out.print(mensagem);
                        }
                    }
                }
                else {
                    System.out.println("Erro: " + response.get("info").asText());
                }
            } catch (Exception e) {
                System.err.println("Erro: " + e.getMessage());
            }
        }
        else {
            ObjectNode resposta = objectMapper.createObjectNode();
            resposta.put("operacao", "erro_servidor");
            resposta.put("operacao_enviada", "transacao_ler");
            resposta.put("info", "Servidor enviou mensagem nula.");
        }
    }

    /**
     * Método genérico para enviar uma requisição e imprimir a resposta do servidor.
     */
    private void processarResposta(String requestJson) {
        String responseJson = connection.sendRequest(requestJson);

        if (responseJson != null) {
            try {
                JsonNode response = objectMapper.readTree(responseJson);
                System.out.println("Servidor: " + response.get("info").asText() + "\n");

            } catch (JsonProcessingException e) {

                System.err.println("Erro: A resposta do servidor não é um JSON válido: " + e.getMessage());

                try {
                    JsonNode rootNode = objectMapper.readTree(requestJson);
                    String operacao = rootNode.path("operacao").asText();

                    ObjectNode resposta = objectMapper.createObjectNode();
                    resposta.put("operacao", "erro_servidor");
                    resposta.put("operacao_enviada", operacao);
                    resposta.put("info", "Falha ao processar resposta do servidor: " + e.getMessage());

                    System.err.println("Erro formatado: " + resposta.toString());

                } catch (JsonProcessingException e2) {
                    System.err.println("Falha CRÍTICA: A resposta do servidor E a requisição original são JSONs inválidos.");
                    System.err.println("Erro da Resposta: " + e.getMessage());
                    System.err.println("Erro da Requisição: " + e2.getMessage());
                }
            }
        } else {
            System.err.println("Erro: O servidor não respondeu à requisição.");
        }
    }

    public static void main(String[] args) {
        // Conecta ao servidor rodando na mesma máquina (localhost) na porta 9000
        String host = "localhost";
        int port = 9000;

        Scanner scanner = new Scanner(System.in);

        System.out.println("IP do servidor: ");
        host = scanner.nextLine();
        System.out.println("Porta do servidor: ");
        port = scanner.nextInt();

        Client client = new Client(host, port);
        client.start();
    }
}