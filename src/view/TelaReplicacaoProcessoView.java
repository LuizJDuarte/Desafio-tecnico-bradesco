package view;

import javax.swing.*;

public class TelaReplicacaoProcessoView extends JFrame {
    private JTextField txfId;
    private JTextField txfProcesso;
    private JTextField txfDescricao;
    private JCheckBox chkHabilitado;

    private JButton btnSalvar;
    private JButton btnAdicionar;
    private JButton btnBuscar;
    private JButton btnExcluir;

    public TelaReplicacaoProcessoView(){
        setTitle("Cadastro de Processos");
        setSize(620,320);

        // Clicar no X, fecha a tela e encerra a operação
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Abre a janela no meio da tela do monitor
        setLocationRelativeTo(null);

        // Não permite redimensionar a tela
        setResizable(false);

        // Não vai utilizar Layout/ Não vai ser responsiva (anda em conjunto com a função de cima)
        setLayout(null);

        btnBuscar = new JButton("BUSCAR");
        btnAdicionar = new JButton("ADICIONAR");
        btnSalvar = new JButton("SALVAR");
        btnExcluir = new JButton("EXCLUIR");

        btnBuscar.setBounds(10, 10, 130, 30);
        btnAdicionar.setBounds(150,10,130,30);
        btnSalvar.setBounds(300,10,130,30);
        btnExcluir.setBounds(450,10,130,30);

        getContentPane().add(btnBuscar);
        getContentPane().add(btnAdicionar);
        getContentPane().add(btnSalvar);
        getContentPane().add(btnExcluir);

        JLabel lblId = new JLabel("ID:");
        lblId.setBounds(10, 70, 120, 25);
        getContentPane().add(lblId);

        txfId = new JTextField();
        txfId.setBounds(140, 70, 200, 25);
        getContentPane().add(txfId);

        JLabel lblProcesso = new JLabel("PROCESSO:");
        lblProcesso.setBounds(10, 105, 120, 25);
        getContentPane().add(lblProcesso);

        txfProcesso = new JTextField();
        txfProcesso.setBounds(140, 105, 420, 25);
        getContentPane().add(txfProcesso);

        JLabel lblDescricao = new JLabel("DESCRIÇÃO:");
        lblDescricao.setBounds(10, 140, 120, 25);
        getContentPane().add(lblDescricao);

        txfDescricao = new JTextField();
        txfDescricao.setBounds(140, 140, 420, 25);
        getContentPane().add(txfDescricao);

        JLabel lblHabilitado = new JLabel("HABILITADO:");
        lblHabilitado.setBounds(10, 175, 120, 25);
        getContentPane().add(lblHabilitado);

        chkHabilitado = new JCheckBox("Sim");
        chkHabilitado.setBounds(140, 175, 80, 25);
        getContentPane().add(chkHabilitado);

    }

    public static void main(String[] args){
        new TelaReplicacaoProcessoView().setVisible(true);
    }
}
