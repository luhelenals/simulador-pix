package common.models;

/**
 * Representa a entidade Usuário no sistema.
 * Contém informações pessoais, credenciais e saldo.
 */
public class Usuario {

    private String nome;
    private String cpf;
    private String senha;
    private double saldo;

    public Usuario() {
    }

    /**double saldo
     * Construtor para criar um novo usuário. O saldo inicial é zero.
     * @param nome O nome completo do usuário.
     * @param cpf O CPF do usuário (formato "000.000.000-00").
     * @param senha A senha de acesso do usuário.
     */
    public Usuario(String nome, String cpf, String senha, double saldo) {
        this.nome = nome;
        this.cpf = cpf;
        this.senha = senha;
        this.saldo = saldo;
    }

    // Getters e Setters

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    // Métodos de negócio (opcional, mas recomendado)

    /**
     * Adiciona um valor ao saldo do usuário.
     * @param valor O valor a ser depositado (deve ser positivo).
     */
    public void depositar(double valor) {
        if (valor > 0) {
            this.saldo += valor;
        }
    }

    /**
     * Remove um valor do saldo do usuário.
     * @param valor O valor a ser sacado (deve ser positivo).
     * @throws IllegalArgumentException se o saldo for insuficiente.
     */
    public void sacar(double valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo.");
        }
        if (this.saldo < valor) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar o saque.");
        }
        this.saldo -= valor;
    }

    @Override
    public String toString() {
        // ATENÇÃO: Nunca inclua a senha em um método toString() em um projeto real.
        return "Usuario{" +
                "nome='" + nome + '\'' +
                ", cpf='" + cpf + '\'' +
                ", saldo=" + saldo +
                '}';
    }
}