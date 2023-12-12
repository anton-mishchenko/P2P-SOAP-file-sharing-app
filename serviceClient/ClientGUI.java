/*
 * 2023/10/13
 * Anton Mishchenko
 */

package serviceClient;

import com.sun.xml.internal.ws.client.ClientTransportException;
import server.P2PServiceImplSEI;
import server.P2PServiceImplService;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.xml.ws.WebServiceException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p> ClientGUI class is responsible for creating a GUI for the Client application, providing additional classes to
 * be used by worker threads in the ActionListeners for the GUI elements, providing methods for communication with
 * the server and methods needed by the classes.</p>
 * <p> It runs an automatic heartbeat function as long as application is running, notifies the server when the
 * the Client application is shut down and checks if sessions can be resumed when internet issues occur.</p>
 * <p> Server communication is done through WebService by using the ServerInterfaceImpl class.</p>
 */
public class ClientGUI extends JFrame {
    private List<ProgressBar> progressBarList = new ArrayList<>();
    private volatile boolean sendHeartBeats = true;
    private ConnectionListener connectionListener;
    private P2PServiceImplSEI P2PServiceImpl;
    protected static final Lock uiLock = new ReentrantLock();
    private static final Lock tblDownloadLock = new ReentrantLock();
    private static final Lock tblUserFilesLock = new ReentrantLock();
    private String userName;
    private String token = "";
    private JTabbedPane tabbedPane;
    private JLabel lblServerStatus;
    private JTextField txtUserName;
    private JTextField txtUserPassword;
    private JTextField txtUserPort;
    private JButton btnLogIn;
    public static JTextArea serverLoginText;
    private JTextField txtUploadFilePath;
    private JButton btnRegisterFile;
    private JTable tblUserFiles;
    private JButton btnUnregisterFile;
    private JTextArea serverManageText;
    private JTextField txtSearchFile;
    private JButton btnDownloadFile;
    private JTable tblDownloadFiles;
    private JTextArea serverDownloadText;
    private DefaultTableModel tableModel;
    private DefaultTableModel tableModel2;
    private JPanel downloadProgressPanel;
    private volatile SelectedFile fileToRemove;
    private SelectedFile fileToDownload;
    private int portNumber;
    private String ip;
    private File fileToRegister;
    private SwingWorker<String,Void> worker;
    private JButton btnExitLogin;
    private JButton btnChooseFile;
    private JButton btnUpdateList;
    private JButton btnExitManage;
    private JButton btnSearchFile;
    private JButton btnExitDownload;
    private ClientOutputManager clientOutputManager;

    /**
     * <p> Constructor for the ClientGUI class. It sets the title and default close operation of the GUI.
     * Additionally it calls createUI method to initiate and create GUI elements. </p>
     *
     * <p> Called by: {@link serviceClient.ClientMain#main(String[])}</p>
     *
     * <p> Calls: {@link #createUI()}</p>
     */
    public ClientGUI() {
        super("P2P Client");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        createUI();
    }

    /**
     * <p> This method is used to assign clientOutputManager to be used by the ClientGUI class. </p>
     *
     * <p> Called by: {@link serviceClient.ClientMain#main(String[])}</p>
     *
     * @param clientOutputManager ClientOutputManager object used to output messages to the GUI
     */
    public void setOutputManager(ClientOutputManager clientOutputManager){
        this.clientOutputManager = clientOutputManager;
    }

