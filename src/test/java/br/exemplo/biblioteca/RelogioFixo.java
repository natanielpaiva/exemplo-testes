package br.exemplo.biblioteca;

import java.time.LocalDate;

/** Stub de relogio: a data e definida pelo teste, nunca por "hoje". */
public class RelogioFixo implements Relogio {
    private LocalDate hoje;

    public RelogioFixo(LocalDate hoje) {
        this.hoje = hoje;
    }

    public void definir(LocalDate novaData) {
        this.hoje = novaData;
    }

    @Override
    public LocalDate hoje() {
        return hoje;
    }

    /** Atalho: dia de marco de 2025 (o emprestimo do roteiro e em 01/03 e vence em 15/03). */
    public static LocalDate marco(int dia) {
        return LocalDate.of(2025, 3, dia);
    }
}
