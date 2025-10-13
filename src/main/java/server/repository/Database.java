package server.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe utilitária para gerenciar a conexão e a inicialização do banco de dados SQLite.
 */
public class Database {

    // Define o nome do arquivo do banco de dados. Ele será criado na raiz do projeto.
    private static final String DB_URL = "jdbc:sqlite:banco.db";

    /**
     * Construtor privado para impedir a instanciação da classe.
     */
    private Database() {}

    /**
     * Fornece uma nova conexão com o banco de dados.
     * @return um objeto Connection com o banco.
     * @throws SQLException se a conexão falhar.
     */
    public static Connection getConnection() throws SQLException {
        // Carrega o driver JDBC do SQLite (passo necessário em algumas configurações)
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver JDBC do SQLite não encontrado.");
            e.printStackTrace();
        }
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Cria as tabelas necessárias no banco de dados se elas não existirem.
     * Este método deve ser chamado uma vez na inicialização do servidor.
     */
    public static void initialize() {
        // SQL para criar a tabela de usuários
        String sqlUsuario = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "  cpf TEXT PRIMARY KEY," +
                "  nome TEXT NOT NULL," +
                "  senha TEXT NOT NULL," +
                "  saldo REAL NOT NULL DEFAULT 0.0" +
                ");";

        // SQL para criar a tabela de transações
        String sqlTransacao = "CREATE TABLE IF NOT EXISTS transacoes (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  cpf_remetente TEXT NOT NULL," +
                "  cpf_destinatario TEXT NOT NULL," +
                "  valor REAL NOT NULL," +
                "  data_transacao TEXT NOT NULL," +
                "  FOREIGN KEY (cpf_remetente) REFERENCES usuarios(cpf)," +
                "  FOREIGN KEY (cpf_destinatario) REFERENCES usuarios(cpf)" +
                ");";

        // Usamos try-with-resources para garantir que a conexão e o statement sejam fechados
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Executa os comandos SQL para criar as tabelas
            stmt.execute(sqlUsuario);
            stmt.execute(sqlTransacao);

            System.out.println("Banco de dados verificado/inicializado com sucesso.");

        } catch (SQLException e) {
            System.err.println("Erro ao inicializar o banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }
}