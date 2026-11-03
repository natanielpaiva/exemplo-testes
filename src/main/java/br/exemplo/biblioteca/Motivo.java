package br.exemplo.biblioteca;

public enum Motivo {
    EXEMPLAR_INDISPONIVEL("Exemplar indisponível"),
    MEMBRO_BLOQUEADO("Membro bloqueado: atraso em aberto ou débito de multa"),
    LIMITE_ATINGIDO("Limite de 3 empréstimos ativos atingido"),
    RENOVACAO_JA_USADA("Este empréstimo já foi renovado uma vez"),
    EMPRESTIMO_ENCERRADO("Empréstimo já devolvido"),
    NAO_ENCONTRADO("Registro não encontrado");

    private final String mensagem;

    Motivo(String mensagem) {
        this.mensagem = mensagem;
    }

    public String mensagem() {
        return mensagem;
    }
}
