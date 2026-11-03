package br.exemplo.biblioteca;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Regras de negocio do servico de emprestimos. */
public class BibliotecaService {

    public static final int PRAZO_DIAS = 14;
    public static final int DIAS_RENOVACAO = 7;
    public static final int LIMITE_ATIVOS = 3;

    private final Repositorio repositorio;
    private final Relogio relogio;

    public BibliotecaService(Repositorio repositorio, Relogio relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    public Membro cadastrarMembro(String nome) {
        return repositorio.criarMembro(nome);
    }

    public Repositorio.Exemplar cadastrarExemplar(String titulo) {
        return repositorio.criarExemplar(titulo);
    }

    public Membro consultarMembro(long membroId) {
        return repositorio.buscarMembro(membroId)
                .orElseThrow(() -> new RegraDeNegocioException(Motivo.NAO_ENCONTRADO));
    }

    /** Bloqueado = atraso em aberto (vencido e nao devolvido) ou debito de multa nao quitado. */
    public boolean estaBloqueado(long membroId) {
        Membro membro = consultarMembro(membroId);
        LocalDate hoje = relogio.hoje();
        return membro.temDebito()
                || repositorio.emprestimosAtivos(membroId).stream().anyMatch(e -> e.vencidoEm(hoje));
    }

    public Emprestimo emprestar(long membroId, long exemplarId) {
        consultarMembro(membroId);
        if (!repositorio.exemplarExiste(exemplarId)) {
            throw new RegraDeNegocioException(Motivo.NAO_ENCONTRADO);
        }
        if (estaBloqueado(membroId)) {
            throw new RegraDeNegocioException(Motivo.MEMBRO_BLOQUEADO);
        }
        if (repositorio.emprestimosAtivos(membroId).size() >= LIMITE_ATIVOS) {
            throw new RegraDeNegocioException(Motivo.LIMITE_ATINGIDO);
        }
        LocalDate hoje = relogio.hoje();
        Emprestimo novo = new Emprestimo(0, membroId, exemplarId, hoje, hoje.plusDays(PRAZO_DIAS),
                false, null, BigDecimal.ZERO);
        // A unicidade do exemplar ativo e garantida pelo banco (EXEMPLAR_INDISPONIVEL).
        return repositorio.inserir(novo);
    }

    public Emprestimo devolver(long emprestimoId) {
        Emprestimo atual = buscarAtivo(emprestimoId);
        LocalDate hoje = relogio.hoje();
        BigDecimal multa = CalculadoraMulta.calcular(atual.vencimento(), hoje);
        Emprestimo devolvido = new Emprestimo(atual.id(), atual.membroId(), atual.exemplarId(),
                atual.dataEmprestimo(), atual.vencimento(), atual.renovado(), hoje, multa);
        repositorio.atualizar(devolvido);
        if (multa.signum() > 0) {
            Membro membro = consultarMembro(atual.membroId());
            repositorio.atualizarDebito(membro.id(), membro.debito().add(multa));
        }
        return devolvido;
    }

    public Emprestimo renovar(long emprestimoId) {
        Emprestimo atual = buscarAtivo(emprestimoId);
        if (estaBloqueado(atual.membroId())) {
            throw new RegraDeNegocioException(Motivo.MEMBRO_BLOQUEADO);
        }
        if (atual.renovado()) {
            throw new RegraDeNegocioException(Motivo.RENOVACAO_JA_USADA);
        }
        Emprestimo renovado = new Emprestimo(atual.id(), atual.membroId(), atual.exemplarId(),
                atual.dataEmprestimo(), atual.vencimento().plusDays(DIAS_RENOVACAO), true, null,
                atual.multa());
        repositorio.atualizar(renovado);
        return renovado;
    }

    /** O bibliotecario quita o debito e o bloqueio por multa cai. */
    public Membro quitarDebito(long membroId) {
        Membro membro = consultarMembro(membroId);
        repositorio.atualizarDebito(membroId, BigDecimal.ZERO.setScale(2));
        return new Membro(membro.id(), membro.nome(), BigDecimal.ZERO.setScale(2));
    }

    private Emprestimo buscarAtivo(long emprestimoId) {
        Emprestimo e = repositorio.buscarEmprestimo(emprestimoId)
                .orElseThrow(() -> new RegraDeNegocioException(Motivo.NAO_ENCONTRADO));
        if (!e.ativo()) {
            throw new RegraDeNegocioException(Motivo.EMPRESTIMO_ENCERRADO);
        }
        return e;
    }
}
