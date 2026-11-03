package br.exemplo.biblioteca;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistencia em H2 (banco em memoria, descartavel, sem Docker). */
public class RepositorioJdbc implements Repositorio, AutoCloseable {

    private final Connection conexao;

    /** Cria um banco novo e isolado a cada chamada, ideal para testes. */
    public static RepositorioJdbc emMemoria() {
        return new RepositorioJdbc("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    }

    public RepositorioJdbc(String url) {
        try {
            this.conexao = DriverManager.getConnection(url, "sa", "");
            criarSchema();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void criarSchema() throws SQLException {
        try (InputStream in = getClass().getResourceAsStream("/schema.sql");
             Statement st = conexao.createStatement()) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String comando : sql.split(";")) {
                if (!comando.isBlank()) {
                    st.execute(comando);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized Membro criarMembro(String nome) {
        try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO membro(nome) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            ps.executeUpdate();
            return new Membro(chaveGerada(ps), nome, BigDecimal.ZERO.setScale(2));
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized Exemplar criarExemplar(String titulo) {
        try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO exemplar(titulo) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, titulo);
            ps.executeUpdate();
            return new Exemplar(chaveGerada(ps), titulo);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized Optional<Membro> buscarMembro(long id) {
        try (PreparedStatement ps = conexao.prepareStatement(
                "SELECT id, nome, debito FROM membro WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Membro(rs.getLong("id"), rs.getString("nome"), rs.getBigDecimal("debito")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized boolean exemplarExiste(long id) {
        try (PreparedStatement ps = conexao.prepareStatement("SELECT 1 FROM exemplar WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized Optional<Emprestimo> buscarEmprestimo(long id) {
        try (PreparedStatement ps = conexao.prepareStatement("SELECT * FROM emprestimo WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized List<Emprestimo> emprestimosAtivos(long membroId) {
        try (PreparedStatement ps = conexao.prepareStatement(
                "SELECT * FROM emprestimo WHERE membro_id = ? AND data_devolucao IS NULL ORDER BY id")) {
            ps.setLong(1, membroId);
            List<Emprestimo> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized Emprestimo inserir(Emprestimo n) {
        try (PreparedStatement ps = conexao.prepareStatement(
                "INSERT INTO emprestimo(membro_id, exemplar_id, data_emprestimo, vencimento, renovado, multa, ativo_exemplar_id) "
                        + "VALUES (?,?,?,?,FALSE,0,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, n.membroId());
            ps.setLong(2, n.exemplarId());
            ps.setDate(3, Date.valueOf(n.dataEmprestimo()));
            ps.setDate(4, Date.valueOf(n.vencimento()));
            ps.setLong(5, n.exemplarId());
            ps.executeUpdate();
            long id = chaveGerada(ps);
            return new Emprestimo(id, n.membroId(), n.exemplarId(), n.dataEmprestimo(), n.vencimento(),
                    false, null, BigDecimal.ZERO.setScale(2));
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new RegraDeNegocioException(Motivo.EXEMPLAR_INDISPONIVEL);
            }
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized void atualizar(Emprestimo e) {
        try (PreparedStatement ps = conexao.prepareStatement(
                "UPDATE emprestimo SET vencimento = ?, renovado = ?, data_devolucao = ?, multa = ?, "
                        + "ativo_exemplar_id = ? WHERE id = ?")) {
            ps.setDate(1, Date.valueOf(e.vencimento()));
            ps.setBoolean(2, e.renovado());
            ps.setDate(3, e.dataDevolucao() == null ? null : Date.valueOf(e.dataDevolucao()));
            ps.setBigDecimal(4, e.multa());
            if (e.ativo()) {
                ps.setLong(5, e.exemplarId());
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            ps.setLong(6, e.id());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public synchronized void atualizarDebito(long membroId, BigDecimal novoDebito) {
        try (PreparedStatement ps = conexao.prepareStatement("UPDATE membro SET debito = ? WHERE id = ?")) {
            ps.setBigDecimal(1, novoDebito);
            ps.setLong(2, membroId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Emprestimo mapear(ResultSet rs) throws SQLException {
        Date devolucao = rs.getDate("data_devolucao");
        return new Emprestimo(rs.getLong("id"), rs.getLong("membro_id"), rs.getLong("exemplar_id"),
                rs.getDate("data_emprestimo").toLocalDate(), rs.getDate("vencimento").toLocalDate(),
                rs.getBoolean("renovado"), devolucao == null ? null : devolucao.toLocalDate(),
                rs.getBigDecimal("multa"));
    }

    private static long chaveGerada(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            keys.next();
            return keys.getLong(1);
        }
    }

    @Override
    public void close() throws SQLException {
        conexao.close();
    }
}
