package br.exemplo.biblioteca;

import static br.exemplo.biblioteca.RelogioFixo.marco;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * NIVEL: UNIDADE (regras do servico). Relogio fixo (stub) e repositorio em memoria (fake):
 * rapido, determinístico e sem banco. Emprestimo em 01/03, vencimento em 15/03.
 */
class BibliotecaServiceTest {

    private RelogioFixo relogio;
    private RepositorioEmMemoria repo;
    private BibliotecaService service;
    private Membro ana;

    @BeforeEach
    void preparar() {
        relogio = new RelogioFixo(marco(1));
        repo = new RepositorioEmMemoria();
        service = new BibliotecaService(repo, relogio);
        ana = service.cadastrarMembro("Ana");
    }

    private Emprestimo emprestarNovoExemplar(Membro membro) {
        return service.emprestar(membro.id(), service.cadastrarExemplar("Livro").id());
    }

    private Motivo motivoDe(Runnable acao) {
        return assertThrows(RegraDeNegocioException.class, acao::run).motivo();
    }

    @Test
    void emprestimoVenceEm14Dias() {
        Emprestimo e = emprestarNovoExemplar(ana);

        assertEquals(marco(15), e.vencimento());
    }

    @Test
    void devolucaoNoDiaDoVencimentoNaoTemMultaNemBloqueio() {
        Emprestimo e = emprestarNovoExemplar(ana);
        relogio.definir(marco(15));

        Emprestimo devolvido = service.devolver(e.id());

        assertEquals(new BigDecimal("0.00"), devolvido.multa());
        assertFalse(service.estaBloqueado(ana.id()));
    }

    @Test
    void devolucaoComTresDiasDeAtrasoGeraMultaDe3EBloqueia() {
        Emprestimo e = emprestarNovoExemplar(ana);
        relogio.definir(marco(18));

        service.devolver(e.id());

        assertEquals(new BigDecimal("3.00"), service.consultarMembro(ana.id()).debito());
        assertTrue(service.estaBloqueado(ana.id()));
    }

    @Test
    void devolucaoComSeteDiasDeAtrasoGeraMultaDe7() {
        Emprestimo e = emprestarNovoExemplar(ana);
        relogio.definir(marco(22));

        service.devolver(e.id());

        assertEquals(new BigDecimal("7.00"), service.consultarMembro(ana.id()).debito());
    }

    @Test
    void atrasoEmAbertoBloqueiaNovoEmprestimo() {
        emprestarNovoExemplar(ana);
        relogio.definir(marco(16)); // venceu ontem e nao foi devolvido

        assertEquals(Motivo.MEMBRO_BLOQUEADO, motivoDe(() -> emprestarNovoExemplar(ana)));
    }

    @Test
    void quitarODebitoLiberaOMembro() {
        Emprestimo e = emprestarNovoExemplar(ana);
        relogio.definir(marco(18));
        service.devolver(e.id());
        assertTrue(service.estaBloqueado(ana.id()));

        service.quitarDebito(ana.id());

        assertFalse(service.estaBloqueado(ana.id()));
        emprestarNovoExemplar(ana); // volta a poder emprestar
    }

    @Test
    void limiteDeTresEmprestimosAtivos() {
        emprestarNovoExemplar(ana);
        emprestarNovoExemplar(ana);
        emprestarNovoExemplar(ana);

        assertEquals(Motivo.LIMITE_ATINGIDO, motivoDe(() -> emprestarNovoExemplar(ana)));
    }

    @Test
    void exemplarJaEmprestadoEhRecusadoESemNovoEmprestimo() {
        Repositorio.Exemplar livro = service.cadastrarExemplar("Dom Casmurro");
        Membro bia = service.cadastrarMembro("Bia");
        service.emprestar(ana.id(), livro.id());

        assertEquals(Motivo.EXEMPLAR_INDISPONIVEL, motivoDe(() -> service.emprestar(bia.id(), livro.id())));
        assertEquals(1, repo.totalDeEmprestimos()); // estado final: nenhum emprestimo criado
    }

    @Test
    void devolucaoLiberaOExemplar() {
        Repositorio.Exemplar livro = service.cadastrarExemplar("Dom Casmurro");
        Membro bia = service.cadastrarMembro("Bia");
        Emprestimo e = service.emprestar(ana.id(), livro.id());
        service.devolver(e.id());

        Emprestimo novo = service.emprestar(bia.id(), livro.id());

        assertEquals(bia.id(), novo.membroId());
    }

    @Test
    void renovacaoEm10DeMarcoVaiParaDia22() {
        Emprestimo e = emprestarNovoExemplar(ana);
        relogio.definir(marco(10));

        Emprestimo renovado = service.renovar(e.id());

        assertEquals(marco(22), renovado.vencimento());
    }

    @Test
    void renovacaoEm16DeMarcoEhRecusadaPorAtraso() {
        Emprestimo e = emprestarNovoExemplar(ana);
        relogio.definir(marco(16));

        assertEquals(Motivo.MEMBRO_BLOQUEADO, motivoDe(() -> service.renovar(e.id())));
    }

    @Test
    void soPodeRenovarUmaVez() {
        Emprestimo e = emprestarNovoExemplar(ana);
        relogio.definir(marco(10));
        service.renovar(e.id());

        assertEquals(Motivo.RENOVACAO_JA_USADA, motivoDe(() -> service.renovar(e.id())));
    }
}
