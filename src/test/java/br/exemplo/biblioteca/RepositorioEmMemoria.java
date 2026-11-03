package br.exemplo.biblioteca;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fake: implementacao simples (mapas) que respeita a regra "um exemplar, um emprestimo ativo". */
public class RepositorioEmMemoria implements Repositorio {
    private final Map<Long, Membro> membros = new HashMap<>();
    private final Map<Long, Exemplar> exemplares = new HashMap<>();
    private final Map<Long, Emprestimo> emprestimos = new HashMap<>();
    private long seq = 0;

    @Override
    public Membro criarMembro(String nome) {
        Membro m = new Membro(++seq, nome, BigDecimal.ZERO.setScale(2));
        membros.put(m.id(), m);
        return m;
    }

    @Override
    public Exemplar criarExemplar(String titulo) {
        Exemplar e = new Exemplar(++seq, titulo);
        exemplares.put(e.id(), e);
        return e;
    }

    @Override
    public Optional<Membro> buscarMembro(long id) {
        return Optional.ofNullable(membros.get(id));
    }

    @Override
    public boolean exemplarExiste(long id) {
        return exemplares.containsKey(id);
    }

    @Override
    public Optional<Emprestimo> buscarEmprestimo(long id) {
        return Optional.ofNullable(emprestimos.get(id));
    }

    @Override
    public List<Emprestimo> emprestimosAtivos(long membroId) {
        return emprestimos.values().stream()
                .filter(e -> e.membroId() == membroId && e.ativo()).toList();
    }

    @Override
    public Emprestimo inserir(Emprestimo n) {
        boolean ocupado = emprestimos.values().stream()
                .anyMatch(e -> e.exemplarId() == n.exemplarId() && e.ativo());
        if (ocupado) {
            throw new RegraDeNegocioException(Motivo.EXEMPLAR_INDISPONIVEL);
        }
        Emprestimo e = new Emprestimo(++seq, n.membroId(), n.exemplarId(), n.dataEmprestimo(),
                n.vencimento(), false, null, BigDecimal.ZERO.setScale(2));
        emprestimos.put(e.id(), e);
        return e;
    }

    @Override
    public void atualizar(Emprestimo e) {
        emprestimos.put(e.id(), e);
    }

    @Override
    public void atualizarDebito(long membroId, BigDecimal novoDebito) {
        Membro m = membros.get(membroId);
        membros.put(membroId, new Membro(m.id(), m.nome(), novoDebito));
    }

    public int totalDeEmprestimos() {
        return new ArrayList<>(emprestimos.values()).size();
    }
}
