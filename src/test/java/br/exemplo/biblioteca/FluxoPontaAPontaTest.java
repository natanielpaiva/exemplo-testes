package br.exemplo.biblioteca;

import static br.exemplo.biblioteca.RelogioFixo.marco;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * NIVEL: PONTA A PONTA. Sobe a API HTTP de verdade (porta livre) com banco H2 e conversa por HTTP.
 * Poucos testes, mais lentos: cobrem a jornada, nao cada regra.
 */
class FluxoPontaAPontaTest {

    private final HttpClient http = HttpClient.newHttpClient();
    private RepositorioJdbc banco;
    private RelogioFixo relogio;
    private ApiHttp api;
    private String base;

    @BeforeEach
    void subirAplicacao() throws Exception {
        banco = RepositorioJdbc.emMemoria();
        relogio = new RelogioFixo(marco(1));
        api = new ApiHttp(new BibliotecaService(banco, relogio));
        base = "http://localhost:" + api.iniciar(0);
    }

    @AfterEach
    void derrubarAplicacao() throws Exception {
        api.parar();
        banco.close();
    }

    private HttpResponse<String> post(String caminho) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base + caminho))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base + caminho)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static long id(HttpResponse<String> resposta) {
        Matcher m = Pattern.compile("\"id\":(\\d+)").matcher(resposta.body());
        assertTrue(m.find(), "sem id na resposta: " + resposta.body());
        return Long.parseLong(m.group(1));
    }

    @Test
    void fluxoFelizEmprestarUmExemplar() throws Exception {
        long membro = id(post("/membros?nome=Ana"));
        long exemplar = id(post("/exemplares?titulo=Dom+Casmurro"));

        HttpResponse<String> r = post("/emprestimos?membro=" + membro + "&exemplar=" + exemplar);

        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"vencimento\":\"2025-03-15\""), r.body());
    }

    @Test
    void exemplarJaEmprestadoEhRecusadoComMensagemClara() throws Exception {
        long ana = id(post("/membros?nome=Ana"));
        long bia = id(post("/membros?nome=Bia"));
        long exemplar = id(post("/exemplares?titulo=Ultimo+exemplar"));
        post("/emprestimos?membro=" + ana + "&exemplar=" + exemplar);

        HttpResponse<String> r = post("/emprestimos?membro=" + bia + "&exemplar=" + exemplar);

        assertEquals(422, r.statusCode());
        assertTrue(r.body().contains("EXEMPLAR_INDISPONIVEL"), r.body());
    }

    @Test
    void devolucaoEm18DeMarcoCobraR3EBloqueiaOMembro() throws Exception {
        long membro = id(post("/membros?nome=Ana"));
        long exemplar = id(post("/exemplares?titulo=Dom+Casmurro"));
        long emprestimo = id(post("/emprestimos?membro=" + membro + "&exemplar=" + exemplar));
        relogio.definir(marco(18));

        HttpResponse<String> r = post("/emprestimos/" + emprestimo + "/devolucao");

        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"multa\":\"3.00\""), "esperado multa 3.00, obtido: " + r.body());
        assertTrue(r.body().contains("\"bloqueado\":true"), r.body());
        assertTrue(get("/membros/" + membro).body().contains("\"bloqueado\":true"));
    }

    @Test
    void devolucaoNoDiaDoVencimentoNaoCobraNada() throws Exception {
        long membro = id(post("/membros?nome=Ana"));
        long exemplar = id(post("/exemplares?titulo=Dom+Casmurro"));
        long emprestimo = id(post("/emprestimos?membro=" + membro + "&exemplar=" + exemplar));
        relogio.definir(marco(15));

        HttpResponse<String> r = post("/emprestimos/" + emprestimo + "/devolucao");

        assertTrue(r.body().contains("\"multa\":\"0.00\""), "esperado multa 0.00, obtido: " + r.body());
        assertTrue(r.body().contains("\"bloqueado\":false"), r.body());
    }
}
