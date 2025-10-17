package server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import common.models.Transacao;
import common.models.Usuario;
import server.repository.TransacaoRepository;
import server.repository.UsuarioRepository;
import common.util.SessaoManager;
import java.util.List;
import java.util.Optional;
import static common.util.RespostaManager.criarResposta;

public class TransacaoController {

    private static final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private static final TransacaoRepository transacaoRepository = new TransacaoRepository();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public TransacaoController() {}

    /**
     * Obtem transações de um usuário
     */
    public static String getTransacoes(JsonNode dados) {
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

        ArrayNode transacoesArrayNode = objectMapper.valueToTree(transacoesEncontradas);
        ObjectNode resposta = objectMapper.createObjectNode();

        resposta.put("operacao", dados.get("operacao").asText());
        resposta.put("status", true);
        resposta.put("info", "Dados do usuário recuperados com sucesso.");
        resposta.set("transacoes", transacoesArrayNode);

        return resposta.toString();
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

        // ATENÇÃO: Em um sistema real, as 4 operações abaixo (sacar, depositar, 2 updates)
        // deveriam estar dentro de uma única transação de banco de dados para garantir a consistência.

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