    /**
     * <p> This method for the ClientGUI class creates GUI elements and positions them on the frame. In
     * addition it adjusts element properties, sets action listeners, table listeners and
     * sets default shut down behaviour.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#ClientGUI()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientGUI#setShutDown}, {@link serviceClient.ClientGUI#setButtonActionListeners},
     * {@link serviceClient.ClientGUI#setTableSelectionListeners}</p>
     */
    private void createUI() {
        JPanel pnlMain = new JPanel(new BorderLayout());
        tabbedPane = new JTabbedPane();

        // loginTab sections
        JPanel loginTab = new JPanel();
        loginTab.setLayout(new BoxLayout(loginTab, BoxLayout.Y_AXIS));
        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Server Connection"));
        JPanel loginOutputPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        loginOutputPanel.setBorder(BorderFactory.createTitledBorder("Error and Status Messages"));
        lblServerStatus = new JLabel("Not connected");
        lblServerStatus.setForeground(Color.RED);
        // login server elements
        JLabel lblUserName = new JLabel("Username: ");
        JLabel lblUserPassword = new JLabel("Password: ");
        JLabel lblUserPort = new JLabel("Port: ");
        txtUserName = new JTextField("User1", 20);
        txtUserPassword = new JTextField("123456", 20);
        txtUserPort = new JTextField("1052", 20);
        btnLogIn = new JButton("Login");
        // other login elements
        btnExitLogin = new JButton("Exit");
        serverLoginText = new JTextArea(5, 20);

        // additional elements and their adjustments
        JPanel loginLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        loginLabelPanel.setPreferredSize(new Dimension(100, 130));
        JPanel loginFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        loginFieldPanel.setPreferredSize(new Dimension(250, 130));
        loginOutputPanel.setPreferredSize(new Dimension(550, 600));
        // additional login elements and their adjustments
        loginLabelPanel.add(lblUserName);
        loginLabelPanel.add(lblUserPassword);
        loginLabelPanel.add(lblUserPort);
        loginLabelPanel.add(btnLogIn);
        btnLogIn.setEnabled(true);
        loginFieldPanel.add(txtUserName);
        loginFieldPanel.add(txtUserPassword);
        loginFieldPanel.add(txtUserPort);
        loginFieldPanel.add(lblServerStatus);
        // text area adjustments
        serverLoginText.setEditable(false);
        loginOutputPanel.add(btnExitLogin);
        JScrollPane loginTextScroll = new JScrollPane(serverLoginText);
        loginTextScroll.setPreferredSize(new Dimension(450, 300));
        loginOutputPanel.add(loginTextScroll);
        // add elements to loginTab
        loginTab.add(loginPanel);
        loginPanel.add(loginLabelPanel);
        loginPanel.add(loginFieldPanel);
        loginTab.add(loginOutputPanel);

        // ManageTab sections
        JPanel manageTab = new JPanel();
        manageTab.setLayout(new BoxLayout(manageTab, BoxLayout.Y_AXIS));
        JPanel choosePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        choosePanel.setBorder(BorderFactory.createTitledBorder("Choose File"));
        JPanel managePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        managePanel.setBorder(BorderFactory.createTitledBorder("Manage Files"));
        managePanel.setPreferredSize(new Dimension(450, 300));
        JPanel manageOutputPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        manageOutputPanel.setBorder(BorderFactory.createTitledBorder("Error and Status Messages"));
        // ManageTab elements
        btnChooseFile = new JButton("Choose File");
        txtUploadFilePath = new JTextField("File Name", 20);
        btnRegisterFile = new JButton("Register File");
        btnUpdateList = new JButton("Update File List");
        JScrollPane registeredFilesScrollPane = new JScrollPane();
        registeredFilesScrollPane.setPreferredSize(new Dimension(450, 200));
        tblUserFiles = new JTable();
        registeredFilesScrollPane.setViewportView(tblUserFiles);
        btnUnregisterFile = new JButton("Unregister Files");
        btnUnregisterFile.setEnabled(false);
        btnExitManage = new JButton("Exit");
        serverManageText = new JTextArea(5, 20);
        serverManageText.setEditable(false);
        JScrollPane manageTextScroll = new JScrollPane(serverManageText);
        manageTextScroll.setPreferredSize(new Dimension(450, 300));
        // ManageTab panel adjustments
        choosePanel.setPreferredSize(new Dimension(300, 60));
        JPanel manageButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        manageButtonsPanel.setPreferredSize(new Dimension(500, 80));
        JPanel manageTablePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        manageTablePanel.setPreferredSize(new Dimension(500, 230));
        // add elements to ManageTab
        choosePanel.add(btnChooseFile);
        choosePanel.add(txtUploadFilePath);
        choosePanel.add(btnRegisterFile);
        manageTab.add(choosePanel);
        manageButtonsPanel.add(btnUpdateList);
        manageButtonsPanel.add(btnUnregisterFile);
        manageTablePanel.add(registeredFilesScrollPane);
        managePanel.add(manageButtonsPanel);
        managePanel.add(manageTablePanel);
        manageTab.add(managePanel);
        manageOutputPanel.add(btnExitManage);
        manageOutputPanel.add(manageTextScroll);
        manageTab.add(manageOutputPanel);

        // DownloadTab sections
        JPanel downloadTab = new JPanel();
        downloadTab.setLayout(new BoxLayout(downloadTab, BoxLayout.Y_AXIS));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search File"));
        JPanel downloadPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        downloadPanel.setBorder(BorderFactory.createTitledBorder("Download File"));
        JPanel downloadOutputPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        downloadOutputPanel.setBorder(BorderFactory.createTitledBorder("Error and Status Messages"));
        JPanel downloadStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        downloadStatusPanel.setBorder(BorderFactory.createTitledBorder("Downloads"));
        // DownloadTab elements
        btnSearchFile = new JButton("Search File");
        txtSearchFile = new JTextField("", 20);
        btnDownloadFile = new JButton("Download File");
        btnDownloadFile.setEnabled(false);
        JScrollPane foundFilesScrollPane = new JScrollPane();
        foundFilesScrollPane.setPreferredSize(new Dimension(450, 200));
        tblDownloadFiles = new JTable();
        foundFilesScrollPane.setViewportView(tblDownloadFiles);
        btnExitDownload = new JButton("Exit");
        serverDownloadText = new JTextArea(5, 20);
        serverDownloadText.setEditable(false);
        JScrollPane downloadTextScroll = new JScrollPane(serverDownloadText);
        downloadTextScroll.setPreferredSize(new Dimension(450, 300));
        downloadProgressPanel = new JPanel();
        downloadProgressPanel.setLayout(new BoxLayout(downloadProgressPanel, BoxLayout.Y_AXIS));
        JScrollPane downloadsScrollPane = new JScrollPane(downloadProgressPanel);
        downloadsScrollPane.setPreferredSize(new Dimension(450, 100));
        // DownloadTab panel adjustments
        searchPanel.setPreferredSize(new Dimension(300, 60));
        downloadPanel.setPreferredSize(new Dimension(450, 330));
        // add elements to DownloadTab
        searchPanel.add(btnSearchFile);
        searchPanel.add(txtSearchFile);
        searchPanel.add(btnDownloadFile);
        downloadTab.add(searchPanel);
        downloadPanel.add(foundFilesScrollPane);
        downloadPanel.add(downloadsScrollPane);
        downloadTab.add(downloadPanel);
        downloadOutputPanel.add(btnExitDownload);
        downloadOutputPanel.add(downloadTextScroll);
        downloadTab.add(downloadOutputPanel);

        // Add tabs and panels
        super.add(pnlMain);
        pnlMain.add(tabbedPane);
        tabbedPane.addTab("Login", loginTab);
        tabbedPane.addTab("Manage", manageTab);
        tabbedPane.addTab("Download", downloadTab);
        // make manageTab and downloadTab inaccessible until login is successful
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setEnabledAt(2, false);

        // adjust table settings
        tableModel = new DefaultTableModel(null, new String [] {"File ID", "File Name", "File Type", "File Path", "File Size"});
        tblUserFiles.setModel(tableModel);
        // make the table uneditable
        tblUserFiles.setDefaultEditor(Object.class, null);
        // disable column reordering
        tblUserFiles.getTableHeader().setReorderingAllowed(false);

        tableModel2 = new DefaultTableModel(null, new String[]{"File ID", "File Name", "File Type", "File Size"});
        tblDownloadFiles.setModel(tableModel2);
        tblDownloadFiles.setDefaultEditor(Object.class, null);
        tblDownloadFiles.getTableHeader().setReorderingAllowed(false);

        super.setVisible(true);
        super.pack();
        // add shut down hook
        setShutDown();
        setButtonActionListeners();
        setTableSelectionListeners();
    }

