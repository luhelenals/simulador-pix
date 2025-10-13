package common.models;

import java.time.LocalDateTime;

/**
 * Representa a entidade Transação no sistema.
 * Contém informações sobre o remetente, o destinatário, o valor e a data.
 */
public class Transacao {

    private int id; // O ID será gerado pelo banco de dados (autoincremento)
    private Usuario remetente;
    private Usuario destinatario;
    private double valor;
    private LocalDateTime dataTransacao;

    public Transacao() {
    }

    /**
     * Construtor para criar uma nova transação.
     * A data da transação é definida para o momento da criação do objeto.
     * @param remetente O objeto Usuario que está enviando o dinheiro.
     * @param destinatario O objeto Usuario que está recebendo o dinheiro.
     * @param valor O valor que está sendo transferido.
     */
    public Transacao(Usuario remetente, Usuario destinatario, double valor) {
        this.remetente = remetente;
        this.destinatario = destinatario;
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

    public Usuario getRemetente() {
        return remetente;
    }

    public void setRemetente(Usuario remetente) {
        this.remetente = remetente;
    }

    public Usuario getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Usuario destinatario) {
        this.destinatario = destinatario;
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

    @Override
    public String toString() {
        return "Transacao{" +
                "id=" + id +
                ", remetenteCpf='" + (remetente != null ? remetente.getCpf() : "N/A") + '\'' +
                ", destinatarioCpf='" + (destinatario != null ? destinatario.getCpf() : "N/A") + '\'' +
                ", valor=" + valor +
                ", dataTransacao=" + dataTransacao +
                '}';
    }
}