//package broadcast;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;



/*
 * A server that delivers status messages to other users.
 */
public class Server {

    // Create a socket for the server
    private static ServerSocket serverSocket = null;
    // Create a socket for the server
    private static Socket userSocket = null;
    // Maximum number of users
    private static int maxUsersCount = 5;
    // An array of threads for users
    private static userThread[] threads = null;

    //private static int[][] group = new int[maxUsersCount ][maxUsersCount];

    public static void main(String args[]) {

        // The default port number.
        int portNumber = 8000;
        if (args.length < 2) {
            System.out.println("Usage: java Server <portNumber>\n"
                    + "Now using port number=" + portNumber + "\n" +
                    "Maximum user count=" + maxUsersCount);
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
            maxUsersCount = Integer.valueOf(args[1]).intValue();
        }

        System.out.println("Server now using port number=" + portNumber + "\n" + "Maximum user count=" + maxUsersCount);


        userThread[] threads = new userThread[maxUsersCount];


		/*
		 * Open a server socket on the portNumber (default 8000).
		 */
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(e);
        }

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
        while (true) {
            try {
                userSocket = serverSocket.accept();

                int i ;
                for (i = 0; i < maxUsersCount; i++) {
                    if (threads[i] == null) {
                        threads[i] = new userThread(userSocket, threads);
                        threads[i].start();
                        break;
                    }
                }

                if (i == maxUsersCount) {
                    PrintStream output_stream = new PrintStream(userSocket.getOutputStream());
                    output_stream.println("#busy");
                    output_stream.close();
                    userSocket.close();
                }

                /*
			 * Close the output stream, close the input stream, close the socket.
			 */


            } catch (IOException e) {
                System.err.println("IOException: "+e);
            }
        }

    }
}

/*
 * Threads
 */
class userThread extends Thread {

    private String userName = null;
    private BufferedReader input_stream = null;
    private PrintStream output_stream = null;
    private Socket userSocket = null;
    private final userThread[] threads;
    private int[] friendList;
    private int maxUsersCount;
    private ArrayList<chatGroup> groupList = new ArrayList<>() ;


