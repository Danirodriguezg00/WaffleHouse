package server;

import java.net.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;

class SockBaseServer {
    public Game game;
    public Leaderboard leaderboard = new Leaderboard();
    public List<Leaderboard.Entry> playersOnline = new ArrayList<Leaderboard.Entry>();

    public SockBaseServer(Game game){
        this.game = game;
    }

    public static void main (String args[]) throws Exception {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }
        int port = 9099; // default port
        int sleepDelay = 10000; // default delay
        Socket clientSocket = null;
        ServerSocket serv = null;

        try {
            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }
        try {
            serv = new ServerSocket(port);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        Game mGame = new Game();
        mGame.newGame();

        while(true) {
            clientSocket = serv.accept();
            SockBaseServer server = new SockBaseServer(mGame);
            GameLogic session = new GameLogic(clientSocket, server);
            executor.submit(session);
        }
    }

}

