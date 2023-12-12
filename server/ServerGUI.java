/*
 * 2023/10/13
 * Anton Mishchenko
 */

package server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p> This class is responsible for creating and managing the Server GUI.
 * It creates the JFrame and all GUI elements inside it. It sets the action listeners for
 * the buttons and uses worker threads to keep GUI responsive. It also updates
 * GUI elements to reflect changes in connection status to the SQL service.</p>
 */
public class ServerGUI extends JFrame {
    private JPanel pnlUserCount;
    private JPanel pnlUserCountLeft;
    private JPanel pnlUserCountRight;
    private JLabel lblCurrentUserCount;
    private JLabel lblUserCountNote;
    private JTextField txtMaxUsers;
    private JButton btnSetMaxUsers;
    private JButton btnListUsers;
    private static ActiveUsers activeUsers;
    private static JTextArea txaUserCount;
    private JPanel sqlPanel;
    private JPanel serverOutputPanel;
    private JPanel sqlLabelPanel;
    private JPanel sqlFieldPanel;
    private JLabel sqlUrlLabel;
    private JLabel sqlUsernameLabel;
    private JLabel sqlPasswordLabel;
    private JTextField sqlUrlField;
    private JTextField sqlUsernameField;
    private JTextField sqlPasswordField;
    private JButton sqlConnectButton;
    private JLabel sqlConnectStatusLabel;
    private static JTextArea serverOutputArea;
    private JScrollPane serverOutputAreaScroll;
    private JButton exitServer;
    private OutputManager outputManager;
    private SwingWorker<String,Void> worker;

    /**
     * <p> Constructor for the server GUI.
     * Initializes JFrame properties and calls createUI method to generate GUI.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * <p> Calls: {@link Server.ServerGUI#createUI}</p>
     */
    public ServerGUI() {
        super("P2P Database Server");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setLayout(new BorderLayout());
        createUI();
    }

