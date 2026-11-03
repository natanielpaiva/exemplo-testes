package br.exemplo.biblioteca;

import static br.exemplo.biblioteca.RelogioFixo.marco;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * NIVEL: INTEGRACAO. Servico + repositorio JDBC + H2 real (em memoria, descartavel por teste).
 * Aqui o risco e a persistencia: query, restricao de unicidade, estado gravado.
 */
class EmprestimoIntegracaoTest {

    private RepositorioJdbc banco;
    private RelogioFixo relogio;
    private BibliotecaService service;

    @BeforeEach
    void preparar() {
        banco = RepositorioJdbc.emMemoria();
        relogio = new RelogioFixo(marco(1));
        service = new BibliotecaService(banco, relogio);
    }

    @AfterEach
    void limpar() throws Exception {
        banco.close();
    }

    @Test
    void emprestimoFicaGravadoEExemplarIndisponivel() {
        Membro ana = service.cadastrarMembro("Ana");
        Repositorio.Exemplar livro = service.cadastrarExemplar("Dom Casmurro");

        Emprestimo e = service.emprestar(ana.id(), livro.id());

        Emprestimo gravado = banco.buscarEmprestimo(e.id()).orElseThrow();
        assertEquals(marco(15), gravado.vencimento());
        assertTrue(gravado.ativo());
        assertEquals(1, banco.emprestimosAtivos(ana.id()).size());
    }

    @Test
    void segundoPedidoDoMesmoExemplarEhRecusadoPeloBanco() {
        Membro ana = service.cadastrarMembro("Ana");
        Membro bia = service.cadastrarMembro("Bia");
        Repositorio.Exemplar livro = service.cadastrarExemplar("Dom Casmurro");
        service.emprestar(ana.id(), livro.id());

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.emprestar(bia.id(), livro.id()));

        assertEquals(Motivo.EXEMPLAR_INDISPONIVEL, erro.motivo());
        assertTrue(banco.emprestimosAtivos(bia.id()).isEmpty()); // nada foi gravado para a Bia
    }

    @Test
    void devolucaoLiberaOExemplarNoBanco() {
        Membro ana = service.cadastrarMembro("Ana");
        Membro bia = service.cadastrarMembro("Bia");
        Repositorio.Exemplar livro = service.cadastrarExemplar("Dom Casmurro");
        Emprestimo e = service.emprestar(ana.id(), livro.id());
        service.devolver(e.id());

        service.emprestar(bia.id(), livro.id()); // nao lanca: a restricao de unicidade foi liberada

        assertEquals(1, banco.emprestimosAtivos(bia.id()).size());
        assertTrue(banco.emprestimosAtivos(ana.id()).isEmpty());
    }

    @Test
    void multaViraDebitoPersistidoEQuitacaoLiberaOBloqueio() {
        Membro ana = service.cadastrarMembro("Ana");
        Emprestimo e = service.emprestar(ana.id(), service.cadastrarExemplar("Livro").id());
        relogio.definir(marco(18));
        service.devolver(e.id());

        assertEquals(new BigDecimal("3.00"), banco.buscarMembro(ana.id()).orElseThrow().debito());
        assertTrue(service.estaBloqueado(ana.id()));

        service.quitarDebito(ana.id());

        assertEquals(0, banco.buscarMembro(ana.id()).orElseThrow().debito().signum());
        assertFalse(service.estaBloqueado(ana.id()));
    }

    @Test
    void ultimoExemplarPedidoPorDoisMembrosAoMesmoTempoSoUmVence() throws Exception {
        Membro ana = service.cadastrarMembro("Ana");
        Membro bia = service.cadastrarMembro("Bia");
        long livro = service.cadastrarExemplar("Ultimo exemplar").id();
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger recusas = new AtomicInteger();

        Thread[] threads = new Thread[2];
        long[] membros = {ana.id(), bia.id()};
        for (int i = 0; i < 2; i++) {
            long membro = membros[i];
            threads[i] = new Thread(() -> {
                try {
                    largada.await();
                    service.emprestar(membro, livro);
                    sucessos.incrementAndGet();
                } catch (RegraDeNegocioException e) {
                    recusas.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        largada.countDown();
        for (Thread t : threads) {
            t.join();
        }

        assertEquals(1, sucessos.get());
        assertEquals(1, recusas.get());
    }
}
