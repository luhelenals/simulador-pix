package common.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.javatuples.Pair;

import java.util.List;

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

    public static String criarResposta(String operacao, boolean status, String info, List<Pair<String, String>> params) {
        ObjectNode resposta = mapper.createObjectNode();
        resposta.put("operacao", operacao);
        resposta.put("status", status);
        resposta.put("info", info);
        for (Pair param : params) {
            resposta.put(param.getValue0().toString(), param.getValue1().toString());
        }

        return resposta.toString();
    }

    public static String criarRespostaErro() {
        ObjectNode resposta = mapper.createObjectNode();
        resposta.put("operacao", "dcsdcs");
        resposta.put("status", true);

        return resposta.toString();
    }
}
