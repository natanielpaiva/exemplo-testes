package br.exemplo.biblioteca;

import java.math.BigDecimal;

public record Membro(long id, String nome, BigDecimal debito) {
    public boolean temDebito() {
        return debito.signum() > 0;
    }
}
