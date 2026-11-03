package br.exemplo.biblioteca;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * API HTTP minima sobre o servidor do JDK (sem framework).
 *
 * POST /membros?nome=Ana              GET  /membros/{id}
 * POST /exemplares?titulo=Dom+Casmurro
 * POST /emprestimos?membro=1&exemplar=1
 * POST /emprestimos/{id}/devolucao    POST /emprestimos/{id}/renovacao
 * POST /membros/{id}/quitacao
 */
public class ApiHttp {

    private final BibliotecaService service;
    private HttpServer server;

    public ApiHttp(BibliotecaService service) {
        this.service = service;
    }

    /** Sobe o servidor. Use porta 0 para uma porta livre. Retorna a porta efetiva. */
    public int iniciar(int porta) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", porta), 0);
        server.createContext("/", this::tratar);
        server.start();
        return server.getAddress().getPort();
    }

    public void parar() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void tratar(HttpExchange ex) throws IOException {
        int status;
        String corpo;
        try {
            String[] p = ex.getRequestURI().getPath().replaceAll("^/|/$", "").split("/");
            Map<String, String> q = consulta(ex.getRequestURI().getRawQuery());
            String metodo = ex.getRequestMethod();
            if (metodo.equals("POST") && p.length == 1 && p[0].equals("membros")) {
                Membro m = service.cadastrarMembro(q.get("nome"));
                status = 201;
                corpo = membroJson(m);
            } else if (metodo.equals("POST") && p.length == 1 && p[0].equals("exemplares")) {
                Repositorio.Exemplar e = service.cadastrarExemplar(q.get("titulo"));
                status = 201;
                corpo = "{\"id\":" + e.id() + "}";
            } else if (metodo.equals("GET") && p.length == 2 && p[0].equals("membros")) {
                corpo = membroJson(service.consultarMembro(Long.parseLong(p[1])));
                status = 200;
            } else if (metodo.equals("POST") && p.length == 1 && p[0].equals("emprestimos")) {
                Emprestimo e = service.emprestar(Long.parseLong(q.get("membro")), Long.parseLong(q.get("exemplar")));
                status = 201;
                corpo = emprestimoJson(e);
            } else if (metodo.equals("POST") && p.length == 3 && p[0].equals("emprestimos") && p[2].equals("devolucao")) {
                Emprestimo e = service.devolver(Long.parseLong(p[1]));
                status = 200;
                corpo = emprestimoJson(e).replaceFirst("}$",
                        ",\"bloqueado\":" + service.estaBloqueado(e.membroId()) + "}");
            } else if (metodo.equals("POST") && p.length == 3 && p[0].equals("emprestimos") && p[2].equals("renovacao")) {
                status = 200;
                corpo = emprestimoJson(service.renovar(Long.parseLong(p[1])));
            } else if (metodo.equals("POST") && p.length == 3 && p[0].equals("membros") && p[2].equals("quitacao")) {
                status = 200;
                corpo = membroJson(service.quitarDebito(Long.parseLong(p[1])));
            } else {
                status = 404;
                corpo = "{\"erro\":\"ROTA_INEXISTENTE\"}";
            }
        } catch (RegraDeNegocioException e) {
            status = e.motivo() == Motivo.NAO_ENCONTRADO ? 404 : 422;
            corpo = "{\"erro\":\"" + e.motivo() + "\",\"mensagem\":\"" + e.getMessage() + "\"}";
        } catch (RuntimeException e) {
            status = 400;
            corpo = "{\"erro\":\"REQUISICAO_INVALIDA\"}";
        }
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private String membroJson(Membro m) {
        return "{\"id\":" + m.id() + ",\"nome\":\"" + m.nome() + "\",\"debito\":\"" + m.debito().toPlainString()
                + "\",\"bloqueado\":" + service.estaBloqueado(m.id()) + "}";
    }

    private static String emprestimoJson(Emprestimo e) {
        return "{\"id\":" + e.id() + ",\"vencimento\":\"" + e.vencimento() + "\",\"multa\":\""
                + e.multa().toPlainString() + "\"}";
    }

    private static Map<String, String> consulta(String raw) {
        Map<String, String> mapa = new HashMap<>();
        if (raw == null) {
            return mapa;
        }
        for (String par : raw.split("&")) {
            String[] kv = par.split("=", 2);
            mapa.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return mapa;
    }
}
