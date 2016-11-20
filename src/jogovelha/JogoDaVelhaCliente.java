package jogovelha;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class JogoDaVelhaCliente {

    private JFrame frame = new JFrame("Jogo da Velha");
    private JLabel messageLabel = new JLabel("");
    private ImageIcon icone;
    private ImageIcon iconeOponente;

    private Square[] tabuleiro = new Square[9];
    private Square pecaAtual;

    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    public JogoDaVelhaCliente(String serverAddress) throws Exception {

        //AMrcando o socket
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        //Layout da tela
        messageLabel.setBackground(Color.DARK_GRAY);
        frame.getContentPane().add(messageLabel, "South");

        JPanel tabuleiroPanel = new JPanel();
        tabuleiroPanel.setBackground(Color.black);
        tabuleiroPanel.setLayout(new GridLayout(3, 3, 2, 2));
        for (int i = 0; i < tabuleiro.length; i++) {
            final int j = i;
            tabuleiro[i] = new Square();
            tabuleiro[i].addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    pecaAtual = tabuleiro[j];
                    out.println("MOVE " + j);}});
            tabuleiroPanel.add(tabuleiro[i]);
        }
        frame.getContentPane().add(tabuleiroPanel, "Center");
    }
    
    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                icone = new ImageIcon(mark == 'X' ? "x.gif" : "o.gif");
                iconeOponente  = new ImageIcon(mark == 'X' ? "o.gif" : "x.gif");
                frame.setTitle("Jogo da Velha - Jogador: " + mark);
            }
            while (true) {
                response = in.readLine();
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Movimento válido, aguarde por favor");
                    pecaAtual.setIcon(icone);
                    pecaAtual.repaint();
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(response.substring(15));
                    tabuleiro[loc].setIcon(iconeOponente);
                    tabuleiro[loc].repaint();
                    messageLabel.setText("Oponente encerrou, sua vez!");
                } else if (response.startsWith("VICTORY")) {
                    messageLabel.setText("Você ganhou!");
                    int saida = JOptionPane.showConfirmDialog(frame,
                        "Você ganhou","",
                        JOptionPane.CLOSED_OPTION);
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    messageLabel.setText("Você perdeu!");
                    int saida = JOptionPane.showConfirmDialog(frame,
                        "Você perdeu","",
                        JOptionPane.CLOSED_OPTION) ;
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("You tied");
                    int saida = JOptionPane.showConfirmDialog(frame,
                        "Sem movimentos válidos","",
                        JOptionPane.CLOSED_OPTION);
                    break;
                } else if (response.startsWith("MESSAGE")) {
                    messageLabel.setText(response.substring(8));
                }
            }
            out.println("Saida");
        }
        finally {
            socket.close();
        }
    }
    

    private boolean jogarNovamente() {
        int response = JOptionPane.showConfirmDialog(frame,
            "Quer jogar novamente?","",
            JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }
    
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);

        public Square() {
            setBackground(Color.white);
            add(label);
        }

        public void setIcon(Icon icone) {
            label.setIcon(icone);
        }
    }
    
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[1];
            JogoDaVelhaCliente client = new JogoDaVelhaCliente(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(240, 160);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
            // if (!client.jogarNovamente()) {
                break;
            //}
        }
    }
}