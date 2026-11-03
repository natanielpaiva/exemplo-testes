# exemplo-testes

Exemplo da **Aula 1 (Qualidade e Testes)**: um serviço de empréstimos de biblioteca com testes nos três níveis da pirâmide (unidade, integração e ponta a ponta).

**Stack:** Java 17+, Maven 3.9.x, JUnit 5.10.2, H2 2.2.224 (banco em memória, sem Docker) e o servidor HTTP do JDK para a API. Sem framework, sem Docker, sem internet depois do primeiro `mvn`.

## Executar

```bash
mvn test                                            # todos os 27 testes
mvn test -Dtest=CalculadoraMultaTest                # unidade (regra de multa)
mvn test -Dtest=BibliotecaServiceTest               # unidade (regras do serviço, com fake e relógio fixo)
mvn test -Dtest=EmprestimoIntegracaoTest            # integração (serviço + H2 real)
mvn test -Dtest=FluxoPontaAPontaTest                # ponta a ponta (HTTP de verdade)
mvn exec:java                                       # sobe a API em http://localhost:8080
```

## Regras (valores didáticos)

| Regra | Valor |
|---|---|
| Prazo | 14 dias (empréstimo em 01/03 vence em 15/03) |
| Limite | 3 empréstimos ativos por membro |
| Disponibilidade | um exemplar, um empréstimo ativo por vez |
| Multa | até o vencimento (inclusive): R$ 0,00. Depois, R$ 1,00 por dia. Vira débito do membro |
| Bloqueio | atraso em aberto ou débito não quitado: sem novos empréstimos e sem renovação |
| Renovação | uma vez, +7 dias, só para membro sem atraso e sem bloqueio |

## Mapa dos testes

| Nível | Classe | O que prova | Dependências |
|---|---|---|---|
| Unidade | `CalculadoraMultaTest` | multa nos limites (15/03, 16/03, 18/03, 22/03) | nenhuma |
| Unidade | `BibliotecaServiceTest` | bloqueio, limite, renovação, devolução | relógio fixo (stub) e repositório em memória (fake) |
| Integração | `EmprestimoIntegracaoTest` | persistência, restrição de unicidade do exemplar, débito gravado, concorrência no último exemplar | H2 real em memória |
| Ponta a ponta | `FluxoPontaAPontaTest` | emprestar, recusar, devolver com multa via HTTP | API no ar + H2 |

Test doubles usados: **stub** (`RelogioFixo`, data definida pelo teste) e **fake** (`RepositorioEmMemoria`). O banco real só entra onde o risco é a persistência.

## Demonstração: falha injetada para o diagnóstico

1. Em `src/main/java/br/exemplo/biblioteca/CalculadoraMulta.java`, conte o dia do vencimento como atraso (erro de um dia):

   ```java
   long diasAtraso = ChronoUnit.DAYS.between(vencimento, devolucao);
   if (diasAtraso < 0) {                                        // era: <= 0
       return BigDecimal.ZERO.setScale(2);
   }
   return VALOR_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso + 1)).setScale(2);   // era: diasAtraso
   ```

2. Rode `mvn test`. **10 testes falham.**
3. Compare o diagnóstico:
   - **Unidade:** `CalculadoraMultaTest` falha primeiro e diz `esperado 3.00, obtido 4.00`, apontando a regra e o caso.
   - **Ponta a ponta:** `FluxoPontaAPontaTest` também falha, mas só mostra `"multa":"4.00"` na resposta, sem dizer em que camada nem em que linha.
4. Desfaça a alteração (`git checkout src/main`) e rode `mvn test` de novo: tudo verde.

## API

```
POST /membros?nome=Ana                  GET  /membros/{id}
POST /exemplares?titulo=Dom+Casmurro
POST /emprestimos?membro=1&exemplar=1
POST /emprestimos/{id}/devolucao        POST /emprestimos/{id}/renovacao
POST /membros/{id}/quitacao
```

Fora do escopo: reserva de exemplares, pagamento online e notificações reais.

## CI

`.github/workflows/ci.yml` roda `mvn test` a cada push (Java 17).
