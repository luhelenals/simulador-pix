package server.repository;

import common.models.Usuario;
import java.sql.*;
import java.util.Optional;

/**
 * Responsável por todas as operações de banco de dados relacionadas à entidade Usuario.
 */
public class UsuarioRepository {

    /**
     * Busca um usuário no banco de dados pelo seu CPF.
     * @param cpf O CPF do usuário a ser procurado.
     * @return um Optional contendo o Usuario se encontrado, ou um Optional vazio caso contrário.
     */
    public Optional<Usuario> findByCpf(String cpf) {
        String sql = "SELECT * FROM usuarios WHERE cpf = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cpf);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Usuario usuario = new Usuario(
                        rs.getString("nome"),
                        rs.getString("cpf"),
                        rs.getString("senha"),
                        rs.getDouble("saldo")
                );
                usuario.setSaldo(rs.getDouble("saldo"));
                return Optional.of(usuario);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário por CPF: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Salva um novo usuário no banco de dados.
     * @param usuario O objeto Usuario a ser salvo.
     */
    public void save(Usuario usuario) {
        String sql = "INSERT INTO usuarios(cpf, nome, senha, saldo) VALUES(?, ?, ?, ?)";

        System.out.println("[REPOSITORY] Tentando salvar o CPF " + usuario.getCpf() + " no banco de dados.");

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getCpf());
            pstmt.setString(2, usuario.getNome());
            pstmt.setString(3, usuario.getSenha());
            pstmt.setDouble(4, usuario.getSaldo());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao salvar novo usuário: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Atualiza os dados de um usuário existente no banco de dados.
     * @param usuario O objeto Usuario com os dados atualizados.
     */
    public void update(Usuario usuario) {
        String sql = "UPDATE usuarios SET nome = ?, senha = ?, saldo = ? WHERE cpf = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getNome());
            pstmt.setString(2, usuario.getSenha());
            pstmt.setDouble(3, usuario.getSaldo());
            pstmt.setString(4, usuario.getCpf());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar usuário: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deleta um usuário do banco de dados pelo CPF.
     * @param cpf O CPF do usuário a ser deletado do banco.
     * */
    public void delete(String cpf) {
        String sql = "DELETE FROM usuarios WHERE cpf = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cpf);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar usuário: " + e.getMessage());
            e.printStackTrace();
        }
    }
}