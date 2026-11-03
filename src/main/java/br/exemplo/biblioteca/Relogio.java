package br.exemplo.biblioteca;

import java.time.LocalDate;

/** Fonte de "hoje". Injetada para que os testes usem datas fixas. */
public interface Relogio {
    LocalDate hoje();

    static Relogio sistema() {
        return LocalDate::now;
    }
}
