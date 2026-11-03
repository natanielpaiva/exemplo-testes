package br.exemplo.biblioteca;

public class Main {
    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        BibliotecaService service = new BibliotecaService(RepositorioJdbc.emMemoria(), Relogio.sistema());
        int efetiva = new ApiHttp(service).iniciar(porta);
        System.out.println("Biblioteca no ar em http://localhost:" + efetiva);
    }
}
