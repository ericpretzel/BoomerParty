package game;

import server.ClientHandler;
import util.Globals;
import util.HashMap;
import util.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameManager implements Runnable {
    public final HashMap<Player, ClientHandler> clients = new HashMap<>();
    public final Game game = new Game();
    private final WordManager wm = new WordManager();
    private Player currentPlayer;

    public void addPlayer(ClientHandler client) {
        Player player = new Player(client.username);
        game.players.add(player);
        clients.put(player, client);
        broadcast();
    }

    public void removePlayer(ClientHandler client) {
        Player player = new Player(client.username);
        if (player.equals(currentPlayer)) {
            currentPlayer.health = 0;
            nextPlayer();
        } else {
            game.players.remove(player);
        }
        //clients.put(player, null);
        broadcast();
    }

    public void run() {
        game.isRunning = true;
        game.prompt = wm.getPrompt();
        currentPlayer = game.players.get(0);
        currentPlayer.myTurn = true;

        startNewTimer();
    }

    void startNewTimer() {
        game.timer = 10;
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(() -> {
            game.timer--;
            if (game.timer < 0) {
                timer.shutdownNow();
                currentPlayer.health--;
                game.soundToPlay = Globals.LOSE_SOUND;
                nextPlayer();
            }
            broadcast();
        }, 0, 1, TimeUnit.SECONDS);
        new Thread(() -> {
            final ClientHandler client = clients.get(currentPlayer).get(0);
            while (!timer.isShutdown()) {
                Logger.log("awaiting word");
                String word = client.receive();
                Logger.log(word);
                currentPlayer.playedWord = word;
                if (wm.check(word)) {
                    timer.shutdownNow();
                    game.soundToPlay = Globals.VALID_WORD_SOUND;
                    nextPlayer();
                    return;
                } else {
                    game.soundToPlay = Globals.INVALID_WORD_SOUND;
                }
                broadcast();
            }
        }).start();
    }

    void nextPlayer() {
        game.players.remove(currentPlayer);
        currentPlayer.myTurn = false;
        if (currentPlayer.health > 0)
            game.players.add(currentPlayer);

        game.isRunning = game.players.size() >= 2;
        if (game.isRunning) {
            currentPlayer = game.players.get(0); //new currentPlayer
            currentPlayer.myTurn = true;
            currentPlayer.playedWord = "";
            game.prompt = wm.getPrompt();

            startNewTimer();
        } else {
            broadcast();
        }
    }

    synchronized void broadcast() {
        for (Player p : clients.getKeys()) {
            ClientHandler client = clients.get(p).get(0);
            client.send(game);
            game.soundToPlay = "";
        }
    }
}
