CREATE TABLE membro (
  id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome    VARCHAR(100) NOT NULL,
  debito  DECIMAL(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE exemplar (
  id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  titulo  VARCHAR(200) NOT NULL
);

-- ativo_exemplar_id guarda o id do exemplar enquanto o emprestimo esta ativo e NULL depois
-- da devolucao. A restricao UNIQUE garante "um exemplar, um emprestimo ativo" no banco.
CREATE TABLE emprestimo (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  membro_id         BIGINT NOT NULL REFERENCES membro(id),
  exemplar_id       BIGINT NOT NULL REFERENCES exemplar(id),
  data_emprestimo   DATE NOT NULL,
  vencimento        DATE NOT NULL,
  renovado          BOOLEAN NOT NULL DEFAULT FALSE,
  data_devolucao    DATE,
  multa             DECIMAL(10,2) NOT NULL DEFAULT 0,
  ativo_exemplar_id BIGINT UNIQUE
);
