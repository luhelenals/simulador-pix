package common.models;

import java.time.LocalDateTime;

/**
 * Representa a entidade Transação no sistema.
 * Contém informações sobre o remetente, o destinatário, o valor e a data.
 */
public class Transacao {

    private int id; // O ID será gerado pelo banco de dados (autoincremento)
    private String cpfRemetente;
    private String cpfDestinatario;
    private double valor;
    private LocalDateTime dataTransacao;

    public Transacao() {
    }

    /**
     * Construtor para criar uma nova transação.
     * A data da transação é definida para o momento da criação do objeto.
     * @param cpfRemetente O CPF do usuário que está enviando o dinheiro.
     * @param cpfDestinatario O CPF do usuário que está recebendo o dinheiro.
     * @param valor O valor que está sendo transferido.
     */
    public Transacao(String cpfRemetente, String cpfDestinatario, double valor) {
        this.cpfRemetente = cpfRemetente;
        this.cpfDestinatario = cpfDestinatario;
        this.valor = valor;
        this.dataTransacao = LocalDateTime.now(); // Define a data/hora atual
    }

    // Getters e Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCpfRemetente() {
        return cpfRemetente;
    }

    public void setCpfRemetente(String cpfRemetente) {
        this.cpfRemetente = cpfRemetente;
    }

    public String getCpfDestinatario() {
        return cpfDestinatario;
    }

    public void setCpfDestinatario(String cpfDestinatario) {
        this.cpfDestinatario = cpfDestinatario;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataTransacao() {
        return dataTransacao;
    }

    public void setDataTransacao(LocalDateTime dataTransacao) {
        this.dataTransacao = dataTransacao;
    }
}