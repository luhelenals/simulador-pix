package server.repository;

import common.models.Transacao;
import common.models.Usuario;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Responsável por todas as operações de banco de dados relacionadas à entidade Transacao.
 */
public class TransacaoRepository {

    // Define um formato padrão para salvar e ler datas do banco de dados (ISO-8601)
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final UsuarioRepository usuarioRepository = new UsuarioRepository();

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

    // Buscar o extrato
    public List<Transacao> findByCpf(String cpf) {
        String sql = "SELECT * FROM transacoes WHERE cpf_remetente = ? OR cpf_destinatario = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            System.out.println("[REPOSITORY] Entrou try");
            pstmt.setString(1, cpf);
            pstmt.setString(2, cpf);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("[REPOSITORY] Checkpoint #1");

            List<Transacao> transacoes = new ArrayList<>();

            while (rs.next()) {
                System.out.println("[REPOSITORY] Checkpoint #2");

                double valor = rs.getDouble("valor");

                System.out.println("[REPOSITORY] rs.getString(\"data_transacao\"): " + rs.getString("data_transacao"));

                String dataComoString = rs.getString("data_transacao");
                LocalDateTime data = LocalDateTime.parse(dataComoString);

                String destino = rs.getString("cpf_destinatario");
                String remetente = rs.getString("cpf_remetente");

                Transacao transacao = new Transacao();
                transacao.setDataTransacao(data);

                Optional<Usuario> userDestinatario = usuarioRepository.findByCpf(destino);
                if(userDestinatario.isEmpty()) transacao.setDestinatario(new Usuario());
                else transacao.setDestinatario(userDestinatario.get());

                Optional<Usuario> userRemetente = usuarioRepository.findByCpf(remetente);
                if(userRemetente.isEmpty()) transacao.setRemetente(new Usuario());
                else transacao.setRemetente(userRemetente.get());

                transacao.setValor(valor);

                transacoes.add(transacao);
            }

            return transacoes;

        } catch (SQLException e) {
            // Mensagem de erro melhorada para este contexto
            System.err.println("Erro ao buscar transações por CPF: " + e.getMessage());
            e.printStackTrace();
        }

        // Retorna uma lista vazia se o 'try' falhar
        return new ArrayList<>();
    }
}