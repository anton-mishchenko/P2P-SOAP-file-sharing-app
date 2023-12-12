/*
 * 2023/10/13
 * Anton Mishchenko
 */

package serviceClient;

import javax.swing.*;

/**
 * <p> Main method for the client program. Creates GUI for the user to interact with. Creates output manager and sets it
 * for displaying messages in the GUI.</p>
 */
public class ClientMain {
    public static void main(String[] args){
        // use event dispatch thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ClientGUI clientGUI = new ClientGUI();
                ClientOutputManager outputManager = new ClientOutputManager(clientGUI.getLoginTextArea(), clientGUI.getManageTextArea(), clientGUI.getDownloadTextArea());
                clientGUI.setOutputManager(outputManager);
            }
        });
    }
}
