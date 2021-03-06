package server;

import util.Logger;

import java.io.*;
import java.net.Socket;

public class ClientHandler {

    public final String username;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final BufferedReader in;

    ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new BufferedReader(new InputStreamReader((socket.getInputStream())));
        //this.send(new Game());
        this.username = this.receive();
    }

    public void disconnect() {
        Logger.log("\"" + username + "\" has disconnected");
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean connected() {
        return this.socket.isConnected();
    }


    public boolean ready() {
        try {
            return in.ready();
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log("\"" + username + "\" is not ready :(");
        }
        return false;
    }

    public String receive() {
        try {
            String str = in.readLine();
            Logger.log("received from \"" + (username == null ? "(no username)" : username) + "\": " + str);
            return str;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void send(Object obj) {
        try {
            out.writeObject(obj);
            Logger.log("sent to \"" + (username == null ? "(no username)" : username) + "\": " + obj.toString());
            out.reset();
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "ClientHandler{" +
                "username='" + username + '\'' +
                ", socket=" + socket +
                '}';
    }
}