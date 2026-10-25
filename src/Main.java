import config.ConexaoBancoControle;
import seguranca.CriptografiaSenha;
import service.ReplicacaoExecutar;
import view.TelaReplicacaoDirecaoView;
import view.TelaReplicacaoProcessoTabelaView;
import view.TelaReplicacaoProcessoView;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;

public class Main extends JFrame {

    // Intervalo entre o fim de um ciclo de replicação e o início do próximo.
    private static final long INTERVALO_MS = 60000;

    private JDesktopPane desktop;
    private static Connection conn;
    private static Thread threadReplicacao;

    public Main() {
        setTitle("Sistema de Replicação de Dados");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        desktop = new JDesktopPane();
        setContentPane(desktop);

        JMenu menuSistema = new JMenu("Sistema");

        JMenuItem itemExecutar = new JMenuItem("Executar Replicação");
        itemExecutar.addActionListener(e -> {
            if (!iniciarReplicacao()) {
                JOptionPane.showMessageDialog(this, "A replicação já está em execução.");
            }
        });
        menuSistema.add(itemExecutar);

        JMenuItem itemParar = new JMenuItem("Parar Replicação");
        itemParar.addActionListener(e -> {
            if (pararReplicacao()) {
                JOptionPane.showMessageDialog(this, "A replicação vai parar ao fim do ciclo atual.");
            } else {
                JOptionPane.showMessageDialog(this, "A replicação não está em execução.");
            }
        });
        menuSistema.add(itemParar);

        JMenuItem itemSair = new JMenuItem("Sair");
        itemSair.addActionListener(e -> {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            System.exit(0);
        });
        menuSistema.add(itemSair);

        JMenu menuCadastro = new JMenu("Cadastro");

        JMenuItem itemProcesso = new JMenuItem("Processos");
        itemProcesso.addActionListener(e -> {
            abrirTelaInternaProcessos();
        });
        menuCadastro.add(itemProcesso);

        JMenuItem itemProcessoTabela = new JMenuItem("Processo x Tabelas");
        itemProcessoTabela.addActionListener(e -> {
            abrirTelaInternaProcessoTabela();
        });
        menuCadastro.add(itemProcessoTabela);

        JMenuItem itemDirecao = new JMenuItem("Direções");
        itemDirecao.addActionListener(e -> {
            abrirTelaInternaDirecoes();
        });
        menuCadastro.add(itemDirecao);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menuSistema);
        menuBar.add(menuCadastro);
        setJMenuBar(menuBar);
    }

    private void abrirTelaInternaProcessos() {
        try {
            TelaReplicacaoProcessoView tela = new TelaReplicacaoProcessoView(conn);

            JInternalFrame internalFrame = new JInternalFrame("Processos", true, true, true, true);
            internalFrame.setSize(650, 360);
            internalFrame.setLayout(new BorderLayout());
            internalFrame.add(tela.getContentPane(), BorderLayout.CENTER);
            internalFrame.setVisible(true);
            desktop.add(internalFrame);
            internalFrame.setSelected(true);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao abrir tela: " + ex.getMessage());
        }
    }

    private void abrirTelaInternaDirecoes() {
        try {
            TelaReplicacaoDirecaoView tela = new TelaReplicacaoDirecaoView(conn);

            JInternalFrame internalFrame = new JInternalFrame("Cadastro de Direções", true, true, true, true);
            internalFrame.setSize(820, 520);
            internalFrame.setLayout(new BorderLayout());
            internalFrame.add(tela.getContentPane(), BorderLayout.CENTER);
            internalFrame.setVisible(true);
            desktop.add(internalFrame);
            internalFrame.setSelected(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao abrir tela: " + e.getMessage());
        }
    }

    private void abrirTelaInternaProcessoTabela() {
        try {
            TelaReplicacaoProcessoTabelaView tela = new TelaReplicacaoProcessoTabelaView(conn);

            JInternalFrame internalFrame = new JInternalFrame("Cadastro de Processos x Tabela", true, true, true, true);
            internalFrame.setSize(720, 500);
            internalFrame.setLayout(new BorderLayout());
            internalFrame.add(tela.getContentPane(), BorderLayout.CENTER);
            internalFrame.setVisible(true);
            desktop.add(internalFrame);
            internalFrame.setSelected(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao abrir tela: " + e.getMessage());
        }
    }

    /**
     * Sobe a thread que repete a replicação a cada INTERVALO_MS.
     * Retorna false se já houver uma rodando: antes, cada clique em "Executar" criava mais uma thread,
     * e as duas processavam a mesma fila ao mesmo tempo.
     */
    private static synchronized boolean iniciarReplicacao() {
        if (threadReplicacao != null && threadReplicacao.isAlive()) {
            return false;
        }

        threadReplicacao = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                new ReplicacaoExecutar(conn);
                try {
                    Thread.sleep(INTERVALO_MS);
                } catch (InterruptedException ex) {
                    break; // parada pedida pelo menu
                }
            }
            System.out.println("Replicação parada.");
        }, "replicacao");
        threadReplicacao.start();
        return true;
    }

    /** Pede a parada. O ciclo em andamento termina normalmente e a thread sai em seguida. */
    private static synchronized boolean pararReplicacao() {
        if (threadReplicacao == null || !threadReplicacao.isAlive()) {
            return false;
        }
        threadReplicacao.interrupt();
        return true;
    }

    public static void main(String[] args) {

        if (args != null && args.length > 0) {
            if (args[0].equalsIgnoreCase("servico=yes")) {
                try {
                    CriptografiaSenha.inicializar();
                    conn = ConexaoBancoControle.abrir();
                    iniciarReplicacao();
                } catch (IllegalStateException | SQLException e) {
                    System.err.println("Não foi possível iniciar o replicador: " + e.getMessage());
                    System.exit(1);
                }
            }
            else {
                try {
                    CriptografiaSenha.inicializar();
                    conn = ConexaoBancoControle.abrir();
                    SwingUtilities.invokeLater(() -> new Main().setVisible(true));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Não foi possível iniciar o replicador.\n" + e.getMessage());
                    System.exit(1);
                }
            }
        }
        else {
            System.out.println("Configure os parâmetros do replicador:");
            System.out.println("  servico=yes  executa a replicação em segundo plano, sem interface");
            System.out.println("  servico=no   abre a interface de cadastro");
            System.exit(1);
        }
    }
}