    /**
     * <p> Creates GUI elements, adjusts their properties and adds them to the JFrame.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.ServerGUI#setSqlUi}</p>
     */
    private void createUI() {
        // user count elements
        pnlUserCount = new JPanel(new FlowLayout());
        pnlUserCount.setBorder(BorderFactory.createTitledBorder("User Telemetry"));
        pnlUserCount.setPreferredSize(new Dimension(500, 150));
        lblCurrentUserCount = new JLabel("Current Users: ");
        txtMaxUsers = new JTextField("3", 5);
        txaUserCount = new JTextArea();
        txaUserCount.setPreferredSize(new Dimension(30, 30));
        txaUserCount.setText("0/0");
        txaUserCount.setFont(new Font("Serif", Font.BOLD, 20));
        txaUserCount.setEditable(false);
        btnSetMaxUsers = new JButton("Set Max Users");
        btnListUsers = new JButton("List Users");
        btnListUsers.setEnabled(false);
        lblUserCountNote = new JLabel("Note: To change maximum amount of users restart the server.");
        // sql elements
        sqlPanel = new JPanel(new FlowLayout());
        sqlPanel.setBorder(BorderFactory.createTitledBorder("SQL Connection"));
        sqlUrlLabel = new JLabel("Database url: ");
        sqlUsernameLabel = new JLabel("Database username: ");
        sqlPasswordLabel = new JLabel("Database password: ");
        sqlUrlField = new JTextField("jdbc:mysql://localhost:3306/Files", 20);
        sqlUsernameField = new JTextField("root", 20);
        sqlPasswordField = new JTextField("A1Hz2@hB10,+", 20);
        sqlConnectButton = new JButton("Connect to database");
        sqlConnectStatusLabel = new JLabel("Not connected");
        sqlConnectStatusLabel.setForeground(Color.RED);
        // server output elements
        serverOutputPanel = new JPanel(new FlowLayout());
        serverOutputPanel.setBorder(BorderFactory.createTitledBorder("Server Messages"));
        serverOutputPanel.setPreferredSize(new Dimension(510, 400));
        serverOutputArea = new JTextArea(5, 20);
        serverOutputArea.setEditable(false);
        serverOutputAreaScroll = new JScrollPane(serverOutputArea);
        serverOutputAreaScroll.setPreferredSize(new Dimension(500, 300));
        // exit button
        exitServer = new JButton("Exit");
        exitServer.setPreferredSize(new Dimension(100, 30));
        // user count elements
        pnlUserCountLeft = new JPanel();
        pnlUserCountLeft.setLayout(new FlowLayout(FlowLayout.LEADING));
        pnlUserCountLeft.setPreferredSize(new Dimension(160, 100));
        pnlUserCountRight = new JPanel();
        pnlUserCountRight.setLayout(new FlowLayout(FlowLayout.LEADING));
        pnlUserCountRight.setPreferredSize(new Dimension(70, 70));
        pnlUserCountLeft.add(btnSetMaxUsers);
        pnlUserCountLeft.add(lblCurrentUserCount);
        pnlUserCountLeft.add(btnListUsers);
        pnlUserCountRight.add(txtMaxUsers);
        pnlUserCountRight.add(txaUserCount);
        pnlUserCount.add(pnlUserCountLeft);
        pnlUserCount.add(pnlUserCountRight);
        pnlUserCount.add(lblUserCountNote);
        // sql elements
        sqlLabelPanel = new JPanel();
        sqlLabelPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        sqlLabelPanel.setPreferredSize(new Dimension(160, 100));
        sqlFieldPanel = new JPanel();
        sqlFieldPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        sqlFieldPanel.setPreferredSize(new Dimension(250, 100));
        sqlLabelPanel.add(sqlUrlLabel);
        sqlLabelPanel.add(sqlUsernameLabel);
        sqlLabelPanel.add(sqlPasswordLabel);
        sqlLabelPanel.add(sqlConnectButton);
        sqlFieldPanel.add(sqlUrlField);
        sqlFieldPanel.add(sqlUsernameField);
        sqlFieldPanel.add(sqlPasswordField);
        sqlFieldPanel.add(sqlConnectStatusLabel);
        sqlPanel.add(sqlLabelPanel);
        sqlPanel.add(sqlFieldPanel);
        // server output and exit elements
        serverOutputPanel.add(serverOutputAreaScroll);
        serverOutputPanel.add(exitServer);

        // additional of panels to the JFrame
        super.add(sqlPanel, BorderLayout.CENTER);
        super.add(pnlUserCount, BorderLayout.NORTH);
        super.add(serverOutputPanel, BorderLayout.SOUTH);
        super.pack();
        super.setVisible(true);
        // set initial sql UI state
        setSqlUi(false, Color.RED, "Not Connected");
    }

