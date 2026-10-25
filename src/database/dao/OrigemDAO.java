package database.dao;

import seguranca.SqlSeguro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrigemDAO {

    private final Connection conn;

    public OrigemDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Lê a tabela de origem com o filtro DS_WHERE configurado.
     *
     * Tabela e filtro não podem ir como parâmetro (?), então passam pelo SqlSeguro antes de entrar no SQL.
     * O fetchSize faz o driver trazer as linhas em lotes (vale com autocommit desligado), e o
     * closeOnCompletion fecha o PreparedStatement junto com o ResultSet, que antes ficava aberto.
     */
    public ResultSet SelectComandoOrigem(final String tabela, String where, int tamanhoLote) throws SQLException {
        String sql = "SELECT * FROM " + SqlSeguro.tabela(tabela) + SqlSeguro.where(where);
        System.out.println("Select na origem: " + sql);

        PreparedStatement pstSelect = conn.prepareStatement(sql);
        pstSelect.setFetchSize(tamanhoLote);
        pstSelect.closeOnCompletion();
        return pstSelect.executeQuery();
    }
}
