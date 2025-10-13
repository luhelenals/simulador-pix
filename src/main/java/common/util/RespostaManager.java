package common.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RespostaManager {
    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * Método auxiliar para criar respostas JSON padronizadas.
     */
    public static String criarResposta(String operacao, boolean status, String info) {
        ObjectNode resposta = mapper.createObjectNode();
        resposta.put("operacao", operacao);
        resposta.put("status", status);
        resposta.put("info", info);
        return resposta.toString();
    }

    public static String criarResposta(String operacao, boolean status, String info, String paramName, String paramValue) {
        ObjectNode resposta = mapper.createObjectNode();
        resposta.put("operacao", operacao);
        resposta.put("status", status);
        resposta.put("info", info);
        resposta.put(paramName, paramValue);
        return resposta.toString();
    }
}
