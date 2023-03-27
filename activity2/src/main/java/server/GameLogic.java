package server;

import buffers.RequestProtos;
import buffers.ResponseProtos;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import buffers.ResponseProtos.Response;

public class GameLogic extends Thread {

    private Socket conn;
    private String name;
    private SockBaseServer server;

    InputStream in = null;
    OutputStream out = null;

    Socket clientSocket = null;

    Game game;

    String gError;

    static String logFilename = "logs.txt";

    public GameLogic(Socket sock, SockBaseServer s) {
        this.conn = sock;
        this.server= s;
        this.game = s.game;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }

    }
    public void startSession() {
        boolean quit = false;
        OutputStream out = null;
        InputStream in = null;
        try {
            out = conn.getOutputStream();
            in = conn.getInputStream();
            System.out.println("Server connected to client:");
            while (!quit) {
                //RECEIVE MESSAGE
                //STORE MESSAGE
                RequestProtos.Request op = RequestProtos.Request.parseDelimitedFrom(in);

                //DECLARE MESSAGE WE ARE RETURNING
                Response response = null;


                //SAVE CHOICE
                RequestProtos.Request.OperationType choice = op.getOperationType();

                //SWITCH ON CHOICE
               if (choice == RequestProtos.Request.OperationType.NAME)
               {
                   name = op.getName();

                   // writing a connect message to the log with name and CONNENCT
                   writeToLog(name, RequestProtos.Message.CONNECT);
                   System.out.println("Got a connection and a name: " + name);

                   if(server.leaderboard.playerNameExists(name)){
                       server.leaderboard.getPlayerByName(name).addLogin();
                   }
                   else{
                       server.leaderboard.addPlayer(name, 0,1);
                   }

                   server.playersOnline.add(server.leaderboard.getPlayerByName(name));

                   response = ResponseProtos.Response.newBuilder()
                        .setResponseType(ResponseProtos.Response.ResponseType.GREETING)
                        .setMessage("Hello " + name + " Welcome to Memory Tile Game Extravaganza!")
                        .setBoard(game.getBoard())
                        .build();

               }
                else if (choice == RequestProtos.Request.OperationType.LEADER)
                {
                    //Create Entry
                    response = getPlayerListAsProtoBuf(server.leaderboard).build();

                }
                else if (choice == RequestProtos.Request.OperationType.NEW)
                {
                    //Create Game if isn't started already
                    if(game.getWon())
                        server.game.newGame();

                    game = server.game;
                    response = ResponseProtos.Response.newBuilder()
                                                      .setResponseType(ResponseProtos.Response.ResponseType.PLAY)
                                                      .setBoard(server.game.getBoard())
                                                      .setSecond(false)
                                                      .setEval(false)
                                                      .build();

                    System.out.println(server.game.showBoard());
                }
                else if (choice == RequestProtos.Request.OperationType.TILE1)
                {
                    if(checkValid(op.getTile())){
                            int row = getRow(op.getTile());
                            int col = getCol(op.getTile());
                            System.out.println(" Tile 1 col: " + col + " row: " + row);
                            game.setTempCol1(col);
                            game.setTempRow1(row);

                            response = ResponseProtos.Response.newBuilder().setResponseType(ResponseProtos.Response.ResponseType.PLAY).setBoard(
                                    game.tempFlipWrongTiles(row, col)).setSecond(true).setEval(false).build();

                            System.out.println(game.showBoard());
                            System.out.println(game.getBoard());

                    }
                    else if(server.game.getWon()){
                        System.out.println("Game Won");

                        server.leaderboard.getPlayerByName(name).addWin();
                        response = ResponseProtos.Response.newBuilder()
                                                          .setResponseType(ResponseProtos.Response.ResponseType.WON)
                                                          .setBoard(game.getBoard())
                                                          .build();
                    }
                    else if(gError.equals("q")) {
                        response = ResponseProtos.Response.newBuilder().setResponseType(ResponseProtos.Response.ResponseType.BYE).setMessage(
                                "****** Goodbye " + name + " ****** \n" + "   Thanks for playing!!").build();
                    }
                    else{
                        response = ResponseProtos.Response.newBuilder().setResponseType(ResponseProtos.Response.ResponseType.ERROR)
                                                          .setMessage(gError)
                                                          .build();
                    }
                }
                else if (choice == RequestProtos.Request.OperationType.TILE2)
                {
                    if(checkValid(op.getTile())) {
                        int row = getRow(op.getTile());
                        int col = getCol(op.getTile());
                        System.out.println(" Tile 2 col: " + col + " row: " + row);
                        game.setTempCol2(col);
                        game.setTempRow2(row);
                        Boolean match = game.checkMatch();
                        if (match) {
                            game.flipTiles(game.getTempRow1(), game.getTempCol1(), row, col);
                        }

                        System.out.println("THIS IS FULL BOARD ");
                        System.out.println(game.showBoard() + "\n");
                        System.out.println("\nTHIS IS HIDDEN BOARD");
                        System.out.println(game.getBoard());
                        server.game.checkWin();

                        if (game.getWon()) {
                            System.out.println("Game Won");

                            server.leaderboard.getPlayerByName(name).addWin();
                            response = ResponseProtos.Response.newBuilder()
                                                              .setResponseType(ResponseProtos.Response.ResponseType.WON)
                                                              .setBoard(game.getBoard())
                                                              .build();

                        }
                        else if (!match) {
                            response = ResponseProtos.Response.newBuilder()
                                                              .setResponseType(ResponseProtos.Response.ResponseType.PLAY)
                                                              .setBoard(game.tempFlipWrongTiles(game.getTempRow1(), game.getTempCol1(), row, col))
                                                              .setSecond(false)
                                                              .setEval(false)
                                                              .build();
                        }
                        else {
                            response = ResponseProtos.Response.newBuilder()
                                                              .setResponseType(ResponseProtos.Response.ResponseType.PLAY)
                                                              .setBoard(game.getBoard())
                                                              .setSecond(false)
                                                              .setEval(true)
                                                              .build();
                        }
                    }
                    else if(gError.equals("q")) {
                        response = ResponseProtos.Response.newBuilder().setResponseType(ResponseProtos.Response.ResponseType.BYE).setMessage(
                                "****** Goodbye " + name + " ****** \n" + "   Thanks for playing!!").build();
                    }
                    else{
                        response = ResponseProtos.Response.newBuilder().setResponseType(ResponseProtos.Response.ResponseType.ERROR)
                                                          .setMessage(gError)
                                                          .build();
                    }
                }
                else if (choice == RequestProtos.Request.OperationType.QUIT)
                {
                    response = ResponseProtos.Response.newBuilder()
                                                      .setResponseType(ResponseProtos.Response.ResponseType.BYE)
                                                      .setMessage("****** Goodbye " + name + " ****** \n" + "   Thanks for playing!!")
                                                      .build();

                    server.playersOnline.remove(server.leaderboard.getPlayerByName(name));
                }
                else
                {
                    System.out.println("Error in choice");
                    response = ResponseProtos.Response.newBuilder()
                                                      .setResponseType(ResponseProtos.Response.ResponseType.ERROR)
                                                      .setMessage("Hey, " + name + " That hasn't been implemented yet.")
                                                      .build();
                }

                //SEND MESSAGE
                response.writeDelimitedTo(out);
            }
            // close the resource
            System.out.println("close the resources of client ");
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        startSession();
        try {
            System.out.println("close socket of client ");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect)
     * @return String of the new hidden image
     */
    public static void writeToLog(String name, RequestProtos.Message message){
        try {
            // read old log file
            RequestProtos.Logs.Builder logs = readLogFile();

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();
            System.out.println(date);

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " +  name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            RequestProtos.Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a protobuf object
            // which has a repeated field "log"

            for (String log: logsObj.getLogList()){

                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        }catch(Exception e){
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static RequestProtos.Logs.Builder readLogFile() throws Exception{
        RequestProtos.Logs.Builder logs = RequestProtos.Logs.newBuilder();

        try {
            // just read the file and put what is in it into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }

    public int getRow( String str)
    {
        int row = 0;
        if(str.charAt(0) == 'A' || str.charAt(0) == 'a')
            row = 1;
        else if(str.charAt(0) == 'B' || str.charAt(0) == 'b')
            row = 2;
        else if(str.charAt(0) == 'C' || str.charAt(0) == 'c')
            row = 3;
        else if(str.charAt(0) == 'D' || str.charAt(0) == 'd')
            row = 4;

        return row;
    }

    public int getCol( String str)
    {
        int col = Integer.parseInt(String.valueOf(str.charAt(1)));
        col = col * 2;
        return col;
    }

    public Boolean checkValid(String str){
        Boolean valid = true;
        try {

            if(str.equals("quit"))
            {
                gError = "q";
                valid = false;
            }
            else if (str.length()!= 2)
            {
                valid = false;
                System.out.println("Invalid input");
                gError = "Invalid input  (e.g: a1)";
            }
            else if(getCol(str) < 1 || getCol(str) > game.colSize()) {
                valid = false;
                System.out.println("Column out of bounds");
                gError = "Column out of bounds  (e.g: a1)";
            }
            else if(str.charAt(0) != 'A' && str.charAt(0) != 'a' && str.charAt(0) != 'B' && str.charAt(0) != 'b' && str.charAt(0) != 'C' && str.charAt(0) != 'c' && str.charAt(0) != 'D' && str.charAt(0) != 'd') {
                valid = false;
                System.out.println("Row out of bounds");
                gError = "Row out of bounds (e.g: a1)";
            }
            else if (game.checkFlipped(getRow(str), getCol(str))) {
                valid = false;
                System.out.println("Tile already flipped");
                gError = "Tile already flipped (e.g: a1)";
            }

        } catch (NumberFormatException e) {
            valid = false;
            System.out.println("Column must be a number");
            gError = "Column must be a number (e.g: a1)";
        } catch (Exception e) {
            System.out.println("Error");
            gError = "Invalid input (e.g: a1)";
            valid = false;
        }

        return valid;
    }

    public Response.Builder getPlayerListAsProtoBuf( Leaderboard leaderboard) {

        Response.Builder res = Response.newBuilder().setResponseType(Response.ResponseType.LEADER);

        List<Leaderboard.Entry> pList = leaderboard.getPlayersList();

        for (Leaderboard.Entry e : pList) {

            ResponseProtos.Entry a = ResponseProtos.Entry.newBuilder()
                                                         .setName(e.getName())
                                                         .setWins(e.getWins())
                                                         .setLogins(e.getLogins())
                                                         .build();
            res.addLeader(a);
        }
        return res;
    }
}
