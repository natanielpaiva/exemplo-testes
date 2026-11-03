package br.exemplo.biblioteca;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Emprestimo(long id, long membroId, long exemplarId, LocalDate dataEmprestimo,
                         LocalDate vencimento, boolean renovado, LocalDate dataDevolucao,
                         BigDecimal multa) {

    public boolean ativo() {
        return dataDevolucao == null;
    }

    public boolean vencidoEm(LocalDate hoje) {
        return ativo() && hoje.isAfter(vencimento);
    }
}
