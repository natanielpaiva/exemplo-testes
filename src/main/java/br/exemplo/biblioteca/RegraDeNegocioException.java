package br.exemplo.biblioteca;

public class RegraDeNegocioException extends RuntimeException {
    private final Motivo motivo;

    public RegraDeNegocioException(Motivo motivo) {
        super(motivo.mensagem());
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
