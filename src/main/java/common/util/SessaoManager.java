package common.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia as sessões ativas dos usuários.
 * Associa um token único a um CPF de usuário logado.
 */
public class SessaoManager {

    // Usamos ConcurrentHashMap por ser seguro para uso em ambientes com múltiplas Threads
    private static final Map<String, String> sessoesAtivas = new ConcurrentHashMap<>();

    /**
     * Cria uma nova sessão para um usuário, gerando um token aleatório.
     * @param cpf O CPF do usuário que está logando.
     * @return O token de sessão gerado.
     */
    public static String criarSessao(String cpf) {
        String token = UUID.randomUUID().toString();
        sessoesAtivas.put(token, cpf);
        System.out.println("Sessão criada para o CPF: " + cpf + " com o token: " + token);
        return token;
    }

    /**
     * Busca o CPF associado a um token de sessão.
     * @param token O token a ser validado.
     * @return O CPF do usuário, ou null se o token for inválido.
     */
    public static String getCpfPeloToken(String token) {
        return sessoesAtivas.get(token);
    }

    /**
     * Encerra uma sessão, removendo o token.
     * @param token O token da sessão a ser encerrada.
     */
    public static void encerrarSessao(String token) {
        if (token != null) {
            String cpf = sessoesAtivas.remove(token);
            if (cpf != null) {
                System.out.println("Sessão encerrada para o CPF: " + cpf);
            }
        }
    }
}