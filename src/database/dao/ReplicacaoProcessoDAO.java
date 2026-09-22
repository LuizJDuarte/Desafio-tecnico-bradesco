package database.dao;

import database.model.TB_REPLICACAO_PROCESSO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ReplicacaoProcessoDAO {

    // Toda classe de acesso com o BD, precisa de uma conexão e para isso usamos essa variável
    private Connection conn;

    private static final String SQL_SELECT =
            "SELECT * FROM TB_REPLICACAO_PROCESSO";

    private static final String SQL_SELECT_BY_ID =
            "SELECT * FROM TB_REPLICACAO_PROCESSO WHERE ID = ?";

    private static final String SQL_INSERT =
            "INSERT INTO TB_REPLICACAO_PROCESSO (PROCESSO, DESCRICAO, HABILITADO) VALUES (?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE TB_REPLICACAO_PROCESSO SET PROCESSO = ?, DESCRICAO = ?, HABILITADO = ? WHERE ID = ?";

    private static final String SQL_DELETE =
            "DELETE FROM TB_REPLICACAO_PROCESSO WHERE ID = ?";

    private PreparedStatement pstSelectAll;
    private PreparedStatement pstSelectById;
    private PreparedStatement pstInsert;
    private PreparedStatement pstUpdate;
    private PreparedStatement pstDelete;

    public ReplicacaoProcessoDAO (Connection conn) throws SQLException {
        this.conn = conn;
        this.pstSelectAll = conn.prepareStatement(SQL_SELECT);
        this.pstSelectById = conn.prepareStatement(SQL_SELECT_BY_ID);
        this.pstInsert = conn.prepareStatement(SQL_INSERT);
        this.pstUpdate = conn.prepareStatement(SQL_UPDATE);
        this.pstDelete = conn.prepareStatement(SQL_DELETE);
    }

    // Vamos usar ArrayList, pois o select all vai nos retornar todas as linhas, e precisamos armazena-las, em alguma EDA correspondente
    public ArrayList<TB_REPLICACAO_PROCESSO> selectAll() throws SQLException{
        ArrayList<TB_REPLICACAO_PROCESSO> lista = new ArrayList<>();
        try (ResultSet rs = pstSelectAll.executeQuery()){
            // Se tivermos 10 linhas, vai rodar 10 vezes nesse While
            while (rs.next()){
                lista.add(map(rs));
            }
        }
        return lista;
    }

    public TB_REPLICACAO_PROCESSO selectById(long id) throws SQLException{
        // Vai substituir o where id = ?, nessa próxima linha (o 1 significa que é o primeiro interrogação,
        //  e o id, significa que vamos colocar esse id no lugar dessa interrogação)
        pstSelectById.setLong(1, id);
        try (ResultSet rs = pstSelectById.executeQuery()){
            return rs.next() ? map(rs) : null;
        }
    }

    public void insert(TB_REPLICACAO_PROCESSO tb) throws SQLException{
        pstInsert.setString(1, tb.getProcesso());
        pstInsert.setString(2, tb.getDescricao());
        pstInsert.setBoolean(3, tb.isHabilitado());
        pstInsert.executeUpdate();
    }

    public void update(TB_REPLICACAO_PROCESSO processo) throws SQLException {
        pstUpdate.setString(1, processo.getProcesso());
        pstUpdate.setString(2, processo.getDescricao());
        pstUpdate.setBoolean(3, processo.isHabilitado());
        pstUpdate.setLong(4, processo.getId());
        pstUpdate.executeUpdate();
    }

    public void delete(long id) throws SQLException{
        pstDelete.setLong(1, id);
        pstDelete.executeUpdate();
    }

    // Classe Mapper para mapear os atributos
    private TB_REPLICACAO_PROCESSO map(ResultSet rs) throws SQLException {
        // Referenciando entidade criada no model
        TB_REPLICACAO_PROCESSO tb = new TB_REPLICACAO_PROCESSO();
        tb.setId(rs.getLong("ID"));
        tb.setProcesso(rs.getString("PROCESSO"));
        tb.setDescricao(rs.getString("DESCRICAO"));
        tb.setHabilitado(rs.getBoolean("HABILITADO"));
        return tb;
    }
}
