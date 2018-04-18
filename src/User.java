//package broadcast;

import java.io.*;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.UnknownHostException;

/***
Created by Richard
***/
public class User extends Thread {

    // The user socket
    private static Socket userSocket = null;
    // The output stream
    private static PrintStream output_stream = null;
    // The input stream
    private static BufferedReader input_stream = null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;
    private static int flagFriendRequestApply = 0;
    private static String friendName = null;

    public static void main(String[] args) {

        // The default port.
        int portNumber = 8000;
        // The default host.
        String host = "localhost";
        String userName ;

        if (args.length < 2) {
            System.out
                    .println("Usage: java User <host> <portNumber>\n"
                            + "Now using host=" + host + ", portNumber=" + portNumber);
        } else {
            host = args[0];
            portNumber = Integer.valueOf(args[1]).intValue();
        }

		/*
		 * Open a socket on a given host and port. Open input and output streams.
		 */
        try {
            userSocket = new Socket(host, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            output_stream = new PrintStream(userSocket.getOutputStream());
            input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to the host "
                    + host);
        }

		/*
		 * If everything has been initialized then we want to write some data to the
		 * socket we have opened a connection to on port portNumber.
		 */
        if (userSocket != null && output_stream != null && input_stream != null) {
            try {
                String userInput ;
                String userMessage ;
                String friendRequest;

                String friendN;
				/* Create a thread to read from the server. */
                System.out.println("Please enter your name: ");
                userName = inputLine.readLine().trim();
                userMessage = "#join "+ userName;
                output_stream.println(userMessage);

                userMessage = "";

                new Thread(new User()).start();
                // Get user name and join the social net
                while((userInput = inputLine.readLine().trim())!=null && !closed) {

                    // Read user input and send protocol message to server
                    if (userInput.equalsIgnoreCase("Exit")) {
                        userMessage = "#Bye";
                    } else if (userInput.startsWith("@connect ")) {
                        friendRequest = userInput.split(" ",2)[1];
                        userMessage = "#friendme "+friendRequest;
                    }else if (userInput.startsWith("@friend ")){
                        friendN = userInput.split(" ",2)[1];
                        if (friendN.equalsIgnoreCase(friendName) && flagFriendRequestApply == 1) {
                            userMessage = "#friends " + friendN;
                            flagFriendRequestApply = 0;
                        }
                    }else if (userInput.startsWith("@deny ")){
                        friendN = userInput.split(" ",2)[1];
                        if (flagFriendRequestApply == 1 && friendN.equalsIgnoreCase(friendName)) {
                            userMessage = "#DenyFriendRequest " + friendN;
                            flagFriendRequestApply = 0;
                        }
                    }else if (userInput.startsWith("@disconnect ")){
                        userMessage = "#unfriend "+ userInput.split(" ",2)[1];
                    }else if(userInput.startsWith("@add ")){
                        if(userInput.split(" ",3).length == 3 ) {
                            userMessage = "#group " + userInput.split(" ", 2)[1];
                        }else
                            userMessage = userInput;
                    }else if (userInput.startsWith("@send ")){
                        if (userInput.split(" ",3).length == 3 ) {
                            userMessage = "#gstatus " + userInput.split(" ", 3)[1] + " " + userName
                                    + " " + userInput.split(" ", 3)[2];
                        }else{
                            userMessage = userInput;
                        }
                    }else if(userInput.startsWith("@delete ")){
                        if (userInput.split(" ",3).length == 3 ) {
                            userMessage = "#ungroup " + userInput.split(" ", 2)[1];
                        }else{
                            userMessage = userInput;
                        }
                    }
                    else
                        userMessage = userInput;

                    output_stream.println(userMessage);

                }
				/*
				 * Close the output stream, close the input stream, close the socket.
				 */
                output_stream.close();
                input_stream.close();
                userSocket.close();

            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }

    /*
     * Create a thread to read from the server.
     */
    public void run() {
		/*
		 * Keep on reading from the socket till we receive a Bye from the
		 * server. Once we received that then we want to break.
		 */
        String userPost = null;
        String userName = null;
        String responseLine = null;
        String friendRequest = null;
        String friendRequestReceiver = null;
        String groupName = null;
        String groupMemberName = null;
        String updateMessage = null;
        String error = null;

        int flag=0 ;

        try {

            while((responseLine =input_stream.readLine())!=null){

                if(responseLine.equals("#welcome!")){
                    flag = 1;
                }
                if(responseLine.equals("#busy")){
                    flag = 2;
                }
                if(responseLine.equals("#Bye")){
                    flag = 3;
                }
                if (responseLine.startsWith("#newuser ")){
                    userName = responseLine.split(" ",2)[1];
                    flag = 4;

                }
                if (responseLine.startsWith("#Leave ")){
                    userName = responseLine.split(" ",2)[1];

                    flag = 5;

                }
                if (responseLine.startsWith("#newStatus ")){
                    userName = responseLine.split(" ",3)[1];
                    userPost = responseLine.split(" ",3)[2];

                    flag = 6;

                }
                if (responseLine.startsWith("#friendme ")){
                    friendRequest = responseLine.split(" ",2)[1];
                    friendName = friendRequest;
                    flagFriendRequestApply = 1;
                    flag = 7;
                }
                if (responseLine.startsWith("#OKfriends "))
                {
                    friendRequestReceiver = responseLine.split(" ", 3)[1];
                    friendRequest = responseLine.split(" ",3)[2];
                    flag = 8;
                }
                if (responseLine.startsWith("#OKfriendsPost ")) {
                    friendRequestReceiver = responseLine.split(" ", 3)[1];
                    friendRequest = responseLine.split(" ", 3)[2];
                    flag = 9;
                }
                if (responseLine.startsWith("#FriendRequestDenied "))
                {
                    friendRequest = responseLine.split(" ", 2)[1];
                    flag = 10;
                }
                if (responseLine.startsWith("#NotFriends "))
                {
                    friendRequest = responseLine.split(" ", 3)[1];
                    friendRequestReceiver = responseLine.split(" ", 3)[2];
                    flag = 11;
                }
                if (responseLine.startsWith("#NotFriendsPost "))
                {
                    friendRequest = responseLine.split(" ", 3)[1];
                    friendRequestReceiver = responseLine.split(" ", 3)[2];
                    flag = 12;
                }
                if (responseLine.startsWith("#group ")){
                    groupName = responseLine.split(" ",3)[1];
                    groupMemberName = responseLine.split(" ",3)[2];
                    flag = 13;
                }
                if (responseLine.startsWith("#gstatus ")){
                    groupName = responseLine.split(" ",4)[1];
                    groupMemberName = responseLine.split(" ",4)[2];
                    updateMessage = responseLine.split(" ",4)[3];
                    flag = 14;
                }
                if (responseLine.startsWith("#ungroup ")){
                    groupName = responseLine.split(" ",3)[1];
                    groupMemberName = responseLine.split(" ",3)[2];
                    flag = 15;
                }
                if (responseLine.startsWith("#statusPosted ")){
                    userPost = responseLine.split(" ",2)[1];

                    flag = 16;

                }
                if (responseLine.startsWith("#error:")){
                    error = responseLine.split(":",3)[1];
                    userPost = responseLine.split(":",3)[2];
                    flag = -1;

                }
                if (responseLine.startsWith("#error_friend:")){
                    error = responseLine.split(":",3)[1];
                    userPost = responseLine.split(":",3)[2];
                    flagFriendRequestApply = 1;
                    flag = -1;

                }
                // Display on console based on what protocol message we get from server.
                switch (flag){
                    case 1:System.out.println(responseLine+" Connection has been established!");
                        break;
                    case 2: System.out.println(responseLine+" The server is busy, try later. Press Enter to accept.");
                        break;
                    case 3:System.out.println("Server response:"+responseLine+"\nYou are disconnected. Press Enter to accept.");
                        break;
                    case 4:System.out.println(userName+" has joined the network!");
                        break;
                    case 5:System.out.println(userName+" has left.");
                        break;
                    case 6:System.out.println("["+userName+"] " +userPost);
                        break;
                    case 7:System.out.println(friendRequest+" has send you a friend request."
                            +"\nType '@friend <requesterUsername>' to accept, or '@deny <requesterUsername>' to reject.");
                        break;
                    case 8:System.out.println(friendRequest+" and "+friendRequestReceiver+"(you) now are friends.");
                        break;
                    case 9:System.out.println(friendRequest+" and "+friendRequestReceiver+" now are friends.");
                        break;
                    case 10:System.out.println(friendRequest+" rejected your friend request.");
                        break;
                    case 11:System.out.println(friendRequest+"(you) and "+friendRequestReceiver+" are no longer friends.");
                        break;
                    case 12:System.out.println(friendRequest+" and "+friendRequestReceiver+" are no longer friends.");
                        break;
                    case 13:System.out.println(groupMemberName +" is now in group " +groupName);
                        break;
                    case 14:System.out.println("["+groupName+"]" +"[" +groupMemberName+"]"+updateMessage);
                        break;
                    case 15:System.out.println(groupMemberName+" is no longer member of the group "+groupName);
                        break;
                    case 16:System.out.println("Your status: " +userPost+" has been posted successfully.");
                        break;
                    case -1:System.out.println("Error: you sent "+ error +" : "+userPost);
                        break;
                    default:break;
                }
                if(flag == 3|| flag == 2)break;
                flag=0;
            }
            closed = true;
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }
}