    /**
     * <p> This method is responsible for creating a shut down hook for notifying server
     * that client is disconnecting. Hook is called automatically when Client application
     * is shut down.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#createUI()}</p>
     *
     * <p> Calls: {P2PServiceSEI#disconnectFromServer}</p>
     */
    private void setShutDown(){
        // create shut down hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    // notify server of disconnect
                    List <String> response = P2PServiceImpl.disconnectFromServer(token, userName).getItem();
                    String[] arrayResponse = response.toArray(new String[0]);
                    System.out.println(arrayResponse[0]);
                    System.out.println(arrayResponse[1]);
                } catch (Exception e){
                    System.out.println("Error disconnecting from server. Exiting..." + "\n" + e.getMessage());
                }
            }
        }));
    }

    /**
     * <p> Sets action listeners for the buttons. Action listeners create
     * worker threads to complete button actions. Allows GUI to stay responsive.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#createUI()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientGUI#loginButtonListener}, {@link serviceClient.ClientGUI#chooseFileListener},
     * {@link serviceClient.ClientGUI#registerFileListener}, {@link serviceClient.ClientGUI#updateListListener},
     * {@link serviceClient.ClientGUI#unregisterFileListener}, {@link serviceClient.ClientGUI#searchFileListener},
     * {@link serviceClient.ClientGUI#downloadFileListener}, {@link serviceClient.ClientGUI#setLoginUi}</p>
     */
    public void setButtonActionListeners(){
        btnLogIn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        setLoginUi(false, "Connecting to server...", Color.PINK);
                        loginButtonListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnExitLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        System.exit(0);
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnChooseFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        chooseFileListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnRegisterFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        registerFileListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnUpdateList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        updateListListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnUnregisterFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        unregisterFileListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnExitManage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        System.exit(0);
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnSearchFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        searchFileListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnDownloadFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        downloadFileListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnExitDownload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        System.exit(0);
                        return null;
                    }
                };
                worker.execute();
            }
        });
    }

    /**
     * <p> This method is responsible for setting selection listeners for the tables in the GUI. ListSelectionListeners
     * use worker threads to complete their tasks. Allows GUI to stay responsive.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#createUI()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientGUI#registeredFilesTableListener},
     * {@link serviceClient.ClientGUI#downloadFilesTableListener}</p>
     */
    public void setTableSelectionListeners(){
        tblUserFiles.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        registeredFilesTableListener ();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        tblDownloadFiles.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        downloadFilesTableListener ();
                        return null;
                    }
                };
                worker.execute();
            }
        });
    }

    /**
     * <p> This method is responsible for updating information of the file user selects from the management table and
     * updating GUI elements to reflect the selected file.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setTableSelectionListeners()}</p>
     *
     * <p> Calls: {@link serviceClient.SelectedFile#getName()}</p>
     */
    private void registeredFilesTableListener () {
        int selectedRow = tblUserFiles.getSelectedRow();
        // Check if a valid selection is made
        if (selectedRow != -1) {
            // get file information from table
            fileToRemove = new SelectedFile(
                    Integer.parseInt((String) tblUserFiles.getValueAt(selectedRow, 0)),
                    (String) tblUserFiles.getValueAt(selectedRow, 1),
                    (String) tblUserFiles.getValueAt(selectedRow, 2),
                    (String) tblUserFiles.getValueAt(selectedRow, 3),
                    Integer.parseInt((String) tblUserFiles.getValueAt(selectedRow, 4)));
            // adjust button properties to reflect selected file
            btnUnregisterFile.setEnabled(true);
            String fileName = fileToRemove.getName();
            if(fileName.length()>45)
                btnUnregisterFile.setText("Unregister: " + fileName.substring(0,45)+"...");
            else
                btnUnregisterFile.setText("Unregister: " + fileName);
        }
    }

    /**
     * <p> This method is responsible for updating information of the file user selects from the download table and
     * updating GUI elements to reflect the selected file.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setTableSelectionListeners()}</p>
     *
     * <p> Calls: {@link serviceClient.SelectedFile#getName()}</p>
     */
    private void downloadFilesTableListener (){
        int selectedRow = tblDownloadFiles.getSelectedRow();
        // Check if a valid selection is made
        if (selectedRow != -1) {
            // get file information from table
            fileToDownload = new SelectedFile(
                    Integer.parseInt((String) tblDownloadFiles.getValueAt(selectedRow, 0)),
                    (String) tblDownloadFiles.getValueAt(selectedRow, 1),
                    (String) tblDownloadFiles.getValueAt(selectedRow, 2),
                    null,
                    Integer.parseInt((String) tblDownloadFiles.getValueAt(selectedRow, 3)));
            // adjust button properties to reflect selected file
            btnDownloadFile.setEnabled(true);
            String fileName = fileToDownload.getName();
            if(fileName.length()>45)
                btnDownloadFile.setText("Download: " + fileName.substring(0,45)+"...");
            else
                btnDownloadFile.setText("Download: " + fileName);
        }
    }

    /**
     * <p> This method is responsible for login features of the Client program.
     * It retrieves information from user inputs, verifies it and attempts to connect to
     * the server. Then it parses server response and displays it to the user and allowing further access
     * to different features of the Client.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setButtonActionListeners()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToLoginTextArea}, {@link serviceClient.ClientGUI#setLoginUi},
     * {@link serviceClient.ClientGUI#isServerPortTaken}, {@link serviceClient.ClientGUI#resumeSession},
     * {@link server.P2PServiceImplSEI#connectToServer}, {@link serviceClient.ClientGUI#guiLoggedIn}</p>
     */
    private void loginButtonListener(){
        try {
            P2PServiceImplService service = new P2PServiceImplService();
            P2PServiceImpl = service.getP2PServiceImplPort();
        } catch (WebServiceException e) {
            clientOutputManager.printToLoginTextArea("Error connecting to server.");
            setLoginUi(true, "Not connected", Color.RED);
            return;
        }
        try {
            List <String> response;
            String[] arrayResponse = null;
            // initialize variables
            ip = Inet4Address.getLocalHost().getHostAddress();
            portNumber = Integer.parseInt(txtUserPort.getText());
            userName = txtUserName.getText();
            String password = txtUserPassword.getText();
            // attempt to resume session, in case client experienced connection loss
            if(resumeSession()){
                return;
            }
            // test if port number is valid length
            if (portNumber < 0) {
                clientOutputManager.printToLoginTextArea("Login Error: Port number cannot be negative");
                return;
            } else if ( portNumber > 65535) {
                clientOutputManager.printToLoginTextArea("Login Error: Port number cannot be greater than 65535");
                return;
            }
            // test if password is valid length
            if(password.length()>50){
                clientOutputManager.printToLoginTextArea("Login Error: Password cannot be longer than 50 characters");
                return;
            } else if(password.length()<6){
                clientOutputManager.printToLoginTextArea("Login Error: Password cannot be shorter than 6 characters");
                return;
            }
            // test if username is valid length
            if(userName.length()>25){
                clientOutputManager.printToLoginTextArea("Login Error: Username cannot be longer than 25 characters");
                return;
            } else if(userName.length()<5){
                clientOutputManager.printToLoginTextArea("Login Error: Username cannot be less than 5 characters");
                return;
            }
            // test if port number is taken
            if(isServerPortTaken()){
                clientOutputManager.printToLoginTextArea("Port " + portNumber + " is in use, use different port number.");
                return;
            }
            // attempt to connect to server
            response = P2PServiceImpl.connectToServer(userName, txtUserPassword.getText(), ip, portNumber).getItem();
            arrayResponse= response.toArray(new String[0]);
            // act based on response
            // good login
            if (arrayResponse[0].equals("OK")) {
                guiLoggedIn();
                token = arrayResponse[1];
                clientOutputManager.printToLoginTextArea("Login successful.");
                // good login, user information updated
            } else if (arrayResponse[0].equals("UPDATE")){
                guiLoggedIn();
                token = arrayResponse[1];
                clientOutputManager.printToLoginTextArea("Login successful." + "\n" +
                        "Account information updated.");
                // good login, new account created
            } else if (arrayResponse[0].equals("NEW")) {
                guiLoggedIn();
                token = arrayResponse[1];
                clientOutputManager.printToLoginTextArea("Login successful." + "\n" +
                        "New account created.");
                // user already logged in
            } else if (arrayResponse[0].equals("COPY")){
                clientOutputManager.printToLoginTextArea("User with this name is already logged in.");
                clientOutputManager.printToLoginTextArea("If you were logged in as " + userName + "\n" +
                        "and lost Server connection, " + "\n" +
                        "please wait 2 minutes and try again.");
                // server full
            }else if (arrayResponse[0].equals("FULL")) {
                clientOutputManager.printToLoginTextArea("Server is currently full. Try again later.");
                // user session restored
            }else if (arrayResponse[0].equals("PASSWORD")){
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
            }else{
                clientOutputManager.printToLoginTextArea("Login Error.");
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                setLoginUi(true, "Not connected", Color.RED);
            }
        } catch (UnknownHostException | NullPointerException ex) {
            clientOutputManager.printToLoginTextArea("Login Error: " + "\n" +
                    ex);
            setLoginUi(true, "Not connected", Color.RED);
        } catch (WebServiceException ex) {
            clientOutputManager.printToLoginTextArea("Login Error: Server communication error.");
            setLoginUi(true, "Not connected", Color.RED);
        } catch (NumberFormatException n){
            clientOutputManager.printToLoginTextArea("Login Error: Invalid port number.");
            setLoginUi(true, "Not connected", Color.RED);
        }
    }

    /**
     * <p> This method is used to resume a session if the client experienced connection loss.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#loginButtonListener()}
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToLoginTextArea}, {@link serviceClient.ClientGUI#setLoginUi},
     * {@link server.P2PServiceImplSEI#resumeSession}, {@link serviceClient.ClientGUI#guiLoggedIn}</p>
     *
     * @return true if session was resumed, false if it was not
     */
    private boolean resumeSession(){
        List <String> response = null;
        String[] arrayResponse = null;
        // attempt to resume session
        try {
            response = P2PServiceImpl.resumeSession(token, userName, ip, portNumber).getItem();
            arrayResponse = response.toArray(new String[0]);
        } catch (WebServiceException | NullPointerException e) {
            clientOutputManager.printToLoginTextArea("Login Error: Server communication error.");
        }
        // parse response
        if (arrayResponse != null) {
            if(arrayResponse[0].equals("OK")){
                token = arrayResponse[1];
                guiLoggedIn();
                clientOutputManager.printToLoginTextArea("Login successful" + "\n" +
                        "Session resumed");
                return true;
            } else if(arrayResponse[0].equals("UPDATE")){
                token = arrayResponse[1];
                guiLoggedIn();
                clientOutputManager.printToLoginTextArea("Login successful" + "\n" +
                        "Session resumed" + "\n" +
                        "Account information updated");
                return true;
            } else if(arrayResponse[0].equals("CRED")){
                clientOutputManager.printToLoginTextArea("Could not resume session. Attempting new session.");
            }
        }
        setLoginUi(true, "Not connected", Color.RED);
        return false;
    }

    /**
     * <p> This method is responsible for creating a dialog window
     * for the user to select a file that they want to register on the server.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setButtonActionListeners()}</p>
     */
    private void chooseFileListener (){
        // open file chooser dialogue
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        // if file is chosen, get its information
        if (result == JFileChooser.APPROVE_OPTION) {
            fileToRegister = fileChooser.getSelectedFile();
            txtUploadFilePath.setText(fileToRegister.getAbsolutePath());
            txtUploadFilePath.setForeground(Color.BLACK);
        }
    }

    /**
     * <p> This method is responsible for verifying the user chosen file, acquiring its data and sending it
     * to the server. It then updates the table by requesting user files to reflect the changes.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setButtonActionListeners()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToManageTextArea}, {@link serviceClient.ClientGUI#updateListListener},
     * {@link server.P2PServiceImplSEI#registerFile}, {@link serviceClient.ClientGUI#returnToLoginTab}</p>
     */
    private void registerFileListener (){
        List<String> response = null;
        String[] arrayResponse = null;
        fileToRegister = new File(txtUploadFilePath.getText());
        // check if file exists
        if (!fileToRegister.exists()) {
            clientOutputManager.printToManageTextArea("File does not exist");
            return;
        }
        // check if file is not a folder
        if (fileToRegister.isDirectory()) {
            clientOutputManager.printToManageTextArea("Directory registration is not allowed.");
            return;
        }
        // get file information
        String fileName = fileToRegister.getName().substring(0, fileToRegister.getName().lastIndexOf("."));
        String fileType = fileToRegister.getName().substring(fileToRegister.getName().lastIndexOf(".") + 1);
        String filePath = fileToRegister.getAbsolutePath().substring(0, fileToRegister.getAbsolutePath().lastIndexOf(File.separator))+File.separator;
        // check file property lengths
        if(fileName.length()>100){
            clientOutputManager.printToManageTextArea("File name cannot be longer than 100 characters");
            return;
        } else if (fileType.length()>25){
            clientOutputManager.printToManageTextArea("File type cannot be longer than 25 characters");
            return;
        } else if (filePath.length()>300){
            clientOutputManager.printToManageTextArea("File path cannot be longer than 300 characters");
            return;
        }
        // register file
        try {
            response = P2PServiceImpl.registerFile(
                    token, userName, fileName, fileType ,filePath , fileToRegister.length()).getItem();
            arrayResponse = response.toArray(new String[0]);
        } catch (WebServiceException | NullPointerException ex ){
            clientOutputManager.printToLoginTextArea("Server communication error.");
            returnToLoginTab();
        }
        // parse response
        if (arrayResponse != null) {
            if(arrayResponse[0].equals("OK")) {
                // update table of user files
                updateListListener();
            }else if (arrayResponse[0].equals("CRED")){
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            }else if (arrayResponse[0].equals("FULL")){
                clientOutputManager.printToManageTextArea(arrayResponse[1]);
            }else if (arrayResponse[0].equals("COPY")){
                clientOutputManager.printToManageTextArea(arrayResponse[1]);
            }else if (arrayResponse[0].equals("ERROR")){
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            }
        }
    }

    /**
     * <p> This method is responsible for acquiring a list of user files and updating the manage tqb table.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setButtonActionListeners()},
     * {@link serviceClient.ClientGUI#registerFileListener()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToManageTextArea},{@link server.P2PServiceImplSEI#getUserFiles},
     * {@link serviceClient.ClientGUI#returnToLoginTab}, {@link serviceClient.ClientGUI#updateTable}</p>
     */
    private void updateListListener() {
        List<String> response = null;
        String[] arrayResponse = null;
        // get user files
        try {
            response = P2PServiceImpl.getUserFiles(token, userName).getItem();
            arrayResponse = response.toArray(new String[0]);
        } catch (WebServiceException | NullPointerException e) {
            clientOutputManager.printToLoginTextArea("Server communication error.");
            returnToLoginTab();
        }
        // update table
        if (arrayResponse != null) {
            if (arrayResponse[0].equals("OK")) {
                try {
                    tblUserFilesLock.lock();
                    String[] tableColumns = new String[]{"File ID", "File Name", "File Type", "File Path", "File Size"};
                    String[] tableData = Arrays.copyOfRange(arrayResponse, 1, arrayResponse.length);
                    tableModel = new DefaultTableModel(updateTable(tableData, tableColumns.length), tableColumns);
                    tblUserFiles.setModel(tableModel);
                    return;
                } catch (Exception ex) {
                    clientOutputManager.printToManageTextArea("Error: when updating table : " + "\n" +
                            ex.getMessage());
                }finally {
                    tblUserFilesLock.unlock();
                }
                return;
            } else if (arrayResponse[0].equals("CRED")) {
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            } else if (arrayResponse[0].equals("404")) {
                clientOutputManager.printToManageTextArea(arrayResponse[1]);
            } else if (arrayResponse[0].equals("ERROR")) {
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            }
        }
        // update empty table
        try {
            tblUserFilesLock.lock();
            String[] tableColumns = new String[]{"File ID", "File Name", "File Type", "File Path", "File Size"};
            tableModel = new DefaultTableModel(null, tableColumns);
            tblUserFiles.setModel(tableModel);
        }finally {
            tblUserFilesLock.unlock();
        }
    }

    /**
     * <p> This method is responsible for removing user selected file from the list of user registered files.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#updateListListener()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToManageTextArea}, {@link serviceClient.ClientGUI#updateListListener},
     * {@link server.P2PServiceImplSEI#deregisterFile}, {@link serviceClient.ClientGUI#returnToLoginTab}</p>
     */
    private void unregisterFileListener (){
        List <String> response = null;
        String[] arrayResponse = null;
        String fileName = "";
        String fileType = "";
        String filePath = "";
        // check fileToRemove and get file information
        if(fileToRemove == null) {
            clientOutputManager.printToManageTextArea("No file selected");
            return;
        }
        fileName = fileToRemove.getName();
        fileType = fileToRemove.getType();
        filePath = fileToRemove.getPath();
        // check file property lengths
        if(fileName.length()>100){
            clientOutputManager.printToManageTextArea("File name cannot be longer than 100 characters");
            return;
        } else if (fileType.length()>25){
            clientOutputManager.printToManageTextArea("File type cannot be longer than 25 characters");
            return;
        } else if (filePath.length()>300){
            clientOutputManager.printToManageTextArea("File path cannot be longer than 300 characters");
            return;
        }
        // unregister file
        try {
            response = P2PServiceImpl.deregisterFile(token, userName, fileName, fileType, filePath).getItem();
            arrayResponse = response.toArray(new String[0]);
        } catch (WebServiceException | NullPointerException e) {
            clientOutputManager.printToLoginTextArea("Server communication error.");
            returnToLoginTab();
        }
        // parse response
        if(arrayResponse!=null) {
            if(arrayResponse[0].equals("OK")) {
                clientOutputManager.printToManageTextArea(arrayResponse[1]);
                // update table of user files
                updateListListener();
                fileToRemove = null;
                btnUnregisterFile.setEnabled(false);
                btnUnregisterFile.setText("Unregister File");
            }else if (arrayResponse[0].equals("CRED")) {
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            }else if (arrayResponse[0].equals("ERROR")) {
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            }
        }
    }

    /**
     * <p> This method is responsible for searching for files on the server that match user query.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setButtonActionListeners()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToDownloadTextArea}, {@link serviceClient.ClientGUI#updateTable},
     * {@link server.P2PServiceImplSEI#searchFile}, {@link serviceClient.ClientGUI#returnToLoginTab}</p>
     */
    private void searchFileListener(){
        List <String> response =null;
        String[] arrayResponse = null;
        String fileName = "";
        // send search request to server
        try {
            fileName = txtSearchFile.getText();
            // check if search string is too long
            if(fileName.length()>100){
                clientOutputManager.printToDownloadTextArea("Search string cannot be longer than 100 characters.");
                return;
            }
            // get files from server that meet user criteria
            response = P2PServiceImpl.searchFile(token, userName, fileName).getItem();
            arrayResponse = response.toArray(new String[0]);
        }catch (WebServiceException | NullPointerException e){
            clientOutputManager.printToLoginTextArea("Server communication error.");
            returnToLoginTab();
        }
        // update table
        if(arrayResponse!=null) {
            if(arrayResponse[0].equals("OK")) {
                try {
                    tblDownloadLock.lock();
                    String[] tableColumns = new String[]{"File ID", "File Name", "File Type", "File Size"};
                    String[] tableData = Arrays.copyOfRange(arrayResponse, 1, arrayResponse.length);
                    tableModel2 = new DefaultTableModel(updateTable(tableData, tableColumns.length), tableColumns);
                    tblDownloadFiles.setModel(tableModel2);
                } catch (Exception ex) {
                    clientOutputManager.printToDownloadTextArea("Error: When updating table : " + "\n" +
                            ex.getMessage());
                }finally {
                    tblDownloadLock.unlock();
                }
                return;
            } else if (arrayResponse[0].equals("CRED")) {
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            } else if (arrayResponse[0].equals("404")) {
                clientOutputManager.printToDownloadTextArea(arrayResponse[1]);
            } else if (arrayResponse[0].equals("ERROR")) {
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            }
            // show empty table
            try {
                tblDownloadLock.lock();
                String[] tableColumns = new String[]{"File ID", "File Name", "File Type", "File Size"};
                tableModel2 = new DefaultTableModel(null, tableColumns);
                tblDownloadFiles.setModel(tableModel2);
            }finally {
                tblDownloadLock.unlock();
            }
        }
    }

    /**
     * <p> This method is responsible for initiating the download process for the file that user selected
     * from the search table.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setButtonActionListeners()}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToDownloadTextArea}, {@link serviceClient.ProgressBar},
     * {@link serviceClient.ClientGUI#returnToLoginTab}, {@link server.P2PServiceImplSEI#getFileHostInfo},
     * {@link serviceClient.FileTransferHandler#requestFile}</p>
     */
    private void downloadFileListener (){
        List <String> response;
        String[] arrayResponse = null;
        // create FileTransferHandler
        FileTransferHandler transferFile = new FileTransferHandler(clientOutputManager);
        String[] ownerData = null;
        int fileID;
        //
        try {
            fileID = fileToDownload.getId();
            // get file owner information
            response = P2PServiceImpl.getFileHostInfo(token, userName, fileID).getItem();
            arrayResponse = response.toArray(new String[0]);
        }catch (WebServiceException | NullPointerException e){
            clientOutputManager.printToDownloadTextArea("Server communication error.");
            returnToLoginTab();
        } catch (Exception ex) {
            clientOutputManager.printToDownloadTextArea("Error: When requesting file information : " + "\n" +
                    ex);
            btnDownloadFile.setText("Download File");
            btnDownloadFile.setEnabled(false);
            return;
        }
        // parse response
        if(arrayResponse != null) {
            if (arrayResponse[0].equals("OK")) {
                clientOutputManager.printToDownloadTextArea("Downloading file ...");
                try {
                    System.out.println("arrayResponse: " + arrayResponse.length);
                    System.out.println("arrayResponse: " + arrayResponse[0]);
                    ownerData = Arrays.copyOfRange(arrayResponse, 1, arrayResponse.length);
                    if(ownerData == null || ownerData[0].isEmpty()){
                        clientOutputManager.printToDownloadTextArea("File not found on the server.");
                        btnDownloadFile.setText("Download File");
                        btnDownloadFile.setEnabled(false);
                        return;
                    }
                    // create custom progress bar
                    ProgressBar progressBar = new ProgressBar(progressBarList, downloadProgressPanel);
                    // add progress bar to the panel
                    downloadProgressPanel.add(progressBar);
                    downloadProgressPanel.revalidate();
                    // start file transfer
                    transferFile.requestFile(progressBar, ownerData, fileToDownload.getName(), fileToDownload.getType(), fileToDownload.getSize());
                } catch (Exception ex) {
                    clientOutputManager.printToDownloadTextArea("Error: When downloading requested file : " + "\n" +
                            ex);
                }
            } else if (arrayResponse[0].equals("404")) {
                clientOutputManager.printToDownloadTextArea(arrayResponse[1]);
                searchFileListener();
            }else if (arrayResponse[0].equals("CRED")) {
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            }else if (arrayResponse[0].equals("ERROR")) {
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                returnToLoginTab();
            }
            btnDownloadFile.setText("Download File");
            btnDownloadFile.setEnabled(false);
        }
    }

    /**
     * <p> This method takes care of updating login tab user interface elements by disabling, enabling, changing
     * text or colour. It uses uiLock to prevent multiple threads from changing the GUI at the same time.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#setButtonActionListeners()}, {@link serviceClient.ClientGUI#loginButtonListener()},
     * {@link serviceClient.ClientGUI#resumeSession()}</p>
     *
     * @param loginGUIState true if login GUI elements should be enabled, false if they should be disabled
     * @param statusLabelMessage text to be displayed in the connection status label
     * @param statusLabelColor colour of the connection status label
     */
    private void setLoginUi (boolean loginGUIState, String statusLabelMessage, Color statusLabelColor){
        try {
            uiLock.lock();
            btnLogIn.setEnabled(loginGUIState);
            txtUserPassword.setEnabled(loginGUIState);
            txtUserName.setEnabled(loginGUIState);
            txtUserPort.setEnabled(loginGUIState);
            lblServerStatus.setText(statusLabelMessage);
            lblServerStatus.setForeground(statusLabelColor);
        }finally {
            uiLock.unlock();
        }
    }

    /**
     * <p> This method is responsible for changing Client GUI once the user logs onto the server, allowing access to
     * manage and download tabs. It also starts a separate thread to listen to incoming connections and a thread for
     * heart beat. It uses the uiLock to prevent threads from changing the GUI at the same time.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#loginButtonListener()}, {@link serviceClient.ClientGUI#resumeSession()}</p>
     *
     * <p> Calls: {@link #startConnectionListener}, {@link #heartBeat}</p>
     */
    private void guiLoggedIn(){
        try {
            uiLock.lock();
            // allow access to manage and download tabs
            //tabbedPane.setEnabledAt(0, false);
            lblServerStatus.setText("Connected");
            lblServerStatus.setForeground(Color.GREEN);
            tabbedPane.setEnabledAt(1, true);
            tabbedPane.setEnabledAt(2, true);
            // disable login button
            btnLogIn.setEnabled(false);
            txtUserName.setEnabled(false);
            txtUserPassword.setEnabled(false);
            txtUserPort.setEnabled(false);
        }finally {
            uiLock.unlock();
        }
        // start connection listener
        startConnectionListener();
        // start heart beat
        heartBeat();
    }

    /**
     * <p> This method closes the currently active ConnectionListener, returns the user back to login tab.
     * It disables GUI elements that a non logged in user should not have access to. It
     * also disconnects the user from the Server removing them from the active list. It uses the uiLock to prevent
     * multiple threads from changing the GUI at the same time.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#resumeSession()}, {@link serviceClient.ClientGUI#registerFileListener()},
     * {@link serviceClient.ClientGUI#updateListListener}, {@link serviceClient.ClientGUI#unregisterFileListener},
     * {@link serviceClient.ClientGUI#searchFileListener}, {@link serviceClient.ClientGUI#heartBeat}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToLoginTextArea}, {@link serviceClient.ClientGUI#setSendHeartBeats},
     * {@link serviceClient.ConnectionListener#closeConnectionListener}, {@link server.P2PServiceImplSEI#disconnectFromServer}</p>
     */
    private void returnToLoginTab(){
        try {
            uiLock.lock();
            // close connectionListener
            connectionListener.closeConnectionListener();
            // stop heart beat thread
            setSendHeartBeats(false);
            // reset GUI to login tab
            tabbedPane.setSelectedIndex(0);
            tabbedPane.setEnabledAt(0, true);
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, false);
            lblServerStatus.setText("Disconnected");
            lblServerStatus.setForeground(Color.RED);
            btnLogIn.setEnabled(true);
            txtUserName.setEnabled(true);
            txtUserPassword.setEnabled(true);
            txtUserPort.setEnabled(true);
            // disconnect user from server
            try {
                List <String> response = P2PServiceImpl.disconnectFromServer(token, userName).getItem();
                String[] arrayResponse = response.toArray(new String[0]);
                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
            } catch (WebServiceException e) {
                clientOutputManager.printToLoginTextArea("Error disconnecting from server." + "\n" +
                        "Please wait 2 minuets to time out.");
            }
            clientOutputManager.printToLoginTextArea("ERROR: Connection with Server interrupted." + "\n" +
                    "Please login again." + "\n");
        }finally {
            uiLock.unlock();
        }
    }

    /**
     * <p> This method is responsible for updating tables in the Manage and Download tabs with data provided by the
     * Server. Depending on amount of columns provided, it will either update the table in the Manage tab (4) or the
     * table in the Download tab (3).</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#updateListListener}, {@link serviceClient.ClientGUI#searchFileListener}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToManageTextArea}, {@link serviceClient.ClientOutputManager#printToDownloadTextArea}</p>
     *
     * @param data data to be added to the table.
     * @param dataColumns number of columns in the table.
     * @return 2D array of data to be added to the table.
     */
    private String[][] updateTable(String[] data, int dataColumns){
        String[][] tableData = null;
        try{
            // create tableData with provided column number
            tableData = new String[data.length/dataColumns][dataColumns];
            // if data is empty, print message appropriate for the table
            if(data[0] == null || data[0].isEmpty()){
                if(dataColumns==5) {
                    clientOutputManager.printToManageTextArea("No files registered");
                }else{
                    clientOutputManager.printToDownloadTextArea("No files found");
                    btnDownloadFile.setText("Download File");
                    btnDownloadFile.setEnabled(false);
                }
                // if data is not empty, fill 2D array with data to fit the table
            }else {
                int tableRow = 0;
                for (int dataRow = 0; dataRow < data.length; dataRow = dataRow + dataColumns) {
                    for(int tableCol = 0; tableCol < dataColumns; tableCol++){
                        tableData[tableRow][tableCol] = data[dataRow+tableCol];
                        // System.arraycopy(data, dataRow, tableData[tableRow], 0, dataColumns);
                    }
                    tableRow++;
                }
            }
        }catch (Exception ex) {
            System.out.println("Error: when updating table data : " + ex);
        }
        return tableData;
    }

    /**
     * <p> This method is responsible for sending regular heart beats to the Server. It is started as a separate
     * and sends heart beats every 30 seconds. If a server error occurs it attempts to send heart beat again. If
     * COMM_FAILURE error occurs it attempts to reconnect to CORBA and send heart beat again. If attempts are not
     * successful, it returns user to the Login tab.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#guiLoggedIn}</p>
     *
     * <p> Calls: {@link serviceClient.ClientOutputManager#printToLoginTextArea}, {@link serviceClient.ClientGUI#setSendHeartBeats},
     * {@link serviceClient.ClientGUI#returnToLoginTab}, {@link server.P2PServiceImplSEI#sendHeartBeat},
     * {@link serviceClient.ClientGUI#getSendHeartBeats}</p>
     */
    private void heartBeat() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setSendHeartBeats(true);
                while (getSendHeartBeats()) {
                    List <String> response;
                    String[] arrayResponse = null;
                    // wait for 30 seconds
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        clientOutputManager.printToLoginTextArea("HeartBeat error" + "\n" +
                                "Please restart the client.");
                        returnToLoginTab();
                        break;
                    }
                    // check if sendHeartBeats is still true
                    if(!getSendHeartBeats()){
                        break;
                    }
                    // send heart beat
                    try {
                        // attempt twice
                        response = P2PServiceImpl.sendHeartBeat(token, userName).getItem();
                        arrayResponse = response.toArray(new String[0]);
                        if(!arrayResponse[0].equals("OK")) {
                            response = P2PServiceImpl.sendHeartBeat(token, userName).getItem();
                            arrayResponse = response.toArray(new String[0]);
                            if (!arrayResponse[0].equals("OK")) {
                                clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                                returnToLoginTab();
                                break;
                            }
                        }
                        clientOutputManager.printToLoginTextArea(arrayResponse[1]);
                    } catch (ClientTransportException e){
                        returnToLoginTab();
                        break;
                    } catch (WebServiceException e){
                        returnToLoginTab();
                        break;
                    }
                }
            }
        }).start();
    }

    /**
     * <p> This method is responsible for thread safe access to sendHeartBeats variable. It allows to set a value
     * of sendHeartBeats variable.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#heartBeat}, {@link serviceClient.ClientGUI#returnToLoginTab}</p>
     *
     * @param state boolean value to set sendHeartBeats variable to.
     */
    public synchronized void setSendHeartBeats(boolean state){
        sendHeartBeats = state;
    }

    /**
     * <p> This method is responsible for thread safe access to sendHeartBeats variable. It allows to get a value
     * of sendHeartBeats variable.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#heartBeat}</p>
     *
     * @return boolean value of sendHeartBeats variable.
     */
    public synchronized boolean getSendHeartBeats(){
        return sendHeartBeats;
    }

    /**
     * <p> This method is responsible for starting ConnectionListener that will be listening for P2P connections on
     * the port provided by user. ConnectionListener is started as a separate thread to continuously listen for
     * incoming connections.</p>
     *
     * <p> Called by: {@link #guiLoggedIn}</p>
     *
     * <p> Calls: {@link serviceClient.ConnectionListener#ConnectionListener},
     * {@link serviceClient.ClientOutputManager#printToLoginTextArea}, {@link serviceClient.ClientGUI#returnToLoginTab}</p>
     */
    private void startConnectionListener(){
        try{
            connectionListener = new ConnectionListener(portNumber, clientOutputManager);
            Thread listenerThread = new Thread(connectionListener);
            listenerThread.start();
        } catch (Exception ex) {
            clientOutputManager.printToLoginTextArea("ERROR: Occurred when listening for P2P connections."+"\n" +
                    "Please reconnect or restart the client app.");
            returnToLoginTab();
        }
    }

    /**
     * <p> This method is responsible for verifying if the port provided by user is taken by another process.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI#loginButtonListener}</p>
     *
     * @return TRUE if port is taken, otherwise return FALSE.
     */
    private boolean isServerPortTaken(){
        try{
            ServerSocket testServerSocket = new ServerSocket(portNumber);
            testServerSocket.close();
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    /**
     * <p> This method is responsible for returning text area located in the login tab.</p>
     *
     * <p> Called by: {@link serviceClient.ClientMain#main}</p>
     *
     * @return JTextArea object from login tab.
     */
    public JTextArea getLoginTextArea(){
        return serverLoginText;
    }

    /**
     * <p> This method is responsible for returning text area located in the manage tab.</p>
     *
     * <p> Called by: {@link serviceClient.ClientMain#main}</p>
     *
     * @return JTextArea object from manage tab.
     */
    public JTextArea getManageTextArea(){
        return serverManageText;
    }

    /**
     * <p> This method is responsible for returning text area located in the download tab.</p>
     *
     * <p> Called by: {@link serviceClient.ClientMain#main}</p>
     *
     * @return JTextArea object from download tab.
     */
    public JTextArea getDownloadTextArea(){
        return serverDownloadText;
    }
}