    public userThread(Socket userSocket, userThread[] threads) {
        this.userSocket = userSocket;
        this.threads = threads;
        maxUsersCount = threads.length;

        friendList = new int[maxUsersCount];
        for(int j = 0;j<maxUsersCount;j++) {
            friendList[j] = -1;
        }

        try {
            input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));

        }catch (IOException e) {
            System.err.println("IOException:  " + e);
        }
    }

    public void run() {
        int maxUsersCount = this.maxUsersCount;
        userThread[] threads = this.threads;

        String userMessage = null;
        String userPost = null;



        try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
             */
            while(true) {
                input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
                userMessage = input_stream.readLine();

                if (userMessage.startsWith("#join")) {
                    userName = userMessage.split(" ", 2)[1];
                    System.out.println(userName+" has joined.");

                    /* Welcome the new user. */

                    synchronized (userThread.class) {
                        for (int i = 0; i < maxUsersCount; i++) {
                            if (threads[i] != null) {
                                output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                            }else continue;
                            if (threads[i] != this) {
                                output_stream.println("#newuser " + userName + "\n");
                            }else
                                output_stream.println("#welcome!");

                        }
                    }
                }


                else if (userMessage.startsWith("#Bye")) {
                    // If user send a message to disconnect.
                    synchronized (userThread.class) {
                        for (int i = 0; i < maxUsersCount; i++) {
                            if (threads[i] != null) {
                                output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                            } else continue;
                            if (threads[i] != this) {
                                output_stream.println("#Leave " + userName + "\n");

                            } else  {
                                for (int j = 0;j<maxUsersCount;j++){
                                    if (threads[j]!=null) {
                                        //clear this user's friend list and group list.
                                        threads[j].friendList[i] = -1;
                                        if (!threads[j].groupList.isEmpty()) {
                                            for (chatGroup h : threads[j].groupList) {
                                                h.setGroupMember(i,-1);
                                            }
                                        }
                                    }
                                }
                                output_stream.println("#Bye");
                                System.out.println(userName+" has left.");
                            }

                        }
                    }
                    break;
                }
                else if (userMessage.startsWith("#status")) {
                    //get the status message and broadcast it to other users.
                    userPost = userMessage.split(" ", 2)[1];
                    synchronized (userThread.class) {
                        for (int i = 0; i < maxUsersCount; i++) {
                            if (this.friendList[i] == 1) {
                                if (threads[i] != null) {
                                    output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                } else continue;

                                output_stream.println("#newStatus " + userName + " " + userPost + "\n");
                            }
                            if (threads[i] == this) {
                                output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                output_stream.println("#statusPosted " + userPost + "\n");
                                System.out.println(userName + " has sent a status: " + userPost);
                            }
                        }
                    }
                }
                else if (userMessage.startsWith("#friendme")) {
                    //get a friend request from a user, send it to intended user.

                    String friendRequest = userMessage.split(" ", 2)[1];
                    int flagg = 0;
                    synchronized (userThread.class) {
                        for (int i = 0; i < maxUsersCount; i++) {

                            if (threads[i] != null && threads[i].userName.equalsIgnoreCase(friendRequest)) {
                                System.out.println(userName + " has sent a friend request to " + friendRequest);
                                output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                output_stream.println("#friendme " + userName);
                                flagg = 1;
                            }
                        }
                        if (flagg == 0) {
                            System.out.println("Got wrong name of the user who will receive friend request: " + friendRequest);
                            for (int i = 0; i < maxUsersCount; i++) {
                                if (threads[i] != null && threads[i].userName != null && threads[i].userName.equals(userName)) {
                                    output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                    output_stream.println("#error:wrong name of the user who will receive friend request:"
                                            + friendRequest);
                                }
                            }
                        }
                    }
                }
                else if (userMessage.startsWith("#friends")) {
                    //the intended user agreed, broadcast this news to every user.

                    String friendRequest = userMessage.split(" ", 2)[1];
                    String u = userName;

                    int k;
                    int t;

                    for (k = 0; k < maxUsersCount; k++) {
                        if (threads[k] != null && threads[k].userName != null && threads[k].userName.equals(friendRequest))
                            break;
                    }
                    for (t = 0; t < maxUsersCount; t++) {
                        if (threads[t] != null && threads[t] == this)
                            break;
                    }
                    if (k < maxUsersCount && t < maxUsersCount) {
                        threads[t].friendList[k] = 1;
                        threads[k].friendList[t] = 1;
                    } else {
                        System.out.println("Got wrong name of the user who sent friend request: " + friendRequest);
                        synchronized (userThread.class) {
                            for (int i = 0; i < maxUsersCount; i++) {
                                if (threads[i] != null && threads[i].userName != null && threads[i].userName.equals(u)) {
                                    output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                    output_stream.println("#error_friend:wrong name of the user who sent friend request:"
                                            + friendRequest);
                                }
                            }
                        }
                        continue;
                    }
                    System.out.println(u + " agree to be a friend of " + friendRequest);
                    synchronized (userThread.class) {
                        for (int i = 0; i < maxUsersCount; i++) {
                            if (threads[i] != null) {
                                output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                            } else continue;
                            if (threads[i] == this) {
                                output_stream.println("#OKfriends " + u + " " + friendRequest);
                            } else if (threads[i].userName.equalsIgnoreCase(friendRequest)) {

                                output_stream.println("#OKfriends " + friendRequest + " " + u);
                            } else
                                output_stream.println("#OKfriendsPost " + u + " " + friendRequest);
                        }
                    }
                }
                else if (userMessage.startsWith("#DenyFriendRequest")) {
                    //the intended user rejected the request, send this news to user who sent this request.

                    String friendRequest = userMessage.split(" ", 2)[1];
                    String u = userName;
                    int flagf = 0;

                    synchronized (userThread.class) {
                        for (int i = 0; i < maxUsersCount; i++) {
                            if (threads[i] != null && threads[i].userName.equalsIgnoreCase(friendRequest)) {
                                output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                output_stream.println("#FriendRequestDenied " + u);
                                System.out.println(u + " rejected the friend request from " + friendRequest);
                                flagf = 1;
                            }
                        }
                        if (flagf == 0){
                            System.out.println("Got a wrong name of the user who sent friend request: " + friendRequest);
                            synchronized (userThread.class) {
                                for (int i = 0; i < maxUsersCount; i++) {
                                    if (threads[i] != null && threads[i].userName != null && threads[i].userName.equals(u)) {
                                        output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                        output_stream.println("#error_friend:a wrong name of the user who sent friend request:"
                                                + friendRequest);
                                    }
                                }
                            }
                        }
                    }
                }

                else if (userMessage.startsWith("#unfriend")) {
                    //a user wants to disconnect with one of his friends.
                    String friendRequest = userMessage.split(" ", 2)[1];
                    String u = userName;

                    int k;
                    int t;
                    for (k = 0; k < maxUsersCount; k++) {
                        if (threads[k] != null && threads[k].userName != null && threads[k].userName.equals(friendRequest))
                            break;
                    }
                    for (t = 0; t < maxUsersCount; t++) {
                        if (threads[t] != null && threads[t] == this)
                            break;
                    }

                    /* First, find out the thread whose user wants to "unfriend" and the one that will be "unfriend"
                     * If they both in the friend list of each other, delete them from each other's list.
                     * Broadcast this news.
                    * */

                    if (k < maxUsersCount && t < maxUsersCount && this.friendList[k] == 1) {
                        threads[t].friendList[k] = -1;
                        threads[k].friendList[t] = -1;
                        System.out.println(u + " unfriend " + friendRequest);
                        synchronized (userThread.class) {
                            for (int i = 0; i < maxUsersCount; i++) {
                                if (threads[i] != null) {
                                    output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                } else continue;
                                if (threads[i] == this) {
                                    output_stream.println("#NotFriends " + u + " " + friendRequest);
                                } else if (threads[i].userName.equalsIgnoreCase(friendRequest)) {
                                    output_stream.println("#NotFriends " + friendRequest + " " + u);
                                } else
                                    output_stream.println("#NotFriendsPost " + u + " " + friendRequest);

                            }
                        }
                    }else{
                        if(k >= maxUsersCount || t >= maxUsersCount){
                            System.out.println("Got a wrong name of the user who will be 'unfriend': " + friendRequest);
                            synchronized (userThread.class) {
                                for (int i = 0; i < maxUsersCount; i++) {
                                    if (threads[i] != null && threads[i].userName != null && threads[i].userName.equals(u)) {
                                        output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                        output_stream.println("#error_friend:a wrong name of the user who will be 'unfriend':"
                                                + friendRequest);
                                    }
                                }
                            }
                        }else{
                            System.out.println("The user "+userName+" is trying to 'unfriend' a stranger " + friendRequest);
                            synchronized (userThread.class) {
                                for (int i = 0; i < maxUsersCount; i++) {
                                    if (threads[i] != null && threads[i].userName != null && threads[i].userName.equals(u)) {
                                        output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                        output_stream.println("#error_friend:a user that is not your friend:"
                                                + friendRequest);
                                    }
                                }
                            }

                        }
                    }
                }
                else if (userMessage.startsWith("#group ")){
                    String groupName  = userMessage.split(" ", 3)[1];
                    String addUserName = userMessage.split(" ", 3)[2];

                    int flagAdd = 0;
                    boolean flagAdded = false;

                    int k;
                    int t;
                    for (k = 0; k < maxUsersCount; k++) {
                        if (threads[k] != null && threads[k].userName != null && threads[k].userName.equals(addUserName))
                            break;
                    }
                    for (t = 0; t < maxUsersCount; t++) {
                        if (threads[t] != null && threads[t] == this)
                            break;
                    }

                    if (k<maxUsersCount && t<maxUsersCount && this.friendList[k] == 1){
                        //if they are friends

                        flagAdd = 0;       // label group existence.
                        flagAdded = false; // check if group member is added successfully.

                        for (chatGroup h : this.groupList) {
                            //if this group exists, add a member in it.
                            // Make this available to every thread.
                            if (h.getGroupName().equals(groupName)) {
                                flagAdd = 1;
                                flagAdded = h.addMember(t, k, 1);
                                for (int w = 0;w<maxUsersCount;w++) {
                                    if (threads[w] != null) {
                                        threads[w].groupList.add(h);
                                        
                                    }
                                }
                                break;
                            }
                        }
                        if (flagAdd != 1 ) {
                            //if this group doesn't exist, create a new one, then add it.
                            chatGroup g = new chatGroup(groupName, maxUsersCount);
                            flagAdded = g.addMember(t, k, 0);
                            for (int w = 0;w<maxUsersCount;w++) {
                                if (threads[w] != null) {
                                    threads[w].groupList.add(g);

                                }
                            }
                        }

                        if (flagAdded){
                            //if this member is successfully added, broadcast this news to every user in this group.
                            System.out.println(addUserName+" is now added to "+groupName);
                            for(chatGroup h :groupList){
                                if (h.getGroupName().equals(groupName)) {
                                    synchronized (userThread.class) {
                                        for (int i = 0; i < maxUsersCount; i++) {
                                            if (threads[i] != null && h.getGroupMember()[i] == 1 ) {
                                                output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                            } else continue;

                                            output_stream.println("#group " + groupName + " " + addUserName);
                                        }
                                    }
                                    break;
                                }
                            }

                        }

                    }

                }

                else if(userMessage.startsWith("#gstatus ")){
                    String groupName  = userMessage.split(" ", 4)[1];
                    String addUserName = userMessage.split(" ", 4)[2];
                    String updateMessage = userMessage.split(" ", 4)[3];

                    int t; //the operator id.
                    for (t = 0;t<maxUsersCount;t++){
                        if (threads[t] == this)
                            break;
                    }

                    for (chatGroup h:groupList){
                        if(t<maxUsersCount && h.getGroupName().equals(groupName) && h.getGroupMember()[t] == 1){
                            //if this user is in this group, his status message should be broadcast to every member in the group.
                            System.out.println(addUserName+" updated a message to "+groupName);
                            synchronized (userThread.class) {
                                for (int i = 0; i < maxUsersCount; i++) {
                                    if (threads[i] != null && h.getGroupMember()[i]==1) {
                                        output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                    } else continue;

                                    output_stream.println("#gstatus " + groupName+ " " + addUserName +" "+updateMessage);
                                }
                            }
                            break;
                        }
                    }

                }
                else if(userMessage.startsWith("#ungroup ")){
                    String groupName  = userMessage.split(" ", 3)[1];
                    String deleteUserName = userMessage.split(" ", 3)[2];
                    boolean flagDelete = false;
                    int k; //deleteUserID
                    int t; //operator id
                    for (k = 0;k<maxUsersCount;k++){
                        if (threads[k] != null && threads[k].userName != null &&threads[k].userName.equals(deleteUserName))
                            break;
                    }

                    for (t = 0;t<maxUsersCount;t++){
                        if (threads[t] == this)
                            break;
                    }
                    for (int w=0;w<maxUsersCount;w++) {
                        if (threads[w]!=null && k < maxUsersCount && t < maxUsersCount && this.friendList[k] != 1) {
                            //the member who will be deleted cannot be a friend of the operator.
                            for (chatGroup h : threads[w].groupList) {

                                //delete this user's membership in every thread.
                                if (h.getGroupName().equals(groupName)) {

                                    flagDelete = h.deleteMember(t, k);

                                    break;
                                }
                            }
                        }
                    }

                    if (flagDelete) {
                        //if this user is deleted successfully, broadcast this news to member in this group.
                        System.out.println(deleteUserName + " is now deleted from " + groupName);
                        for (chatGroup h : groupList) {
                            synchronized (userThread.class) {
                                for (int i = 0; i < maxUsersCount; i++) {
                                    if (threads[i] != null && (h.getGroupMember()[i] == 1 || i == k)) {
                                        output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                                    } else continue;
                                    output_stream.println("#ungroup " + groupName + " " + deleteUserName);

                                }
                            }
                            break;
                        }
                    }
                }else {
                    userPost = userMessage;
                    System.out.println(userName + " has sent a message not in correct format: " + userPost);
                    synchronized (userThread.class) {
                        for (int i = 0; i < maxUsersCount; i++) {
                            if (threads[i] != null) {
                                output_stream = new PrintStream(threads[i].userSocket.getOutputStream());
                            } else continue;
                            if (threads[i] == this)
                                output_stream.println("#error:a message not in correct format:"
                                        + userPost + "\n");
                        }
                    }
                }
                // conversation ended.
            }
			/*
			 * Clean up. Set the current thread variable to null so that a new user
			 * could be accepted by the server.
			 */

            synchronized (userThread.class) {
                for (int i = 0; i < maxUsersCount; i++) {
                    if (threads[i] == this) {
                        threads[i] = null;
                    }

                }
            }

        } catch (IOException e) {
            System.err.println(e);
        }
    }

}
class chatGroup {
    private String groupName;
    private int[] groupMember;
    private int maxUsersCount;
    public chatGroup(String name,int maxUsersCount){
        this.groupName = name;
        this.maxUsersCount = maxUsersCount;

        groupMember = new int[maxUsersCount];
        for (int i = 0;i<maxUsersCount;i++){
            groupMember[i] = -1;
        }
    }
    public String getGroupName(){
        return groupName;
    }

