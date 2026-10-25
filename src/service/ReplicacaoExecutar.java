package service;

import database.dao.CheckpointDAO;
import database.dao.DirecaoDAO;
import database.dao.OrigemDAO;
import database.dao.ProcessoTabelaDAO;
import database.dao.ReplicacaoProcessoDAO;
import database.model.TB_REPLICACAO_DIRECAO;
import database.model.TB_REPLICACAO_PROCESSO;
import database.model.TB_REPLICACAO_PROCESSO_TABELA;
import seguranca.CriptografiaSenha;
import seguranca.SqlSeguro;
import seguranca.UrlJdbc;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReplicacaoExecutar {

    // Linhas lidas da origem por ida ao banco e enviadas ao destino por executeBatch.
    private static final int TAMANHO_LOTE = 500;

    private final Connection connControle;

    public ReplicacaoExecutar(Connection connControle) {
        this.connControle = connControle;
        System.out.println("Replicação Iniciada. Acompanhe pelo console ou log");
        try {
            ReplicacaoIniciar();
        } catch (SQLException e) {
            // Erro lendo a configuração: registra e tenta de novo no próximo ciclo, sem derrubar a thread.
            System.out.println("Falha ao ler a configuração no banco de controle: " + e.getMessage());
        }
        System.out.println("Replicação Finalizada. Aguardando inicio da próxima replicação");
    }

    private void ReplicacaoIniciar() throws SQLException {

        ReplicacaoProcessoDAO processo = new ReplicacaoProcessoDAO(connControle);
        DirecaoDAO direcao = new DirecaoDAO(connControle);
        ProcessoTabelaDAO tabela = new ProcessoTabelaDAO(connControle);
        CheckpointDAO checkpoint = new CheckpointDAO(connControle);

        ArrayList<TB_REPLICACAO_PROCESSO> arlProcessos = processo.selectAll();
        if (arlProcessos.isEmpty()) {
            System.out.println("Nenhum processo encontrado ...");
            return;
        }

        for (TB_REPLICACAO_PROCESSO p : arlProcessos) {
            if (!p.isHabilitado()) {
                System.out.println("Processo " + p.getProcesso() + " desabilitado ...");
                continue;
            }

            ArrayList<TB_REPLICACAO_PROCESSO_TABELA> arlTabelas = tabela.selectByProcessoHabilitado(p.getId());
            if (arlTabelas.isEmpty()) {
                System.out.println("Nenhuma tabela habilitada para replicar no processo " + p.getProcesso() + " ...");
                continue;
            }

            ArrayList<TB_REPLICACAO_DIRECAO> arlDirecao = direcao.selectByProcessoHabilitado(p.getId());
            if (arlDirecao.isEmpty()) {
                System.out.println("Nenhuma direção habilitada para replicar no processo " + p.getProcesso() + " ...");
                continue;
            }

            for (TB_REPLICACAO_DIRECAO d : arlDirecao) {
                // Cada direção roda isolada: origem ou destino fora do ar não impede as outras (RF6).
                try {
                    replicarDirecao(d, arlTabelas, checkpoint);
                } catch (Exception e) {
                    System.out.println("Falha na direção " + d.getId() + " (" + descricao(d) + "): " + e.getMessage());
                }
            }
        }
    }

    private void replicarDirecao(TB_REPLICACAO_DIRECAO d, List<TB_REPLICACAO_PROCESSO_TABELA> tabelas,
                                 CheckpointDAO checkpoint) throws SQLException {

        UrlJdbc.validar(d.getDirecao_origem());
        UrlJdbc.validar(d.getDirecao_destino());
        if (CriptografiaSenha.estaEmTextoPuro(d.getSenha_origem()) || CriptografiaSenha.estaEmTextoPuro(d.getSenha_destino())) {
            System.out.println("Aviso: a direção " + d.getId() + " tem senha gravada sem criptografia. "
                    + "Abra a direção na tela, digite as senhas e salve.");
        }

        // As duas conexões fecham ao fim da direção, mesmo com erro. Antes ficavam abertas a cada ciclo.
        try (Connection connOrigem = DriverManager.getConnection(d.getDirecao_origem(), d.getUsuario_origem(),
                     CriptografiaSenha.descriptografar(d.getSenha_origem()));
             Connection connDestino = DriverManager.getConnection(d.getDirecao_destino(), d.getUsuario_destino(),
                     CriptografiaSenha.descriptografar(d.getSenha_destino()))) {

            OrigemDAO daoOrigem = new OrigemDAO(connOrigem);
            for (TB_REPLICACAO_PROCESSO_TABELA t : tabelas) {
                System.out.println("Origem: " + descricao(d) + " - Tabela: " + t.getTabela_origem());
                replicarTabela(daoOrigem, connOrigem, connDestino, t);
            }

            ReplicacaoFinalizar(d, tabelas, connOrigem, connDestino, checkpoint);
        }
    }

    /**
     * Carga completa de uma tabela: insere no destino as linhas da origem que ainda não existem lá.
     * Uma falha desfaz só esta tabela; as próximas continuam.
     */
    private void replicarTabela(OrigemDAO daoOrigem, Connection connOrigem, Connection connDestino,
                                TB_REPLICACAO_PROCESSO_TABELA t) throws SQLException {

        // Leitura numa transação somente leitura: o DS_WHERE não consegue alterar dados na origem.
        // Com autocommit desligado o driver também respeita o fetchSize e não carrega a tabela inteira na memória.
        connOrigem.setAutoCommit(false);
        connOrigem.setReadOnly(true);
        connDestino.setAutoCommit(false);
        try {
            String tabelaDestino = SqlSeguro.tabela(t.getTabela_destino());
            int lidos = 0;
            int inseridos = 0;

            try (ResultSet resultado = daoOrigem.SelectComandoOrigem(t.getTabela_origem(), t.getDs_where(), TAMANHO_LOTE)) {
                ResultSetMetaData metaData = resultado.getMetaData();
                int ln_columns = metaData.getColumnCount();

                try (PreparedStatement pstInsert = connDestino.prepareStatement(insertGet(tabelaDestino, metaData))) {
                    int noLote = 0;
                    while (resultado.next()) {
                        for (int i = 1; i <= ln_columns; i++) {
                            pstInsert.setObject(i, resultado.getObject(i));
                        }
                        pstInsert.addBatch();
                        lidos++;

                        // Envia em lotes para não acumular a tabela inteira num único batch.
                        if (++noLote == TAMANHO_LOTE) {
                            inseridos += somar(pstInsert.executeBatch());
                            noLote = 0;
                        }
                    }
                    if (noLote > 0) {
                        inseridos += somar(pstInsert.executeBatch());
                    }
                }
            }

            connDestino.commit();
            connOrigem.commit();
            System.out.println("Dados replicados com sucesso! " + inseridos + " novo(s) de " + lidos + " lido(s).");

        } catch (SQLException | IllegalArgumentException e) {
            connDestino.rollback();
            connOrigem.rollback();
            System.out.println("Falha ao replicar a tabela " + t.getTabela_origem() + ": " + e.getMessage());
        } finally {
            connOrigem.setReadOnly(false);
            connOrigem.setAutoCommit(true);
            connDestino.setAutoCommit(true);
        }
    }

    private String insertGet(String tabelaDestino, ResultSetMetaData metaData) throws SQLException {

        StringBuilder colunas = new StringBuilder();
        StringBuilder valores = new StringBuilder();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (i > 1) {
                colunas.append(", ");
                valores.append(", ");
            }
            colunas.append(SqlSeguro.coluna(metaData.getColumnName(i)));
            valores.append("?");
        }

        // ON CONFLICT DO NOTHING: linha que já existe no destino é pulada e o resto do lote segue.
        // Antes, uma única chave repetida derrubava o lote inteiro e nada novo era copiado.
        return "insert into " + tabelaDestino + " (" + colunas + ") values (" + valores + ") on conflict do nothing";
    }

    /**
     * Aplica no destino as alterações que os triggers registraram na replication_queue da origem.
     *
     * O ponto de parada de cada direção fica no banco de controle (TB_REPLICACAO_CHECKPOINT), então
     * vários destinos da mesma origem recebem todos os eventos e a origem é só lida.
     */
    private void ReplicacaoFinalizar(TB_REPLICACAO_DIRECAO d, List<TB_REPLICACAO_PROCESSO_TABELA> tabelas,
                                     Connection connOrigem, Connection connDestino,
                                     CheckpointDAO checkpoint) throws SQLException {

        // Só eventos de tabelas configuradas no processo são aplicados, e vão para a TABELA_DESTINO configurada.
        Map<String, TB_REPLICACAO_PROCESSO_TABELA> configPorTabela = new HashMap<>();
        for (TB_REPLICACAO_PROCESSO_TABELA t : tabelas) {
            configPorTabela.put(nomeSemSchema(t.getTabela_origem()), t);
        }

        // Ordem pelo id: dentro de uma transação o occurred_at se repete, o id não.
        String sqlFila =
                "SELECT id, table_name, row_id, operation " +
                        "FROM replication_queue " +
                        "WHERE id > ? " +
                        "ORDER BY id " +
                        "LIMIT " + TAMANHO_LOTE;

        long ultimoId = checkpoint.ultimoIdProcessado(d.getId());
        int aplicados = 0;

        try (PreparedStatement pstFila = connOrigem.prepareStatement(sqlFila)) {
            while (true) {
                pstFila.setLong(1, ultimoId);
                int noLote = 0;

                try (ResultSet rsFila = pstFila.executeQuery()) {
                    while (rsFila.next()) {
                        noLote++;
                        long queueId = rsFila.getLong("id");
                        String tableName = rsFila.getString("table_name");
                        long rowId = rsFila.getLong("row_id");
                        String operation = rsFila.getString("operation");

                        TB_REPLICACAO_PROCESSO_TABELA t = configPorTabela.get(nomeSemSchema(tableName));
                        if (t != null) {
                            System.out.println("Fila: tabela=" + tableName + " id=" + rowId + " op=" + operation);
                            aplicarEvento(connOrigem, connDestino, SqlSeguro.tabela(t.getTabela_origem()),
                                    SqlSeguro.tabela(t.getTabela_destino()), rowId, operation);
                            aplicados++;
                        }
                        ultimoId = queueId;
                    }
                }

                if (noLote == 0) {
                    break;
                }
                // Avança o ponto de parada a cada lote. Se cair no meio, o lote é reaplicado no próximo ciclo,
                // o que é seguro porque cada evento grava no destino o estado atual da linha na origem.
                checkpoint.salvar(d.getId(), ultimoId);
                if (noLote < TAMANHO_LOTE) {
                    break;
                }
            }
        }

        System.out.println("Fila: " + aplicados + " alteração(ões) aplicada(s).");
    }

    private void aplicarEvento(Connection connOrigem, Connection connDestino, String tabelaOrigem,
                               String tabelaDestino, long rowId, String operation) throws SQLException {

        if ("D".equalsIgnoreCase(operation)) {
            apagarNoDestino(connDestino, tabelaDestino, rowId);
            return;
        }
        if (!"I".equalsIgnoreCase(operation) && !"U".equalsIgnoreCase(operation)) {
            System.out.println("Operação desconhecida na fila: " + operation + ". Ignorando.");
            return;
        }

        // Insert e update: lê o estado atual na origem e grava no destino (upsert pelo id).
        try (PreparedStatement pstSel = connOrigem.prepareStatement("SELECT * FROM " + tabelaOrigem + " WHERE id = ?")) {
            pstSel.setLong(1, rowId);

            try (ResultSet rsRow = pstSel.executeQuery()) {
                if (!rsRow.next()) {
                    // A linha já não existe na origem: apaga no destino também.
                    apagarNoDestino(connDestino, tabelaDestino, rowId);
                    return;
                }

                ResultSetMetaData md = rsRow.getMetaData();
                int colCount = md.getColumnCount();

                StringBuilder colNames = new StringBuilder();
                StringBuilder qs = new StringBuilder();
                StringBuilder set = new StringBuilder();

                for (int i = 1; i <= colCount; i++) {
                    String col = SqlSeguro.coluna(md.getColumnLabel(i));
                    if (i > 1) {
                        colNames.append(", ");
                        qs.append(", ");
                    }
                    colNames.append(col);
                    qs.append("?");

                    if (!"id".equalsIgnoreCase(md.getColumnLabel(i))) {
                        if (!set.isEmpty()) set.append(", ");
                        set.append(col).append(" = EXCLUDED.").append(col);
                    }
                }

                String sqlUpsert = "INSERT INTO " + tabelaDestino + " (" + colNames + ") VALUES (" + qs + ") "
                        + (set.isEmpty() ? "ON CONFLICT (id) DO NOTHING" : "ON CONFLICT (id) DO UPDATE SET " + set);

                try (PreparedStatement pstUpsert = connDestino.prepareStatement(sqlUpsert)) {
                    for (int i = 1; i <= colCount; i++) {
                        pstUpsert.setObject(i, rsRow.getObject(i));
                    }
                    pstUpsert.executeUpdate();
                }
            }
        }
    }

    private void apagarNoDestino(Connection connDestino, String tabelaDestino, long rowId) throws SQLException {
        try (PreparedStatement pstDel = connDestino.prepareStatement("DELETE FROM " + tabelaDestino + " WHERE id = ?")) {
            pstDel.setLong(1, rowId);
            pstDel.executeUpdate();
        }
    }

    private static int somar(int[] resultados) {
        int total = 0;
        for (int r : resultados) {
            if (r > 0) total += r;
        }
        return total;
    }

    private static String nomeSemSchema(String tabela) {
        if (tabela == null) return "";
        String nome = tabela.trim().toLowerCase(Locale.ROOT);
        int ponto = nome.lastIndexOf('.');
        return ponto >= 0 ? nome.substring(ponto + 1) : nome;
    }

    // Origem e destino para log, com senha escondida caso alguma URL antiga tenha password=...
    private static String descricao(TB_REPLICACAO_DIRECAO d) {
        return UrlJdbc.mascarar(d.getDirecao_origem()) + " <--> " + UrlJdbc.mascarar(d.getDirecao_destino());
    }
}
