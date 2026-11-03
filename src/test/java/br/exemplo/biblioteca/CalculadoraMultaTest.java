package br.exemplo.biblioteca;

import static br.exemplo.biblioteca.RelogioFixo.marco;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** NIVEL: UNIDADE. Regra isolada, sem banco, sem relogio, sem rede. Vencimento: 15/03. */
class CalculadoraMultaTest {

    @ParameterizedTest(name = "devolucao em {0}/03 resulta em multa de R$ {1}")
    @CsvSource({
            "10, 0.00",   // antes do vencimento
            "14, 0.00",   // um dia antes
            "15, 0.00",   // LIMITE: dia do vencimento nao e atraso
            "16, 1.00",   // LIMITE: primeiro dia de atraso
            "18, 3.00",   // exemplo do roteiro
            "22, 7.00"    // exemplo do roteiro
    })
    void multaPorDiaDeAtraso(int diaDevolucao, String esperado) {
        BigDecimal multa = CalculadoraMulta.calcular(marco(15), marco(diaDevolucao));

        assertEquals(new BigDecimal(esperado), multa, "esperado " + esperado + ", obtido " + multa);
    }
}