    /**
     * <p> Sets action listeners for the buttons. Action listeners create
     * worker threads to complete button actions. Allows GUI to stay responsive.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.ServerGUI#sqlConnect}, {@link Server.SQLConnectionManager#closeSqlConnection},
     * {@link Server.ServerGUI#maxUsersListener}, {@link Server.ServerGUI#listUsersListener}</p>
     *
     * @param sqlConnectionManager manager for sql connection.
     */
    public void setButtonListeners(final SQLConnectionManager sqlConnectionManager){
        // set action listeners for the buttons. Create worker threads to complete button actions
        // this setup allows GUI to remain usable while the potentially slow actions are being completed
        sqlConnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        sqlConnect(sqlConnectionManager);
                        return null;
                    }
                };
                worker.execute();
            }
        });
        exitServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sqlConnectionManager.closeSqlConnection();
                System.exit(0);
            }
        });
        btnSetMaxUsers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        maxUsersListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        btnListUsers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                worker = new SwingWorker<String, Void>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        listUsersListener();
                        return null;
                    }
                };
                worker.execute();
            }
        });
    }

    /**
     * <p> Method responsible for updating GUI elements in response to changes in the
     * connection status of the SQL database facilitated by the SQLConnectionManager.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.OutputManager#printToTextArea}, {@link Server.SQLConnectionManager#testSQLDriver},
     * {@link Server.SQLConnectionManager#connectToDatabase}, {@link Server.SQLConnectionManager#checkSQLConnection},
     * {@link Server.ServerGUI#setSqlUi}, {@link Server.ServerGUI#getSqlConnectStatusLabel}</p>
     *
     * @param sqlConnectionManager manager for sql connection.
     */
    private void sqlConnect(SQLConnectionManager sqlConnectionManager){
        // update ui and get url, username and password
        String sqlConnectStatus = getSqlConnectStatusLabel();
        setSqlUi(false,Color.PINK, "Connecting ...");
        String url = sqlUrlField.getText();
        String username = sqlUsernameField.getText();
        String password = sqlPasswordField.getText();
        // check if url, username and password are not empty and connect to database
        if(url.isEmpty() || username.isEmpty() || password.isEmpty()){
            JOptionPane.showMessageDialog(null, "Please enter a url, username and password.");
            outputManager.printToTextArea("Please enter a url, username and password.");
            setSqlUi(true,Color.RED, sqlConnectStatus);
        }else{
            // test if url, username and password are not extremely long
            if(url.length()>200){
                outputManager.printToTextArea("Url cannot be longer than 200 characters.");
                setSqlUi(true,Color.RED, sqlConnectStatus);
                return;
            }else if(username.length()>100){
                outputManager.printToTextArea("Username cannot be longer than 100 characters.");
                setSqlUi(true,Color.RED, sqlConnectStatus);
                return;
            }else if(password.length()>100){
                outputManager.printToTextArea("Password cannot be longer than 100 characters.");
                setSqlUi(true,Color.RED, sqlConnectStatus);
                return;
            }
            // start by verifying driver, then connect to database and check connection
            if(sqlConnectionManager.testSQLDriver()) {
                if(sqlConnectionManager.connectToDatabase(url, username, password)){
                    setSqlUi(false,Color.GREEN, "Connected");
                    // check if SQL service is still active, thread blocks here
                    if(!sqlConnectionManager.checkSQLConnection(url, username, password)) {
                        setSqlUi(true,Color.RED, "Lost connection");
                    }
                }else {
                    setSqlUi(true,Color.RED, "Could not connect to database");
                }
            } else {
                setSqlUi(true,Color.RED, "Diver error");
            }
        }
    }

    /**
     * <p> Method responsible for setting maximum number of users allowed on the server at the same time. Starts by
     * verifying user input.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.OutputManager#printToTextArea}, {@link Server.ActiveUsers#ActiveUsers},
     * {@link Server.ServerGUI#setSqlUi}, {@link Server.ServerGUI#setUserCountUi}, {@link Server.ServerGUI#activityTest},
     */
    private void maxUsersListener(){
        String maxUsers = txtMaxUsers.getText();
        // check if max users is not empty
        if(maxUsers.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please enter a number representing maximum users" +
                    " allowed on the server at the same time.");
        }else{
            // check if max users is a number
            try {
                int maxUsersInt = Integer.parseInt(maxUsers);
                if(maxUsersInt<1 || maxUsersInt>100){
                    JOptionPane.showMessageDialog(null, "Max users cannot be less than 1 or greater than 100.");
                    return;
                }
                activeUsers = new ActiveUsers(maxUsersInt, txaUserCount);
                outputManager.printToTextArea("Max users set to " + maxUsersInt + ".");
                setSqlUi(true,Color.RED, "Not Connected");
                setUserCountUi(false);
                btnListUsers.setEnabled(true);
                activityTest();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Please enter a valid number.");
            }
        }
    }

    /**
     * <p> Lists all active users in the text area.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.ActiveUsers#listActiveUsers}, {@link Server.OutputManager#printToTextArea},
     */
    private void listUsersListener(){
        ClientData[] users = activeUsers.listActiveUsers();
        if(users==null||users.length==0){
            outputManager.printToTextArea("== No active users ==");
            return;
        }
        outputManager.printToTextArea("== Active users ==");
        for(ClientData user : users){
            outputManager.printToTextArea(user.getName());
        }
        outputManager.printToTextArea("==================");
    }

    /**
     * <p> This method sets OutputManager object responsible for printing messages to the Server GUI.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * @param outputManager manager for GUI output.
     */
    public void setOutputManager(OutputManager outputManager){
        this.outputManager = outputManager;
    }


    /**
     * <p> Method allows to set SQL GUI elements enabled or disabled, set text and
     * set SQL connection status label color. </p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * @param enabled Boolean value to set sql GUI elements enabled or disabled.
     * @param color Color value to set sql label color.
     * @param text String value to set sql label text.
     */
    public synchronized void setSqlUi(Boolean enabled, Color color, String text){
        if(enabled!=null) {
            sqlConnectButton.setEnabled(enabled);
            sqlUrlField.setEnabled(enabled);
            sqlUsernameField.setEnabled(enabled);
            sqlPasswordField.setEnabled(enabled);
        }
        if(color!=null)
            sqlConnectStatusLabel.setForeground(color);
        if(text!=null)
            sqlConnectStatusLabel.setText(text);
    }

    /**
     * <p> Method allows to set user count related GUI elements to enabled or disabled.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * @param enabled Boolean value to set user count GUI elements enabled or disabled.
     */
    public synchronized void setUserCountUi(boolean enabled){
        btnSetMaxUsers.setEnabled(enabled);
        txtMaxUsers.setEnabled(enabled);
    }

    /**
     * <p> Returns String text of the sql connection status label.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * @return String text of the sql connection status label.
     */
    public synchronized String getSqlConnectStatusLabel(){
        return sqlConnectStatusLabel.getText();
    }

    /**
     * <p> Returns ActiveUsers object responsible for management of active users.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * @return ActiveUsers object responsible for management of active users.
     */
    public ActiveUsers getActiveUsers() {
        return activeUsers;
    }

    /**
     * <p> Returns JTextArea from Server GUI for displaying messages.</p>
     *
     * <p> Called by: {@link Server.P2PServiceImpl}</p>
     *
     * @return serverOutputArea JTextArea from Server GUI for displaying messages.
     */
    public static JTextArea getServerOutputArea() {
        return serverOutputArea;
    }

    /**
     * <p> This method is used to test which users are active. It runs in a separate thread, every minute it checks
     * a list of active users and removes users that have been inactive for more than 2 minutes. It outputs results to
     * server GUI.</p>
     *
     * <p> Called by: {@link Server.ServerGUI}</p>
     *
     * <p> Calls: {@link Server.ActiveUsers#listActiveUsers}, {@link Server.ActiveUsers#removeUser},
     * {@link Server.OutputManager#printToTextArea}, {@link Server.ClientData#getLastActive},
     * {@link Server.ClientData#getName}, {@link Server.ClientData#getToken} </p>
     */
    protected void activityTest () {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try{
                        // get current time in seconds
                        long currentTime = System.currentTimeMillis() / 1000L;
                        // get list of active users
                        ClientData[] users = activeUsers.listActiveUsers();
                        // check if there are any active users
                        if (!(users == null)) {
                            // for each active user, check if they are inactive
                            for (ClientData user : users) {
                                long lastActive = user.getLastActive();
                                long difference = currentTime - lastActive;
                                // remove users inactive for more than 2 minutes
                                if (difference > 120) {
                                    String userName = user.getName();
                                    activeUsers.removeUser(user.getToken(), user.getName());
                                    outputManager.printToTextArea("- Removed: " + userName + " inactive for " + difference + " s. -");
                                }
                            }
                        }
                    }catch (Exception e){
                        outputManager.printToTextArea("ERROR: During activity test: " + "\n" + e);
                    }
                    // wait 1 minute
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        outputManager.printToTextArea("ERROR: During activity test sleep: " + "\n" + e);
                    }
                }
            }
        }).start();
    }
}