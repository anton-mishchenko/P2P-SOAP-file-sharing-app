/*
 * 2023/10/13
 * Anton Mishchenko
 */

package server;

import javax.swing.*;
import java.util.Objects;

/**
 * <p> ActiveUsers class used to store active users that log on to the server. Allows to keep
 * track of active users and their tokens. Allows to search and remove active users.</p>
 */
public class ActiveUsers {
    private int MAX_USERS;
    private volatile ClientData[] users;
    private int numOfUsers = 0;
    private JTextArea textArea;

    /**
     * <p> Constructor for ActiveUsers class. Sets max number of users to the default value,  creates user array,
     * updates user count.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link ActiveUsers#updateUserCountArea}</p>
     *
     * @param MAX_USERS max number of users.
     */
    public ActiveUsers(int MAX_USERS, JTextArea textArea){
        this.MAX_USERS = MAX_USERS;
        this.textArea = textArea;
        users = new ClientData[MAX_USERS];
        updateUserCountArea();
    }

    /**
     * <p> This method adds a user to the active users array and updates user count gui.</p>
     *
     * <p>Called by: {@link Server.P2PServiceImpl}</p>
     *
     * <p> Calls: {@link ActiveUsers#updateUserCountArea}</p>
     *
     * @param token String of the user to add.
     * @param userName String of the user to add.
     * @return ClientData object of the added user or <i>null</i> if no space left.
     */
    public synchronized ClientData addUser(String token, String userName){
        for(int i=0; i<users.length; i++){
            // find null spot in array and add user
            if(users[i] == null){
                users[i] = new ClientData(userName, token);
                numOfUsers++;
                updateUserCountArea();
                return users[i];
            }
        }
        return null;
    }

    /**
     * <p> This method removes a user from the active users array and updates user count gui.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}, {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link ActiveUsers#updateUserCountArea}</p>
     *
     * @param token of user to be removed.
     * @param userName of the user to be removed.
     * @return ClientData object of the removed user or <i>null</i> if user not found.
     */
    public synchronized ClientData removeUser(String token, String userName){
        for(int i=0; i<users.length; i++){
            if (users[i] != null) {
                // find user and replace with null
                if (Objects.equals(users[i].getToken(), token) && users[i].getName().equals(userName)) {
                    ClientData removedUser = users[i];
                    users[i] = null;
                    numOfUsers--;
                    updateUserCountArea();
                    return removedUser;
                }
            }
        }
        return null;
    }

    /**
     * <p> This method finds a user in the active users array based on their userName.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * @param userName of the user to find.
     * @return ClientData object of the found user or <i>null</i> if user was not found.
     */
    public synchronized ClientData findUser(String userName){
        for (ClientData user : users) {
            if (user != null) {
                if (user.getName().equals(userName)) {
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * <p> This method lists all active users.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}, {@link Server.ServerGUI}</p>
     *
     * @return ClientData array of currently active users.
     */
    public synchronized ClientData[] listActiveUsers(){
        // return null if no users are active
        if(numOfUsers == 0){
            return null;
        }
        int usersAdded = 0;
        ClientData [] activeUsers = new ClientData[numOfUsers];
        // add all active users to the new array to be returned
        for (ClientData user : users) {
            if (user != null && usersAdded <= numOfUsers) {
                activeUsers[usersAdded] = user;
                usersAdded++;
            }
        }
        return activeUsers;
    }

    /**
     * <p> This method updates the user count gui.</p>
     *
     * <p> Called by: {@link Server.ActiveUsers}</p>
     */
    private void updateUserCountArea(){
        textArea.setText(numOfUsers + "/" + MAX_USERS);
    }

    /**
     * <p> This method checks if there is space for a new user in the array of active users.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * @return boolean true if there is space, false if there isn't.
     */
    public synchronized boolean hasSpace(){
        return numOfUsers < MAX_USERS;
    }

    /**
     * <p> This method returns the number of active users.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * @return int number of active users.
     */
    public synchronized int getNumOfUsers(){
        return numOfUsers;
    }
}