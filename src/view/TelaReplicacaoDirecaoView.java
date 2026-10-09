package view;

import database.model.TB_REPLICACAO_PROCESSO;

import javax.swing.*;

public class TelaReplicacaoDirecaoView extends JFrame {

    private JTextField txtId;
    private JComboBox<TB_REPLICACAO_PROCESSO> cbProcesso;

    private JTextField txtDirecaoOrigem;
    private JTextField txtUsuarioOrigem;
    private JPasswordField txtSenhaOrigem;

    private JTextField txtDirecaoDestino;
    private JTextField txtUsuarioDestino;
    private JPasswordField txtSenhaDestino;

    private JCheckBox chkHabilitado;

    private JButton btnBuscar;
    private JButton btnAdicionar;
    private JButton btnSalvar;
    private JButton btnExcluir;

    public TelaReplicacaoDirecaoView(){
        setTitle("TB_REPLICACAO_DIRECAO");
        setSize(760, 460);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        add(panel);

        // BOTÕES
        btnBuscar = new JButton("BUSCAR");
        btnBuscar.setBounds(10, 10, 170, 30);
        panel.add(btnBuscar);

        btnAdicionar = new JButton("ADICIONAR");
        btnAdicionar.setBounds(190, 10, 170, 30);
        panel.add(btnAdicionar);

        btnSalvar = new JButton("SALVAR");
        btnSalvar.setBounds(370, 10, 170, 30);
        panel.add(btnSalvar);

        btnExcluir = new JButton("EXCLUIR");
        btnExcluir.setBounds(550, 10, 170, 30);
        panel.add(btnExcluir);

        // ID
        JLabel lblId = new JLabel("ID:");
        lblId.setBounds(10, 70, 140, 25);
        panel.add(lblId);

        txtId = new JTextField();
        txtId.setBounds(160, 70, 220, 25);
        panel.add(txtId);

        // PROCESSO (COMBO)
        JLabel lblProcesso = new JLabel("PROCESSO:");
        lblProcesso.setBounds(10, 105, 140, 25);
        panel.add(lblProcesso);

        cbProcesso = new JComboBox<>();
        cbProcesso.setBounds(160, 105, 320, 25);
        panel.add(cbProcesso);

        // HABILITADO
        JLabel lblHabilitado = new JLabel("HABILITADO:");
        lblHabilitado.setBounds(500, 105, 120, 25);
        panel.add(lblHabilitado);

        chkHabilitado = new JCheckBox("Sim");
        chkHabilitado.setBounds(620, 105, 80, 25);
        panel.add(chkHabilitado);

        // ORIGEM
        JLabel lblOrigemTitulo = new JLabel("ORIGEM");
        lblOrigemTitulo.setBounds(10, 150, 200, 25);
        lblOrigemTitulo.setFont(lblOrigemTitulo.getFont().deriveFont(java.awt.Font.BOLD));
        panel.add(lblOrigemTitulo);

        JLabel lblDirecaoOrigem = new JLabel("DIRECAO_ORIGEM:");
        lblDirecaoOrigem.setBounds(10, 185, 140, 25);
        panel.add(lblDirecaoOrigem);

        txtDirecaoOrigem = new JTextField();
        txtDirecaoOrigem.setBounds(160, 185, 560, 25);
        panel.add(txtDirecaoOrigem);

        JLabel lblUsuarioOrigem = new JLabel("USUARIO_ORIGEM:");
        lblUsuarioOrigem.setBounds(10, 220, 140, 25);
        panel.add(lblUsuarioOrigem);

        txtUsuarioOrigem = new JTextField();
        txtUsuarioOrigem.setBounds(160, 220, 560, 25);
        panel.add(txtUsuarioOrigem);

        JLabel lblSenhaOrigem = new JLabel("SENHA_ORIGEM:");
        lblSenhaOrigem.setBounds(10, 255, 140, 25);
        panel.add(lblSenhaOrigem);

        txtSenhaOrigem = new JPasswordField();
        txtSenhaOrigem.setBounds(160, 255, 560, 25);
        panel.add(txtSenhaOrigem);

        // DESTINO
        JLabel lblDestinoTitulo = new JLabel("DESTINO");
        lblDestinoTitulo.setBounds(10, 300, 200, 25);
        lblDestinoTitulo.setFont(lblDestinoTitulo.getFont().deriveFont(java.awt.Font.BOLD));
        panel.add(lblDestinoTitulo);

        JLabel lblDirecaoDestino = new JLabel("DIRECAO_DESTINO:");
        lblDirecaoDestino.setBounds(10, 335, 140, 25);
        panel.add(lblDirecaoDestino);

        txtDirecaoDestino = new JTextField();
        txtDirecaoDestino.setBounds(160, 335, 560, 25);
        panel.add(txtDirecaoDestino);

        JLabel lblUsuarioDestino = new JLabel("USUARIO_DESTINO:");
        lblUsuarioDestino.setBounds(10, 370, 140, 25);
        panel.add(lblUsuarioDestino);

        txtUsuarioDestino = new JTextField();
        txtUsuarioDestino.setBounds(160, 370, 280, 25);
        panel.add(txtUsuarioDestino);

        JLabel lblSenhaDestino = new JLabel("SENHA_DESTINO:");
        lblSenhaDestino.setBounds(450, 370, 120, 25);
        panel.add(lblSenhaDestino);

        txtSenhaDestino = new JPasswordField();
        txtSenhaDestino.setBounds(570, 370, 150, 25);
        panel.add(txtSenhaDestino);

        // CARREGA COMBO PROCESSOS
        cbProcesso.removeAllItems();
    }

    static void main(String[] args) {
        new TelaReplicacaoDirecaoView().setVisible(true);
    }
}
