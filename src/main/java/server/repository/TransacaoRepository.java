package server.repository;

import common.models.Transacao;
import java.sql.*;
import java.time.format.DateTimeFormatter;

/**
 * Responsável por todas as operações de banco de dados relacionadas à entidade Transacao.
 */
public class TransacaoRepository {

    // Define um formato padrão para salvar e ler datas do banco de dados (ISO-8601)
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Salva uma nova transação no banco de dados.
     * @param transacao O objeto Transacao a ser salvo.
     */
    public void save(Transacao transacao) {
        String sql = "INSERT INTO transacoes(cpf_remetente, cpf_destinatario, valor, data_transacao) VALUES(?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, transacao.getRemetente().getCpf());
            pstmt.setString(2, transacao.getDestinatario().getCpf());
            pstmt.setDouble(3, transacao.getValor());
            // Converte o objeto LocalDateTime para uma String antes de salvar no banco
            pstmt.setString(4, transacao.getDataTransacao().format(formatter));

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao salvar nova transação: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Futuramente, você poderia adicionar um método aqui para buscar o extrato.
    /*
    public List<Transacao> findByCpf(String cpf) {
        // Lógica para buscar todas as transações (enviadas e recebidas) de um usuário
    }
    */
}