package server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.models.Transacao;
import common.models.Usuario;
import server.repository.TransacaoRepository;
import server.repository.UsuarioRepository;
import common.util.SessaoManager;

import java.util.Optional;

import static common.util.RespostaManager.criarResposta;

public class TransacaoController {

    private static final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private static final TransacaoRepository transacaoRepository = new TransacaoRepository();

    public TransacaoController() {}

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

    // NOTA: O método para LER transações (extrato) seria implementado aqui.
    // Ele receberia as datas, buscaria no transacaoRepository e retornaria uma lista.

}