package seguranca;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografa as senhas de origem e destino gravadas na TB_REPLICACAO_DIRECAO.
 *
 * Usa AES-256-GCM: além de esconder a senha, o GCM detecta se o valor gravado foi alterado.
 * A chave vem da variável de ambiente REPLICADOR_CHAVE (32 bytes em Base64), então quem tiver
 * acesso só ao banco de controle (um dump, um backup) não consegue recuperar as senhas.
 *
 * Formato gravado: "enc:v1:" + Base64(IV de 12 bytes + texto cifrado com a tag).
 */
public final class CriptografiaSenha {

    public static final String VAR_CHAVE = "REPLICADOR_CHAVE";

    private static final String PREFIXO = "enc:v1:";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_CHAVE = 32;
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private static volatile SecretKey chave;

    private CriptografiaSenha() {
    }

    /** Lê e valida a chave. Chamado na partida, para falhar antes de abrir qualquer tela ou conexão. */
    public static void inicializar() {
        String valor = System.getenv(VAR_CHAVE);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Variável de ambiente ausente ou vazia: " + VAR_CHAVE
                    + ". Veja o arquivo .env.example.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(valor.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(VAR_CHAVE + " não está em Base64 válido.");
        }
        if (bytes.length != TAMANHO_CHAVE) {
            throw new IllegalStateException(VAR_CHAVE + " precisa ter 32 bytes em Base64 (tem " + bytes.length + ").");
        }
        chave = new SecretKeySpec(bytes, "AES");
    }

    public static String criptografar(String senha) {
        if (senha == null || senha.isEmpty()) {
            return senha;
        }
        try {
            // IV novo a cada senha: a mesma senha gravada duas vezes gera textos diferentes.
            byte[] iv = new byte[TAMANHO_IV];
            ALEATORIO.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, chave(), new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(senha.getBytes(StandardCharsets.UTF_8));

            byte[] saida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIXO + Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao criptografar a senha.", e);
        }
    }

    public static String descriptografar(String valor) {
        if (valor == null || valor.isEmpty() || !valor.startsWith(PREFIXO)) {
            // Vazio, ou registro gravado antes da criptografia: devolve como está.
            return valor;
        }
        try {
            byte[] dados = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
            if (dados.length <= TAMANHO_IV) {
                throw new IllegalStateException("Senha gravada em formato inválido.");
            }

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, chave(), new GCMParameterSpec(TAMANHO_TAG_BITS, dados, 0, TAMANHO_IV));
            byte[] senha = cipher.doFinal(dados, TAMANHO_IV, dados.length - TAMANHO_IV);
            return new String(senha, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new IllegalStateException("A senha gravada não confere com a " + VAR_CHAVE
                    + " atual (chave trocada ou valor alterado no banco).");
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Senha gravada em formato inválido.", e);
        }
    }

    /** True quando há uma senha gravada sem criptografia (cadastro antigo). */
    public static boolean estaEmTextoPuro(String valor) {
        return valor != null && !valor.isEmpty() && !valor.startsWith(PREFIXO);
    }

    private static SecretKey chave() {
        SecretKey atual = chave;
        if (atual == null) {
            throw new IllegalStateException("Criptografia não inicializada. Chame CriptografiaSenha.inicializar() na partida.");
        }
        return atual;
    }
}
