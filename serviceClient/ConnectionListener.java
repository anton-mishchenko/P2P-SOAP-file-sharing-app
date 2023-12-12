/*
 * 2023/10/13
 * Anton Mishchenko
 */

package serviceClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * <p> ConnectionListener class is responsible for listening for incoming connections from peers
 * without blocking the main thread. If it acquires a connection with peer it will attempt to
 * transfer files using FileTransferHandler class. Also contains a method to close the socket
 * used for listening for connections.</p>
 */
public class ConnectionListener implements Runnable{
    private ServerSocket serverSocket;
    private boolean keepListening = true;
    ClientOutputManager clientOutputManager;

    /**
     * <p>Constructor for ConnectionListener class. Assigns ClientOutputManager object and calls openPort method to
     * initialize port to listen on.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link serviceClient.ConnectionListener#openPort(int)}</p>
     *
     * @param serverPort int server port to listen on for incoming connections.
     * @param clientOutputManager ClientOutputManager object used to print messages to GUI.
     */
    public ConnectionListener(int serverPort, ClientOutputManager clientOutputManager) {
        this.clientOutputManager = clientOutputManager;
        openPort(serverPort);
    }

    /**
     * <p> Method used to initialize port to listen on for incoming connections.</p>
     *
     * <p> Called by: {@link serviceClient.ConnectionListener#ConnectionListener(int, ClientOutputManager)}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToLoginTextArea(String)}</p>
     *
     * @param serverPort int server port to listen on for incoming connections.
     */
    private void openPort(int serverPort){
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            clientOutputManager.printToLoginTextArea("ERROR: ConnectionListener failed to open server port."+"\n"
                    +"Please restart the Client and try again.");
        } catch (IllegalArgumentException e){
            clientOutputManager.printToLoginTextArea("ERROR: ConnectionListener failed to create server port."+"\n"
                    +"Enter correct port number.");
        }
    }

    /**
     * <p> Method that listens for incoming connections from peers.</p>
     *
     * <p> Called by: {@link serviceClient.ConnectionListener#run()}</p>
     *
     * <p> Calls: {@link  serviceClient.ClientOutputManager#printToLoginTextArea(String)}</p>
     *
     * @return Socket that represents the connection with the peer.
     */
    public Socket listenForPeer() {
        Socket sendSocket = null;
        try {
            // blocks and waits for connection
            sendSocket = serverSocket.accept();
        } catch (IOException e) {
            // don't print error if socket was closed on peruse
            if(keepListening) {
                clientOutputManager.printToLoginTextArea("ERROR: While waiting for connection with peers." + "\n"
                        + "Please reconnect the Client and try again.");
            }
        }catch (NullPointerException e){
            clientOutputManager.printToLoginTextArea("ERROR: Could not create socket to listen on." +"\n"
                    +"Please reconnect the Client and try again.");
        }
        return sendSocket;
    }

    /**
     * <p> Main loop of the class that is responsible for listening for incoming connections from peers.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link serviceClient.ConnectionListener#listenForPeer()}</p>
     * <p> Calls: {@link serviceClient.FileTransferHandler#sendFile(Socket)}</p>
     */
    @Override
    public void run() {
        while(keepListening) {
            Socket sendSocket = listenForPeer();
            // if an error caused sendSocket to be null, skip this iteration and wait for another connection
            if(sendSocket == null) {
                continue;
            }
            // attempt file transfer using FileTransferHandler class
            FileTransferHandler transferFile = new FileTransferHandler(clientOutputManager);
            transferFile.sendFile(sendSocket);
        }
    }

    /**
     * <p> Method that closes the connection listener. Used by ClientGUI class to restart connection
     * listener when issues result in client having to log back in.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     */
    public void closeConnectionListener(){
        try {
            keepListening = false;
            serverSocket.close();
        } catch (Exception e) {
            System.out.println("ERROR: Could not close connection listener.");
        }
    }
}