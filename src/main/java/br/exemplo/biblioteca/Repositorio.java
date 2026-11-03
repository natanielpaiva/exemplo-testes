package br.exemplo.biblioteca;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Porta de persistencia. Ha uma implementacao JDBC (H2) e um fake em memoria nos testes. */
public interface Repositorio {
    Membro criarMembro(String nome);

    Exemplar criarExemplar(String titulo);

    Optional<Membro> buscarMembro(long id);

    boolean exemplarExiste(long id);

    Optional<Emprestimo> buscarEmprestimo(long id);

    List<Emprestimo> emprestimosAtivos(long membroId);

    /** @throws RegraDeNegocioException EXEMPLAR_INDISPONIVEL se o exemplar ja estiver emprestado. */
    Emprestimo inserir(Emprestimo novo);

    void atualizar(Emprestimo emprestimo);

    void atualizarDebito(long membroId, BigDecimal novoDebito);

    record Exemplar(long id, String titulo) {
    }
}
