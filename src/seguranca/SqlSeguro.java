package seguranca;

import java.util.regex.Pattern;

/**
 * Validação dos trechos de SQL que vêm da configuração e não podem ir como parâmetro (?).
 *
 * O PreparedStatement só parametriza valores. Nome de tabela, nome de coluna e o filtro DS_WHERE
 * precisam ser concatenados no SQL, então cada um passa por aqui antes, para não abrir SQL injection.
 */
public final class SqlSeguro {

    // tabela ou schema.tabela: letras, números e _, começando por letra ou _ (limite de 63 do PostgreSQL)
    private static final Pattern NOME_TABELA =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}(\\.[A-Za-z_][A-Za-z0-9_]{0,62})?");

    private SqlSeguro() {
    }

    /** Devolve o nome da tabela pronto para o SQL, ou lança erro se tiver qualquer caractere fora da lista. */
    public static String tabela(String nome) {
        String valor = nome == null ? "" : nome.trim();
        if (!NOME_TABELA.matcher(valor).matches()) {
            throw new IllegalArgumentException("Nome de tabela inválido: \"" + nome
                    + "\". Use só letras, números e _ (ou schema.tabela).");
        }
        return valor;
    }

    /**
     * Coluna vinda dos metadados do banco, entre aspas duplas.
     * As aspas aceitam maiúsculas e palavras reservadas; aspas internas são duplicadas para não fechar o nome.
     */
    public static String coluna(String nome) {
        return "\"" + nome.replace("\"", "\"\"") + "\"";
    }

    /**
     * Monta a cláusula WHERE a partir do DS_WHERE. Vazio significa tabela inteira.
     * Sem ';' e sem comentários, o filtro não consegue encerrar o SELECT e emendar outro comando.
     */
    public static String where(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return "";
        }
        if (filtro.contains(";") || filtro.contains("--") || filtro.contains("/*")) {
            throw new IllegalArgumentException("DS_WHERE não pode conter ';', '--' ou '/*'.");
        }
        return " WHERE (" + filtro.trim() + ")";
    }
}
