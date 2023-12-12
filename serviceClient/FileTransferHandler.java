/*
 * 2023/10/13
 * Anton Mishchenko
 */

package serviceClient;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * <p> FileTransferHandler class is responsible for handling file transfers between peers. It
 * contains two threaded methods sendFile and requestFile. sendFile is called when a peer
 * requests a file. requestFile is called when this client requests some file from a peer.</p>
 */
public class FileTransferHandler{
    private ClientOutputManager clientOutputManager;
    public FileTransferHandler(ClientOutputManager clientOutputManager){
        this.clientOutputManager = clientOutputManager;
    }
    /**
     * <p> This method is called by ConnectionListener when a peer requests a file from this client.
     * It parses the request and calls transferFile method to send the file to the requesting peer. </p>
     *
     * <p> Called by: {@link serviceClient.ConnectionListener}</p>
     *
     * <p> Calls: {@link serviceClient.FileTransferHandler#transferFile}</p>
     *
     * @param sendSocket Socket connection object of peer to which the file will be sent.
     */
    public void sendFile(final Socket sendSocket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = null;
                BufferedReader dataInputStream = null;
                OutputStream out = null;
                try {
                    // get requested file information from the peer
                    dataInputStream = new BufferedReader(new InputStreamReader(sendSocket.getInputStream()));
                    out = sendSocket.getOutputStream();
                    String request = dataInputStream.readLine();
                    // parse request string and get file form it
                    request = request.split(" ")[1];
                    String fileName = request.replace("%20", " ");
                    file = new File(fileName);
                    // check if such file exists
                    if (!file.exists()) {
                        out.write("HTTP/1.1 404 Not Found\n".getBytes());
                        try {
                            dataInputStream.close();
                            sendSocket.close();
                        } catch (IOException e) {
                            System.out.println("ERROR: While closing streams.");
                        }
                        return;
                    }
                }catch (IOException e) {
                    System.out.println("ERROR: Input stream error while attempting to send file.");
                }
                try{
                    // confirm that File file was created
                    if(file==null){
                        return;
                    }
                    // create relevant streams and transfer file to peer
                    FileInputStream fileInputStream = new FileInputStream(file);
                    transferFile(fileInputStream, out);
                    // close streams
                    try {
                        fileInputStream.close();
                        out.close();
                        dataInputStream.close();
                        sendSocket.close();
                    } catch (IOException e) {
                        System.out.println("ERROR: While closing streams after sending file.");
                    }
                }catch (SocketException e) {
                    System.out.println("ERROR: While accessing socket during file transfer.");
                }catch (IOException e) {
                    System.out.println("ERROR: While accessing file during file transfer.");
                }
            }
        }).start();
    }

    /**
     * <p> This method takes cre of the actual transfer of bytes between streams. </p>
     *
     * <p> Called by: {@link serviceClient.FileTransferHandler#sendFile}</p>
     *
     * @param fileInputStream FileInputStream of the file to be sent to peer.
     * @param out OutputStream of peer socket to which the file is sent.
     * @throws IOException If an I/O error occurs during transfer.
     */
    private void transferFile( FileInputStream fileInputStream, OutputStream out ) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        // transfer data
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }

    /**
     * <p> This method is called by when this client requests a file from a peer. It creates
     * relevant streams correctly names a new file and utilizes progress bar to display the
     * progress of the file transfer.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * <p> Calls: {@link FileTransferHandler#setFileCopyNumber}, {@link FileTransferHandler#downloadFile},
     * {@link FileTransferHandler#displayError}</p>
     *
     * @param progressBar Progress bar object to be updated during transfer.
     * @param ownerData String array contains IP, port and path of the peer for request.
     * @param fileName Name of file to be requested.
     * @param fileType Type of file to be requested.
     * @param fileSize Size of file to be requested.
     */
    public void requestFile(final ProgressBar progressBar, final String[] ownerData, final String fileName, final String fileType, final long fileSize) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // initialize variables
                File newFile=null;
                FileOutputStream fileOutputStream = null;
                DataInputStream dataInputStream = null;
                Socket requestSocket = null;
                try {
                    // parse file owner data
                    String ownerIP = ownerData[0];
                    int ownerPort = Integer.parseInt(ownerData[1]);
                    String ownerPath = ownerData[2];
                    // create request socket and adjust timeout to 10 seconds
                    requestSocket = new Socket(ownerIP, ownerPort);
                    requestSocket.setSoTimeout(10000);
                    OutputStream out = requestSocket.getOutputStream();
                    // write and send simple GET request
                    String request = ownerPath + fileName + "." + fileType + "\n";
                    request = "GET " + request.replaceAll(" ", "%20");
                    out.write(request.getBytes());
                    out.flush();
                    // create file with correct name
                    newFile = new File(fileName + "." + fileType);
                    newFile = setFileCopyNumber(newFile, fileName, fileType);
                    // create steams and adjust progressBar
                    fileOutputStream = new FileOutputStream(newFile);
                    dataInputStream = new DataInputStream(requestSocket.getInputStream());
                    progressBar.getProgressBar().setStringPainted(true);
                    // make sure progress bar is displayed correctly
                    if(fileName.length()>30)
                        progressBar.getProgressBar().setString("Downloading: " + fileName.substring(0,30) + "..." + "." + fileType);
                    else
                        progressBar.getProgressBar().setString("Downloading: " + fileName + "." + fileType);
                    downloadFile(fileOutputStream, dataInputStream, progressBar, newFile, fileSize);
                }catch (ConnectException e) {
                    displayError("ERROR: Could not connect to peer", progressBar, fileOutputStream, newFile);
                }catch (SocketTimeoutException e){
                    displayError("ERROR: Peer timed out", progressBar, fileOutputStream, newFile);
                }catch (SocketException e) {
                    displayError("ERROR: Socket error", progressBar, fileOutputStream, newFile);
                }catch (IOException e) {
                    displayError("ERROR: IO error", progressBar, fileOutputStream, newFile);
                }finally {
                    // close streams and socket
                    try {
                        if (fileOutputStream != null)
                            fileOutputStream.close();
                        if (dataInputStream != null)
                            dataInputStream.close();
                        if (requestSocket != null)
                            requestSocket.close();
                    } catch (IOException ex) {
                        System.out.println("ERROR: When closing file transfer streams.");
                    }
                }

            }
        }).start();
    }

    /**
     * <p> This method is called to correctly name the new file to be created when a copy of the
     * file already exists. It loops through existing files with the same name and increments last digit.</p>
     *
     * <p> Called by: {@link serviceClient.FileTransferHandler#requestFile}</p>
     *
     * @param newFile File to be created.
     * @param fileName Name of the file to be created.
     * @param fileType Type of file to be created.
     * @return File of the correctly named file.
     */
    private File setFileCopyNumber(File newFile, String fileName, String fileType){
        int fileNum = 1;
        // add copy number to file name
        while (newFile.exists()) {
            newFile = new File(fileName + "(" + fileNum + ")." + fileType);
            fileNum++;
            // replace file if more than 1000 copies already exist
            if (fileNum > 1000) {
                newFile = new File(fileName + "." + fileType);
                break;
            }
        }
        return newFile;
    }

    /**
     * <p> This method facilitates the actual download the file from the peer. It reads bytes from
     * input stream and writes them to file. It updates the progress bar during download.</p>
     *
     * <p> Called by: {@link serviceClient.FileTransferHandler#requestFile}</p>
     *
     * <p> Calls: {@link serviceClient.FileTransferHandler#displayError}</p>
     *
     * @param fileOutputStream FileOutputStream of file to be created.
     * @param dataInputStream DataInputStream of the peer socket.
     * @param progressBar Progress bar to update during download.
     * @param newFile File to be created.
     * @param fileSize Size of the file to download.
     * @throws IOException If I/O error happens while transferring data.
     */
    private void downloadFile(FileOutputStream fileOutputStream, DataInputStream dataInputStream, ProgressBar progressBar, File newFile, long fileSize) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        // read first line of response
        if((bytesRead = dataInputStream.read(buffer)) != -1) {
            String s = new String(buffer, 0, bytesRead);
            // check if file exists on peer machine
            if (s.contains("HTTP/1.1 404 Not Found")) {
                displayError("ERROR: File not found on peer machine.", progressBar, fileOutputStream, newFile);
                return;
            } else {
                fileOutputStream.write(buffer, 0, bytesRead);
                progressBar.getProgressBar().setValue((int) (newFile.length() * 100 / fileSize));
            }
        }
        // transfer data
        while ((bytesRead = dataInputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
            // update progress bar
            progressBar.getProgressBar().setValue((int) (newFile.length() * 100 / fileSize));
            // sleep here to mimic large file download
            try{
                Thread.sleep(10);
            }catch (InterruptedException e){
                System.out.println("ERROR: Download thread interrupted while sleeping.");
                displayError("ERROR: Occurred during file transfer.", progressBar, fileOutputStream, newFile);
            }
        }
        if(bytesRead == -1){
            clientOutputManager.printToDownloadTextArea("File download complete.");
        }
    }

    /**
     * <p> This method is called to display an error message to console, progress bar and close
     * FileOutputStream. It will also delete the malformed file that was created.</p>
     *
     * <p> Called by: {@link serviceClient.FileTransferHandler#requestFile}, {@link serviceClient.FileTransferHandler#downloadFile}</p>
     *
     * @param progressBarMessage Message that will be displayed on progress bar.
     * @param progressBar Progress bar to be updated.
     * @param fileOutputStream FileOutputStream to close.
     * @param newFile File to delete.
     */
    private void displayError(String progressBarMessage, ProgressBar progressBar, FileOutputStream fileOutputStream, File newFile){
        progressBar.getProgressBar().setString(progressBarMessage);
        try {
            if (fileOutputStream != null)
                fileOutputStream.close();
            if (newFile != null && newFile.exists()) {
                newFile.delete();
            }
        } catch (IOException ex) {
            System.out.println("ERROR closing streams");
        }
    }
}
