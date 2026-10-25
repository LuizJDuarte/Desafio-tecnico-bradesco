package seguranca;

import java.util.Locale;
import java.util.regex.Pattern;

/** Regras para as URLs JDBC cadastradas nas direções. */
public final class UrlJdbc {

    private static final Pattern SENHA_NA_URL = Pattern.compile("(?i)([?&]password=)[^&]*");

    private UrlJdbc() {
    }

    /**
     * Aceita só URL do PostgreSQL (o único driver do projeto) e sem senha embutida.
     * A senha tem campo próprio, gravado criptografado; dentro da URL ela ficaria em texto puro.
     */
    public static void validar(String url) {
        if (url == null || !url.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql:")) {
            throw new IllegalArgumentException("A URL deve começar com jdbc:postgresql:");
        }
        if (SENHA_NA_URL.matcher(url).find()) {
            throw new IllegalArgumentException("Não coloque a senha na URL. Use o campo de senha, que é gravado criptografado.");
        }
    }

    /** URL para log e tela, com password=... escondido caso exista em algum cadastro antigo. */
    public static String mascarar(String url) {
        return url == null ? null : SENHA_NA_URL.matcher(url).replaceAll("$1***");
    }
}
