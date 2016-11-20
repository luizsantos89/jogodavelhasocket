/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jogovelha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A server for a network multi-jogador tic tac toe game.  Modified and
 * extended from the class presented in Deitel and Deitel "Java How to
 * Program" book.  I made a bunch of enhancements and rewrote large sections
 * of the code.  The main change is instead of passing *data* between the
 * client and server, I made a TTTP (tic tac toe protocol) which is totally
 * plain text, so you can test the game with Telnet (always a good idea.)
 * The strings that are sent in TTTP are:
 *
 *  Client -> Server           Server -> Client
 *  ----------------           ----------------
 *  MOVE <n>  (0 <= n <= 8)    WELCOME <char>  (char in {X, O})
 *  QUIT                       VALID_MOVE
 *                             OTHER_PLAYER_MOVED <n>
 *                             VICTORY
 *                             DEFEAT
 *                             TIE
 *                             MESSAGE <text>
 *
 * A second change is that it allows an unlimited number of pairs of
 * jogadors to play.
 */
public class JogoDaVelhaServer {

    /**
     * Runs the application. Pairs up clients that connect.
     */
    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(8901);
        System.out.println("Jogo da velha está em execução");
        try {
            while (true) {
                Game game = new Game();
                Game.Player jogadorX = game.new Player(listener.accept(), 'X');
                Game.Player jogadorO = game.new Player(listener.accept(), 'O');
                jogadorX.setOpponent(jogadorO);
                jogadorO.setOpponent(jogadorX);
                game.jogadorAtual = jogadorX;
                jogadorX.start();
                jogadorO.start();
            }
        } finally {
            listener.close();
        }
    }
}

/**
 * A two-jogador game.
 */
class Game {

    /**
     * A tabuleiro has nine squares.  Each square is either unowned or
     * it is owned by a jogador.  So we use a simple array of jogador
     * references.  If null, the corresponding square is unowned,
     * otherwise the array cell stores a reference to the jogador that
     * owns it.
     */
    private Player[] tabuleiro = {
        null, null, null,
        null, null, null,
        null, null, null};

    /**
     * The current jogador.
     */
    Player jogadorAtual;

    /**
     * Returns whether the current state of the tabuleiro is such that one
     * of the jogadors is a winner.
     */
    public boolean temVencedor() {
        return
            (tabuleiro[0] != null && tabuleiro[0] == tabuleiro[1] && tabuleiro[0] == tabuleiro[2])
          ||(tabuleiro[3] != null && tabuleiro[3] == tabuleiro[4] && tabuleiro[3] == tabuleiro[5])
          ||(tabuleiro[6] != null && tabuleiro[6] == tabuleiro[7] && tabuleiro[6] == tabuleiro[8])
          ||(tabuleiro[0] != null && tabuleiro[0] == tabuleiro[3] && tabuleiro[0] == tabuleiro[6])
          ||(tabuleiro[1] != null && tabuleiro[1] == tabuleiro[4] && tabuleiro[1] == tabuleiro[7])
          ||(tabuleiro[2] != null && tabuleiro[2] == tabuleiro[5] && tabuleiro[2] == tabuleiro[8])
          ||(tabuleiro[0] != null && tabuleiro[0] == tabuleiro[4] && tabuleiro[0] == tabuleiro[8])
          ||(tabuleiro[2] != null && tabuleiro[2] == tabuleiro[4] && tabuleiro[2] == tabuleiro[6]);
    }

    /**
     * Returns whether there are no more empty squares.
     */
    public boolean tabuleiroCheio() {
        for (int i = 0; i < tabuleiro.length; i++) {
            if (tabuleiro[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called by the jogador threads when a jogador tries to make a
     * move.  This method checks to see if the move is legal: that
     * is, the jogador requesting the move must be the current jogador
     * and the square in which she is trying to move must not already
     * be occupied.  If the move is legal the game state is updated
     * (the square is set and the next jogador becomes current) and
     * the other jogador is notified of the move so it can update its
     * client.
     */
    public synchronized boolean movimentoLegal(int location, Player jogador) {
        if (jogador == jogadorAtual && tabuleiro[location] == null) {
            tabuleiro[location] = jogadorAtual;
            jogadorAtual = jogadorAtual.oponente;
            jogadorAtual.otherPlayerMoved(location);
            return true;
        }
        return false;
    }

    /**
     * The class for the helper threads in this multithreaded server
     * application.  A Player is identified by a character mark
     * which is either 'X' or 'O'.  For communication with the
     * client the jogador has a socket with its input and output
     * streams.  Since only text is being communicated we use a
     * reader and a writer.
     */
    class Player extends Thread {
        char mark;
        Player oponente;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Aguardando conexão do oponente");
            } catch (IOException e) {
                System.out.println("Jogador morreu: " + e);
            }
        }

        public void setOpponent(Player oponente) {
            this.oponente = oponente;
        }

        public void otherPlayerMoved(int location) {
            output.println("OPPONENT_MOVED " + location);
            output.println(
                temVencedor() ? "DEFEAT" : tabuleiroCheio() ? "TIE" : "");
        }

        public void run() {
            try {
                
                output.println("MESSAGE Os dois jogadores estão conectados");

                
                if (mark == 'X') {
                    output.println("MESSAGE Sua vez");
                }

                
                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        if (movimentoLegal(location, this)) {
                            output.println("VALID_MOVE");
                            output.println(temVencedor() ? "VICTORY"
                                         : tabuleiroCheio() ? "TIE"
                                         : "");
                        } else {
                            output.println("MESSAGE Peça já escolhida, favor escolher outra");
                        }
                    } else if (command.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Jogador morto: " + e);
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }
    }
}
