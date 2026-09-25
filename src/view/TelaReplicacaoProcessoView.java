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

        JPanel panel = new JPanel();
        panel.setLayout(null);
        add(panel);

        btnBuscar = new JButton("BUSCAR");
        btnAdicionar = new JButton("ADICIONAR");
        btnSalvar = new JButton("SALVAR");
        btnExcluir = new JButton("EXCLUIR");

        btnBuscar.setBounds(10, 10, 130, 30);
        btnAdicionar.setBounds(150,10,130,30);
        btnSalvar.setBounds(300,10,130,30);
        btnExcluir.setBounds(450,10,130,30);
    }
}
