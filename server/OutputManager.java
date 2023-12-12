/*
 * 2023/10/13
 * Anton Mishchenko
 */

package server;

import javax.swing.JTextArea;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p> OutputManager class is used to print messages to Server GUI text area. It uses a lock to make
 * sure that only one thread can access the text area at one time. Messages displayed by this method
 * are start with the current hour, minute and second.</p>
 */
public class OutputManager {
    private JTextArea serverOutputArea;
    private static Lock textLock;

    /**
     * <p> Constructor for the OutputManager class.
     * It takes in a JTextArea that will be used to display messages to the Server user. And
     * initializes the lock that will be used by this manager.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * @param serverOutputArea JTextArea from Server GUI for displaying messages.
     */
    public OutputManager(JTextArea serverOutputArea) {
        this.serverOutputArea = serverOutputArea;
        textLock = new ReentrantLock();
    }

    /**
     * <p> This method prints the given text to the Server GUI serverOutputArea. It uses a lock
     * to ensure only one thread can access the text area at one time. It appends current hour, minute and second
     * to the message printed.</p>
     *
     * <p> Called by: {@link Server.SQLConnectionManager}, {@link Server.ServerGUI}, {@link Server.P2PServiceImpl}</p>
     *
     * @param text String text to be printed to the Server GUI.
     */
    public void printToTextArea(String text) {
        try {
            // lock the text area so only one thread can access it at a time
            textLock.lock();
            // add current time and print the text to the text area
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            serverOutputArea.append("(" + dtf.format(LocalDateTime.now()) + ") " + text + "\n");
        } finally {
            // unlock the text area
            textLock.unlock();
        }
    }
}