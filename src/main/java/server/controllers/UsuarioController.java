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
        try {
            String nome = dados.get("nome").asText();
            String cpf = dados.get("cpf").asText();
            String senha = dados.get("senha").asText();

            // Verifica se o usuário já existe
            if (usuarioRepository.findByCpf(cpf).isPresent()) {
                return criarResposta(dados.get("operacao").asText(), false, "CPF já cadastrado.");
            }

            Usuario novoUsuario = new Usuario(nome, cpf, senha, 0); // Inicializa usuário novo com saldo 0
            System.out.println("[CONTROLLER] Tentando criar usuário com CPF: " + novoUsuario.getCpf());

            usuarioRepository.save(novoUsuario);

            return criarResposta(dados.get("operacao").asText(), true, "Usuário criado com sucesso.");
        }
        catch (Exception e) {
            return criarResposta(dados.get("operacao").asText(), false, "Erro ao cadastrar o usuário.");
        }
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

        return criarResposta(dados.get("operacao").asText(), true, "Login bem-sucedido.", "token", token);
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
    public static String depositar(JsonNode dados) {
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
     * Espera um JSON com "token" no nível principal e um objeto "usuario" aninhado com os campos a serem alterados.
     */
    public static String updateUsuario(JsonNode dados) {
        try {
            // CORREÇÃO: Ler o token do objeto principal 'dados'
            String token = dados.get("token").asText();
            JsonNode usuarioNode = dados.get("usuario"); // O objeto com os novos dados (nome e/ou senha)

            String cpf = SessaoManager.getCpfPeloToken(token);
            if (cpf == null) {
                return criarResposta("usuario_atualizar", false, "Token inválido ou sessão expirada.");
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cpf);
            if (usuarioOpt.isEmpty()) {
                return criarResposta("usuario_atualizar", false, "Usuário não encontrado.");
            }

            Usuario usuarioParaAtualizar = usuarioOpt.get(); // Pega o objeto de usuário existente do banco

            // MELHORIA: Atualiza apenas os campos que foram fornecidos no JSON
            boolean foiAtualizado = false;
            if (usuarioNode.has("nome")) {
                usuarioParaAtualizar.setNome(usuarioNode.get("nome").asText());
                foiAtualizado = true;
            }
            if (usuarioNode.has("senha")) {
                usuarioParaAtualizar.setSenha(usuarioNode.get("senha").asText());
                foiAtualizado = true;
            }

            if (!foiAtualizado) {
                return criarResposta("usuario_atualizar", false, "Nenhum dado fornecido para atualização.");
            }

            System.out.println("[CONTROLLER] Atualizando usuário com CPF: " + cpf);
            usuarioRepository.update(usuarioParaAtualizar); // Envia o objeto modificado para o repositório

            return criarResposta("usuario_atualizar", true, "Usuário atualizado com sucesso.");

        } catch (Exception e) {
            System.err.println("[CONTROLLER] Erro ao atualizar usuário: " + e.getMessage());
            e.printStackTrace();
            return criarResposta("usuario_atualizar", false, "Erro interno ao atualizar o usuário.");
        }
    }
    
    /**
     * Processa a operação de deletar um usuário.
     */
    public static String deleteUsuario(JsonNode dados) {
        String token = dados.get("token").asText();

        String cpf = SessaoManager.getCpfPeloToken(token);
        System.out.println("[CONTROLLER] Tentando deletar usuário com CPF " + cpf);

        if (cpf == null) {
            return criarResposta(dados.get("operacao").asText(), false, "Token inválido ou sessão expirada.");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCpf(cpf);
        if (usuarioOpt.isEmpty()) {
            return criarResposta(dados.get("operacao").asText(), false, "Usuário não encontrado.");
        }

        usuarioRepository.delete(usuarioOpt.get().getCpf());

        return criarResposta(dados.get("operacao").asText(), true, "Usuário deletado com sucesso.");
    }
}