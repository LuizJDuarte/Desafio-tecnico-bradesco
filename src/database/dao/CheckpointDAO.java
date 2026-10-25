package database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Guarda no banco de controle até qual evento da replication_queue cada direção já aplicou.
 *
 * Antes, o evento era marcado como processado na própria origem. Com dois destinos para a mesma
 * origem, o primeiro marcava e o segundo nunca recebia a alteração. Com um ponto de parada por
 * direção, cada destino avança sozinho, e o replicador não precisa mais escrever na origem.
 */
public class CheckpointDAO {

    private static final String SQL_SELECT =
            "SELECT ULTIMO_ID_FILA FROM TB_REPLICACAO_CHECKPOINT WHERE DIRECAO_ID = ?";

    private static final String SQL_UPSERT =
            "INSERT INTO TB_REPLICACAO_CHECKPOINT (DIRECAO_ID, ULTIMO_ID_FILA, ATUALIZADO_EM) VALUES (?, ?, NOW()) " +
            "ON CONFLICT (DIRECAO_ID) DO UPDATE SET ULTIMO_ID_FILA = EXCLUDED.ULTIMO_ID_FILA, ATUALIZADO_EM = NOW()";

    private final PreparedStatement pstSelect;
    private final PreparedStatement pstUpsert;

    public CheckpointDAO(Connection conn) throws SQLException {
        this.pstSelect = conn.prepareStatement(SQL_SELECT);
        this.pstUpsert = conn.prepareStatement(SQL_UPSERT);
    }

    public long ultimoIdProcessado(long direcaoId) throws SQLException {
        pstSelect.setLong(1, direcaoId);
        try (ResultSet rs = pstSelect.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    public void salvar(long direcaoId, long ultimoIdFila) throws SQLException {
        pstUpsert.setLong(1, direcaoId);
        pstUpsert.setLong(2, ultimoIdFila);
        pstUpsert.executeUpdate();
    }
}
