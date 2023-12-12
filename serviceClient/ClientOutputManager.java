/*
 * 2023/10/13
 * Anton Mishchenko
 */

package serviceClient;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import static javax.swing.text.DefaultCaret.ALWAYS_UPDATE;

/**
 * <p>This output manager class is used by the client to print messages to the GUI text areas. It uses a lock to make
 * sure that only one thread can append text to an area at one time.</p>
 */
public class ClientOutputManager {
    private JTextArea loginTextArea;
    private JTextArea manageTextArea;
    private JTextArea downloadTextArea;
    private static Lock loginLock;
    private static Lock manageLock;
    private static Lock downloadLock;

    /**
     * <p> Constructor for the ClientOutputManager class. It takes in three JTextAreas that will be used to display
     * messages to the user. It initializes the locks that will be used by this manager.</p>
     *
     * <p> Called by: {@link serviceClient.ClientMain#main} </p>
     *
     * @param loginTextArea JTextArea from client login tab GUI for displaying messages.
     * @param manageTextArea JTextArea from client manage tab GUI for displaying messages.
     * @param downloadTextArea JTextArea from client download tab GUI for displaying messages.
     */
    public ClientOutputManager(JTextArea loginTextArea, JTextArea manageTextArea, JTextArea downloadTextArea){
        this.loginTextArea = loginTextArea;
        this.manageTextArea = manageTextArea;
        this.downloadTextArea = downloadTextArea;
        loginLock = new ReentrantLock();
        manageLock = new ReentrantLock();
        downloadLock = new ReentrantLock();
    }

    /**
     * <p> This method prints the given text to the client GUI loginTextArea. It uses a lock
     * to ensure only one thread can access the text area at one time. Appends current hour, minute and second
     * to the message printed.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}, {@link serviceClient.ConnectionListener} </p>
     *
     * @param text String text to be printed to the client GUI.
     */
    public void printToLoginTextArea(String text) {
        try {
            loginLock.lock();
            // append current time, text and new line character to the JTextArea
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            loginTextArea.append("("+ dtf.format(LocalDateTime.now()) + ") " + text + "\n");
            // scroll down to the bottom of the JTextArea
            DefaultCaret caret = (DefaultCaret) loginTextArea.getCaret();
            caret.setUpdatePolicy(ALWAYS_UPDATE);
        } finally {
            loginLock.unlock();
        }
    }

    /**
     * <p> This method prints the given text to the client GUI manageTextArea. It uses a lock
     * to ensure only one thread can access the text area at one time. Appends current hour, minute and second
     * to the message printed.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI} </p>
     *
     * @param text String text to be printed to the client GUI.
     */
    public void printToManageTextArea(String text) {
        try {
            manageLock.lock();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            manageTextArea.append("("+ dtf.format(LocalDateTime.now()) + ") " + text + "\n");
            DefaultCaret caret = (DefaultCaret) manageTextArea.getCaret();
            caret.setUpdatePolicy(ALWAYS_UPDATE);
        } finally {
            manageLock.unlock();
        }
    }

    /**
     * <p> This method prints the given text to the client GUI downloadTextArea. It uses a lock
     * to ensure only one thread can access the text area at one time. Appends current hour, minute and second
     * to the message printed.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}, {@link serviceClient.FileTransferHandler} </p>
     *
     * @param text String text to be printed to the client GUI.
     */
    public void printToDownloadTextArea(String text) {
        try {
            downloadLock.lock();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            downloadTextArea.append("("+ dtf.format(LocalDateTime.now()) + ") " + text + "\n");
            DefaultCaret caret = (DefaultCaret) downloadTextArea.getCaret();
            caret.setUpdatePolicy(ALWAYS_UPDATE);
        } finally {
            downloadLock.unlock();
        }
    }
}
