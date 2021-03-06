package client;

import game.Game;
import sounds.SoundManager;
import util.Logger;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.Executors;

public class ClientManager {

    protected final String username;
    protected final Game game = new Game();
    protected final InputManager im = new InputManager(this);
    private final String ip;
    private final int portNumber;
    private final ClientUI sc = new ClientUI(this);

    private PrintWriter out;
    private ObjectInputStream in;

    public ClientManager(String username, String ip, int portNumber) {
        this.username = username;
        this.ip = ip;
        this.portNumber = portNumber;

        JFrame fr = new JFrame("BoomerParty (" + username + ")");
        fr.add(sc);
        fr.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?",
                        "Exit", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    fr.dispose();
                    disconnect();
                }
            }
        });
        fr.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        fr.pack();
        fr.setLocationRelativeTo(null);
        fr.setVisible(true);

        //game.players.add(new Player(this.username));
    }

    public void connect() throws IOException {
        Socket server = new Socket(ip, portNumber);

        Logger.log("successfully connected to server " + ip + " on port " + portNumber);

        out = new PrintWriter(server.getOutputStream(), true);
        in = new ObjectInputStream(server.getInputStream());

        Logger.log("sending username");
        send(username);

        Executors.newSingleThreadExecutor().execute(() -> {
            while (server.isConnected()) {
                Game game = (Game) receive();
                //update game info
                this.game.copy(game);

                SoundManager.playSound(game.soundToPlay);

                SwingUtilities.invokeLater(sc::updateInfo);
            }
        });
    }

    public void disconnect() {
        try {
            out.close();
            in.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String word) {
        out.println(word);
        Logger.log("sent: " + word);
    }

    public Object receive() {
        try {
            Object obj = in.readObject();
            Logger.log("received: " + obj.toString());
            return obj;
        } catch (IOException e) {
            Logger.log("IO error occurred when trying to receive game");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            Logger.log("can't find class");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientManager that = (ClientManager) o;
        return Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
