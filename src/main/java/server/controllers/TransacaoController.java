package server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import common.models.Transacao;
import common.models.Usuario;
import server.repository.TransacaoRepository;
import server.repository.UsuarioRepository;
import common.util.SessaoManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static common.util.RespostaManager.criarResposta;

public class TransacaoController {

    private static final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private static final TransacaoRepository transacaoRepository = new TransacaoRepository();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public TransacaoController() {}

    /**
     * Obtem transações de um usuário
     */
    public static String getTransacoes(JsonNode dados) {
        try {
            String token = dados.get("token").asText();
            String cpf = SessaoManager.getCpfPeloToken(token);

            if (cpf == null) {
                return criarResposta(dados.get("operacao").asText(), false, "Token inválido ou sessão expirada.");
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cpf);
            if (usuarioOpt.isEmpty()) {
                return criarResposta(dados.get("operacao").asText(), false, "Usuário não encontrado.");
            }

            List<Transacao> transacoesEncontradas = transacaoRepository.findByCpf(cpf);
            ArrayNode transacoesArrayNode = objectMapper.createArrayNode();

            // --- LÓGICA DE FILTRO CORRIGIDA (MOVEMOS PARA FORA DO LOOP) ---
            LocalDateTime dataInicioFiltro = null;
            LocalDateTime dataFimFiltro = null;
            boolean aplicarFiltro = false;

            // 1. Verificamos se as datas foram enviadas
            if (dados.has("data_inicial") && dados.has("data_final") &&
                    !dados.get("data_inicial").asText().isEmpty() &&
                    !dados.get("data_final").asText().isEmpty()) {

                try {
                    // 2. Pegamos as strings (ex: "2025-11-01T00:00:00Z")
                    String dataInicio = dados.get("data_inicial").asText();
                    String dataFim = dados.get("data_final").asText();

                    // 3. (CORREÇÃO DO FUSO) Convertemos o Instant para LocalDateTime EM UTC
                    dataInicioFiltro = LocalDateTime.ofInstant(Instant.parse(dataInicio), ZoneId.of("UTC"));
                    dataFimFiltro = LocalDateTime.ofInstant(Instant.parse(dataFim), ZoneId.of("UTC"));

                    aplicarFiltro = true;
                    System.out.println("[CONTROLLER] Filtro de data UTC ativado de " + dataInicioFiltro + " até " + dataFimFiltro);

                } catch (Exception e) {
                    System.err.println("Erro ao parsear datas de filtro (formato ISO 8601 com 'Z' esperado): " + e.getMessage());
                    // Não aplicamos o filtro se as datas forem inválidas
                }
            }
            // --- FIM DA LÓGICA DE FILTRO ---


            for (Transacao transacao : transacoesEncontradas) {
                LocalDateTime dataOriginal = transacao.getDataTransacao();

                if (dataOriginal == null) {
                    continue; // Pula transações sem data
                }

                boolean estaNoPeriodo = false;

                // 4. Se o filtro estiver ativo, verificamos
                if (aplicarFiltro) {
                    // 5. (CORREÇÃO DA LÓGICA) Usamos "Não é antes" (>=) e "Não é depois" (<=)
                    if (!dataOriginal.isBefore(dataInicioFiltro) && !dataOriginal.isAfter(dataFimFiltro)) {
                        estaNoPeriodo = true;
                    }
                }

                // 6. Só adiciona no JSON se (NÃO for pra filtrar) OU (for pra filtrar E ESTIVER no período)
                if (!aplicarFiltro || estaNoPeriodo) {

                    ObjectNode transacaoNode = objectMapper.createObjectNode();

                    // Formatar data da transação
                    String dataFormatadaUTC = dataOriginal.toInstant(ZoneOffset.UTC).toString();
                    transacaoNode.put("data_transacao", dataFormatadaUTC);
                    transacaoNode.put("criado_em", dataFormatadaUTC);
                    transacaoNode.put("atualizado_em", dataFormatadaUTC);

                    // Criar nós de usuário
                    ObjectNode usuarioEnviadorNode = objectMapper.createObjectNode();
                    ObjectNode usuarioRecebedorNode = objectMapper.createObjectNode();

                    usuarioEnviadorNode.put("cpf", transacao.getRemetente().getCpf());
                    usuarioEnviadorNode.put("nome", transacao.getRemetente().getNome());

                    usuarioRecebedorNode.put("cpf", transacao.getDestinatario().getCpf());
                    usuarioRecebedorNode.put("nome", transacao.getDestinatario().getNome());

                    transacaoNode.put("valor_enviado", transacao.getValor());
                    transacaoNode.put("id", transacao.getId());
                    transacaoNode.set("usuario_enviador", usuarioEnviadorNode);
                    transacaoNode.set("usuario_recebedor", usuarioRecebedorNode);

                    transacoesArrayNode.add(transacaoNode);
                }
            }

            System.out.println("[CONTROLLER] Transações encontradas: " + transacoesArrayNode.toString());

            ObjectNode resposta = objectMapper.createObjectNode();
            resposta.put("operacao", dados.get("operacao").asText());
            resposta.put("status", true);
            resposta.put("info", "Transações do usuário recuperados com sucesso.");
            resposta.set("transacoes", transacoesArrayNode);

            return resposta.toString();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(); // É bom ter o stack trace completo
            ObjectNode resposta = objectMapper.createObjectNode();
            resposta.put("operacao", dados.get("operacao").asText());
            resposta.put("status", false);
            resposta.put("info", "Erro ao recuperar transações do usuário.");

            return resposta.toString();
        }
    }

    /**
     * Processa a criação de uma nova transação (PIX).
     */
    public static String criarTransacao(JsonNode dados) {
        String token = dados.get("token").asText();
        String cpfDestino = dados.get("cpf_destino").asText();
        double valor = dados.get("valor").asDouble();

        String cpfRemetente = SessaoManager.getCpfPeloToken(token);
        if (cpfRemetente == null) {
            return criarResposta(dados.get("operacao").asText(), false, "Token inválido ou sessão expirada.");
        }

        if (cpfRemetente.equals(cpfDestino)) {
            return criarResposta(dados.get("operacao").asText(), false, "Não é possível enviar dinheiro para si mesmo.");
        }

        Optional<Usuario> remetenteOpt = usuarioRepository.findByCpf(cpfRemetente);
        Optional<Usuario> destinatarioOpt = usuarioRepository.findByCpf(cpfDestino);

        if (remetenteOpt.isEmpty()) {
            return criarResposta(dados.get("operacao").asText(), false, "Usuário remetente não encontrado.");
        }
        if (destinatarioOpt.isEmpty()) {
            return criarResposta(dados.get("operacao").asText(), false, "Usuário de destino não encontrado.");
        }

        Usuario remetente = remetenteOpt.get();
        if (remetente.getSaldo() < valor) {
            return criarResposta(dados.get("operacao").asText(), false, "Saldo insuficiente.");
        }

        Usuario destinatario = destinatarioOpt.get();

        remetente.sacar(valor);
        destinatario.depositar(valor);

        usuarioRepository.update(remetente);
        usuarioRepository.update(destinatario);

        Transacao novaTransacao = new Transacao(remetente, destinatario, valor);
        transacaoRepository.save(novaTransacao);

        return criarResposta(dados.get("operacao").asText(), true, "Transação realizada com sucesso.");
    }

}