    public int[] getGroupMember() {
        return groupMember;
    }
    public void printGroupMember(){
        for(int i = 0 ;i<maxUsersCount;i++)
            System.out.print(groupMember[i]+" ");
        System.out.print("\n");
    }

    public void setGroupMember(int groupMemberID,int set) {
        this.groupMember[groupMemberID] = set;
    }

    public boolean addMember(int operatorID, int userID, int flagE){

        if (userID < 0 || userID >= maxUsersCount){
            System.out.println("The userID is invalid" );
            return false;
        }
        if (operatorID < 0 || operatorID >= maxUsersCount){
            System.out.println("The operatorID is invalid" );
            return false;
        }
        //check if operator is in this group,
        // the operator and the user should be friends,
        //the user should not be a member before this operation.

        if (flagE == 0){
            groupMember[operatorID] = 1;
            groupMember[userID] = 1;
            return true;
        }
        else if (groupMember[operatorID] == 1 && groupMember[userID] == -1 ) {
            groupMember[userID] = 1;
            return true;
        }
        return false;
    }

    public boolean deleteMember(int operatorID, int memberID){

        if (memberID < 0 || memberID >= maxUsersCount){
            System.out.println("The userID is invalid" );
            return false;
        }
        if (operatorID < 0 || operatorID >= maxUsersCount){
            System.out.println("The operatorID is invalid" );
            return false;
        }

        //check if both operator and user are in this group,
        //if so, delete the member.
        if(groupMember[operatorID] == 1 ){
            groupMember[memberID] = -1;
            return true;
        }

        return false;
    }
}




