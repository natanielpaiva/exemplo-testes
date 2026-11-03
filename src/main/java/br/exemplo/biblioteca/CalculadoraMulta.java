package br.exemplo.biblioteca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regra de multa: devolucao ate o dia do vencimento (inclusive) nao paga nada.
 * A partir do dia seguinte, R$ 1,00 por dia de atraso. Sem banco, sem relogio: so a regra.
 */
public final class CalculadoraMulta {

    public static final BigDecimal VALOR_POR_DIA = new BigDecimal("1.00");

    private CalculadoraMulta() {
    }

    public static BigDecimal calcular(LocalDate vencimento, LocalDate devolucao) {
        long diasAtraso = ChronoUnit.DAYS.between(vencimento, devolucao);
        if (diasAtraso <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return VALOR_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso)).setScale(2);
    }
}
