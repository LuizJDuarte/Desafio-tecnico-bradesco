package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abre a conexão com o Banco de Controle (Gerencial).
 *
 * URL, usuário e senha vêm de variáveis de ambiente, então nenhum dado de conexão
 * fica no código-fonte nem no repositório. Para rodar localmente, copie o
 * .env.example para .env e aponte a configuração de execução do IntelliJ para ele.
 */
public final class ConexaoBancoControle {

    public static final String VAR_URL = "REPLICADOR_CONTROLE_URL";
    public static final String VAR_USUARIO = "REPLICADOR_CONTROLE_USUARIO";
    public static final String VAR_SENHA = "REPLICADOR_CONTROLE_SENHA";

    private ConexaoBancoControle() {
    }

    public static Connection abrir() throws SQLException {
        List<String> ausentes = new ArrayList<>();

        String url = ler(VAR_URL, ausentes);
        String usuario = ler(VAR_USUARIO, ausentes);
        String senha = ler(VAR_SENHA, ausentes);

        // Valida as três antes de conectar e informa todas as que faltam de uma vez.
        if (!ausentes.isEmpty()) {
            throw new IllegalStateException("Variáveis de ambiente ausentes ou vazias: "
                    + String.join(", ", ausentes) + ". Veja o arquivo .env.example.");
        }

        return DriverManager.getConnection(url, usuario, senha);
    }

    private static String ler(String nome, List<String> ausentes) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) {
            ausentes.add(nome);
        }
        return valor;
    }
}
