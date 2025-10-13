package server.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import common.models.Usuario;
import server.repository.UsuarioRepository;
import common.util.SessaoManager;
import java.util.Optional;
import static common.util.RespostaManager.*;

public class UsuarioController {

    private static final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Para construir respostas JSON

    public UsuarioController() {}

    /**
     * Processa a criação de um novo usuário.
     */
    public static String criarUsuario(JsonNode dados) {
        String nome = dados.get("nome").asText();
        String cpf = dados.get("cpf").asText();
        String senha = dados.get("senha").asText();
        double saldo = dados.get("saldo").asDouble();

        // Verifica se o usuário já existe
        if (usuarioRepository.findByCpf(cpf).isPresent()) {
            return criarResposta(dados.get("operacao").asText(), false, "CPF já cadastrado.");
        }

        Usuario novoUsuario = new Usuario(nome, cpf, senha, saldo);
        System.out.println("[CONTROLLER] Tentando criar usuário com CPF: " + novoUsuario.getCpf());
        usuarioRepository.save(novoUsuario);

        return criarResposta(dados.get("operacao").asText(), true, "Usuário criado com sucesso.");
    }

    /**
     * Processa o login de um usuário.
     */
    public static String login(JsonNode dados) {
        String cpf = dados.get("cpf").asText();
        String senha = dados.get("senha").asText();

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cpf);

        if (usuarioOpt.isEmpty() || !usuarioOpt.get().getSenha().equals(senha)) {
            return criarResposta(dados.get("operacao").asText(), false, "CPF ou senha inválidos.");
        }

        String token = SessaoManager.criarSessao(cpf);

        return criarResposta(dados.get("operacao").asText(), false, "Login bem-sucedido.", "token", token);
    }

    /**
     * Processa o logout de um usuário.
     */
    public static String logout(JsonNode dados) {
        String token = dados.get("token").asText();
        SessaoManager.encerrarSessao(token);
        return criarResposta(dados.get("operacao").asText(), true, "Logout realizado com sucesso.");
    }

    /**
     * Processa a leitura dos dados de um usuário logado.
     */
    public static String lerUsuario(JsonNode dados) {
        String token = dados.get("token").asText();
        String cpf = SessaoManager.getCpfPeloToken(token);

        if (cpf == null) {
            return criarResposta(dados.get("operacao").asText(), false, "Token inválido ou sessão expirada.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cpf);
        if (usuarioOpt.isEmpty()) {
            return criarResposta(dados.get("operacao").asText(), false, "Usuário não encontrado.");
        }

        Usuario usuario = usuarioOpt.get();
        ObjectNode usuarioNode = objectMapper.createObjectNode();
        usuarioNode.put("nome", usuario.getNome());
        usuarioNode.put("cpf", usuario.getCpf());
        usuarioNode.put("saldo", usuario.getSaldo());

        ObjectNode resposta = objectMapper.createObjectNode();
        resposta.put("operacao", dados.get("operacao").asText());
        resposta.put("status", true);
        resposta.put("info", "Dados do usuário recuperados com sucesso.");
        resposta.set("usuario", usuarioNode);

        return resposta.toString();
    }

    /**
     * Processa a operação de depósito na conta do usuário.
     */
    public String depositar(JsonNode dados) {
        String token = dados.get("token").asText();
        double valor = dados.get("valor_enviado").asDouble();
        String cpf = SessaoManager.getCpfPeloToken(token);

        if (cpf == null) {
            return criarResposta(dados.get("operacao").asText(), false, "Token inválido ou sessão expirada.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cpf);
        if (usuarioOpt.isEmpty()) {
            return criarResposta(dados.get("operacao").asText(), false, "Usuário não encontrado.");
        }

        Usuario usuario = usuarioOpt.get();
        usuario.depositar(valor);
        usuarioRepository.update(usuario);

        return criarResposta(dados.get("operacao").asText(), true, "Depósito realizado com sucesso.");
    }

    /**
     * Processa a operação de atualizar um usuário.
     */
    public static String updateUsuario(JsonNode dados) {
        String nome = dados.get("nome").asText();
        String cpf = dados.get("cpf").asText();
        String senha = dados.get("senha").asText();

        // Verifica se o usuário existe
        Optional<Usuario> usuario = usuarioRepository.findByCpf(cpf);
        if (usuario.isEmpty()) {
            return criarResposta(dados.get("operacao").asText(), false, "CPF não encontrado.");
        }

        System.out.println("[CONTROLLER] Tentando atualizar usuário com CPF: " + cpf);
        Usuario updatedUsuario = new Usuario(nome, cpf, senha, usuario.get().getSaldo());
        usuarioRepository.update(updatedUsuario);

        return criarResposta(dados.get("operacao").asText(), true, "Usuário criado com sucesso.");
    }

    /**
     * Processa a operação de deletar um usuário.
     */
    public static String deleteUsuario(JsonNode dados) {
        String cpf = dados.get("cpf").asText();

        // Verifica se o usuário existe
        Optional<Usuario> usuario = usuarioRepository.findByCpf(cpf);
        if (usuario.isEmpty()) {
            return criarResposta(dados.get("operacao").asText(), false, "CPF não encontrado.");
        }

        usuarioRepository.delete(cpf);

        return criarResposta(dados.get("operacao").asText(), true, "Usuário deletado com sucesso.");
    }
}