package client;

import java.net.*;
import java.io.*;
import java.util.List;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos;
import buffers.ResponseProtos.Response;

class SockBaseClient {

    public static void main (String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int port = 9099; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server.");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response response;
        try {
            // connect to the server
            serverSock = new Socket(host, port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            op.writeDelimitedTo(out);

            do{
                //RECEIVE RESPONSE
                response = Response.parseDelimitedFrom(in);
                //SAVE CHOICE
                Response.ResponseType choice = response.getResponseType();

                //IF CHOICE IS NAME
                if(choice == Response.ResponseType.GREETING){
                    //DISPLAY GREETING
                    System.out.println(response.getMessage());
                    //ASK FOR INPUT
                    op = menu( op, stdin, strToSend, response);
                }
                if (choice == Response.ResponseType.LEADER) {
                    //DISPLAY LEADERBOARD
                    System.out.println("**********LEADERBOARD**********");
                    System.out.println(entriesToString(response));
                    //ASK FOR INPUT
                    op = menu( op, stdin, strToSend, response);
                }
                if (choice == Response.ResponseType.PLAY) {
                    //Build Response
                    op = play(op, stdin, response, in, out);
                }
                if(choice == Response.ResponseType.WON){
                    //DISPLAY GAMEWON
                    System.out.println("\nYou won!");
                    //ASK FOR INPUT
                    op = menu( op, stdin, strToSend, response);
                }

                if(response.getResponseType() == Response.ResponseType.ERROR){
                    //DISPLAY ERROR
                    System.out.println(response.getMessage());
                    System.out.println(response.getBoard());
                    op = play( op, stdin, response, in, out);
                }
                if(choice == Response.ResponseType.BYE){
                    //DISPLAY GOODBYE
                    System.out.println(response.getMessage());
                    break;
                }
                //SEND REQUEST
                op.writeDelimitedTo(out);

            }
            while(true);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null)   in.close();
            if (out != null)  out.close();
            if (serverSock != null) serverSock.close();
        }



    }

    public static Request menu(Request op, BufferedReader stdin, String strToSend, Response response) throws IOException{
        //DISPLAY MENU
        System.out.println("What would you like to do? " +
                                   "\n 1 - to see the leader board " +
                                   "\n 2 - to enter a game " +
                                   "\n 3 - quit the game");

        // Ask user for input
        strToSend = stdin.readLine();
        if(validOption(strToSend) == true) {
            int selected = Integer.parseInt(strToSend);

            // Build the request object

            if (selected == 1) op = Request.newBuilder().setOperationType(Request.OperationType.LEADER).build();
            else if (selected == 2) {

                op = Request.newBuilder().setOperationType(Request.OperationType.NEW).build();

            }
            else if (selected == 3) op = Request.newBuilder().setOperationType(Request.OperationType.QUIT).build();
        }
        else{
            System.out.println("Invalid option");
        }

        return op;
    }

    public static Request play( Request op, BufferedReader stdin, Response response, InputStream in, OutputStream out ) throws IOException{
        String strToSend;

        //Handle first tile
        if(response.getSecond() == false)
        {
            System.out.println(response.getBoard());

            System.out.println("Please enter the location of a tile: ");
            strToSend = stdin.readLine();
            op = Request.newBuilder()
                        .setOperationType(Request.OperationType.TILE1)
                        .setTile(strToSend).build();


        }
        //Handle second tile
        else
        {
            System.out.println(response.getBoard());
            System.out.println("Please enter the location of a tile: ");
            strToSend = stdin.readLine();
            op = Request.newBuilder()
                        .setOperationType(Request.OperationType.TILE2)
                        .setTile(strToSend).build();
            //SEND REQUEST
            op.writeDelimitedTo(out);
            //RECEIVE RESPONSE
            response = Response.parseDelimitedFrom(in);

            //WIN
            if((response.getResponseType() == Response.ResponseType.WON))
            {
                System.out.println("CONGRATS! YOU WON!");
                op = menu( op, stdin, strToSend, response);
            }
            //MATCH
            else if ((response.getResponseType() == Response.ResponseType.PLAY) && (response.getEval() == true)) {

                System.out.println(response.getBoard());

                System.out.println("Nice You got a Match");

                System.out.println("Please enter the location of a tile: ");
                strToSend = stdin.readLine();
                op = Request.newBuilder()
                            .setOperationType(Request.OperationType.TILE1)
                            .setTile(strToSend).build();

            }
            //NO MATCH
            else if((response.getResponseType() == Response.ResponseType.PLAY) && (response.getEval() == false)) {
                System.out.println(response.getBoard());

                System.out.print("Enter Anything to Continue: ");
                try{
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    br.read();
                    while (br.ready()) {
                        br.read();
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                op = Request.newBuilder()
                            .setOperationType(Request.OperationType.NEW)
                            .build();
            }
            else {
            }

        }

        return op;
    }


    public static String entriesToString( Response rProto ){
        List<ResponseProtos.Entry> list = rProto.getLeaderList();
        String res = "";
        for(var proto : rProto.getLeaderList()){
          res += proto.getName() + ": Wins: " + proto.getWins() + " Logins: " + proto.getLogins() + "\n";
        }
        return res;
    }

    public static Boolean validOption(String str) {
        if (str.equals("1") || str.equals("2") || str.equals("3")) return true;
        else return false;
    }



}


