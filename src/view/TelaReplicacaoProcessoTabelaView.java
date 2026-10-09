package view;

import database.dao.ProcessoTabelaDAO;
import database.dao.ReplicacaoProcessoDAO;
import database.model.TB_REPLICACAO_PROCESSO;
import database.model.TB_REPLICACAO_PROCESSO_TABELA;

import javax.swing.*;
import java.sql.Connection;
import java.util.ArrayList;

import static java.awt.AWTEventMulticaster.add;

public class TelaReplicacaoProcessoTabelaView extends JFrame{

    private JTextField txtId;
    private JComboBox<TB_REPLICACAO_PROCESSO> cbProcesso;
    private JTextField txtTabelaOrigem;
    private JTextField txtTabelaDestino;
    private JTextField txtOrdem;
    private JCheckBox chkHabilitado;
    private JTextArea txtWhere;

    private JButton btnBuscar;
    private JButton btnAdicionar;
    private JButton btnSalvar;
    private JButton btnExcluir;

    public TelaReplicacaoProcessoTabelaView(){
        setTitle("Cadastro de tabelas");
        setSize(720, 420);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        add(panel);

        // BOTÕES TOPO
        btnBuscar = new JButton("BUSCAR");
        btnBuscar.setBounds(10, 10, 160, 30);
        panel.add(btnBuscar);

        btnAdicionar = new JButton("ADICIONAR");
        btnAdicionar.setBounds(180, 10, 160, 30);
        panel.add(btnAdicionar);

        btnSalvar = new JButton("SALVAR");
        btnSalvar.setBounds(350, 10, 160, 30);
        panel.add(btnSalvar);

        btnExcluir = new JButton("EXCLUIR");
        btnExcluir.setBounds(520, 10, 160, 30);
        panel.add(btnExcluir);

        // ID
        JLabel lblId = new JLabel("ID:");
        lblId.setBounds(10, 70, 140, 25);
        panel.add(lblId);

        txtId = new JTextField();
        txtId.setBounds(160, 70, 220, 25);
        panel.add(txtId);

        // PROCESSO
        JLabel lblProcesso = new JLabel("PROCESSO:");
        lblProcesso.setBounds(10, 105, 140, 25);
        panel.add(lblProcesso);

        cbProcesso = new JComboBox<>();
        cbProcesso.setBounds(160, 105, 520, 25);
        panel.add(cbProcesso);

        // TABELA ORIGEM
        JLabel lblTabelaOrigem = new JLabel("TABELA_ORIGEM:");
        lblTabelaOrigem.setBounds(10, 140, 140, 25);
        panel.add(lblTabelaOrigem);

        txtTabelaOrigem = new JTextField();
        txtTabelaOrigem.setBounds(160, 140, 520, 25);
        panel.add(txtTabelaOrigem);

        // TABELA DESTINO
        JLabel lblTabelaDestino = new JLabel("TABELA_DESTINO:");
        lblTabelaDestino.setBounds(10, 175, 140, 25);
        panel.add(lblTabelaDestino);

        txtTabelaDestino = new JTextField();
        txtTabelaDestino.setBounds(160, 175, 520, 25);
        panel.add(txtTabelaDestino);

        // ORDEM
        JLabel lblOrdem = new JLabel("ORDEM:");
        lblOrdem.setBounds(10, 210, 140, 25);
        panel.add(lblOrdem);

        txtOrdem = new JTextField();
        txtOrdem.setBounds(160, 210, 220, 25);
        panel.add(txtOrdem);

        // HABILITADO
        JLabel lblHabilitado = new JLabel("HABILITADO:");
        lblHabilitado.setBounds(10, 245, 140, 25);
        panel.add(lblHabilitado);

        chkHabilitado = new JCheckBox("Sim");
        chkHabilitado.setBounds(160, 245, 80, 25);
        panel.add(chkHabilitado);

        // WHERE
        JLabel lblWhere = new JLabel("DS_WHERE:");
        lblWhere.setBounds(10, 280, 140, 25);
        panel.add(lblWhere);

        txtWhere = new JTextArea();
        JScrollPane scrollWhere = new JScrollPane(txtWhere);
        scrollWhere.setBounds(160, 280, 520, 80);
        panel.add(scrollWhere);

        // CARREGA COMBO PROCESSOS
        cbProcesso.removeAllItems();
    }

    public static void main(String[] args){
        new TelaReplicacaoProcessoTabelaView().setVisible(true);
    }
}
