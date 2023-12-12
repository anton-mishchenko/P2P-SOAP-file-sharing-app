/*
 * 2023/10/13
 * Anton Mishchenko
 */

package server;

/**
 * <p> ClientData class used to store user data of an active user that is using PeerToPeer service.</p>
 */
public class ClientData {
    private String name;
    private String password;
    private String token;
    private long port;
    private boolean active;
    private long lastActive;

    /**
     * <p> Constructor for ClientData class. Sets user name and token in addition to
     * calling setLastActive() method to set set activity status.</p>
     *
     * <p> Called by: {@link Server.ActiveUsers}</p>
     *
     * <p> Calls: {@link Server.ClientData#setLastActive}</p>
     *
     * @param name String of user's name.
     * @param token String of user's token.
     */
    public ClientData(String name, String token) {
        this.name = name;
        this.token = token;
        setLastActive(true);
    }

    /**
     * <p> Sets user name.</p>
     *
     * @param name String of user's name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p> Sets user password.</p>
     *
     * @param password String of user's password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * <p> Sets user token.</p>
     *
     * @param token String of user's token.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * <p> Sets user port.</p>
     *
     * @param port String of user's port.
     */
    public void setPort(long port) {
        this.port = port;
    }

    /**
     * <p> Sets activity status of user. Updates time of last user activity.</p>
     *
     * <p> Called by: {@link Server.ClientData}, {@link Server.P2PServiceImpl}</p>
     *
     * @param active boolean true if user is active, false if inactive.
     */
    public void setLastActive(boolean active) {
        this.active = active;
        if (active) {
            this.lastActive = System.currentTimeMillis() / 1000L;
        }
    }

    /**
     * <p> Returns user name.</p>
     *
     * @return String of user's name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p> Returns user password.</p>
     *
     * @return String of user's password.
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * <p> Returns user token.</p>
     *
     * <P> Called by: {@link Server.P2PServiceImpl}, {@link Server.ServerGUI}, {@link Server.ActiveUsers}</p>
     *
     * @return String of user's token.
     */
    public String getToken() {
        return this.token;
    }

    /**
     * <p> Returns user port.</p>
     *
     * @return String of user's port.
     */
    public long getPort() {
        return this.port;
    }

    /**
     * <p> Returns activity status of user.</p>
     *
     * @return boolean true if user is active, false if inactive.
     */
    public boolean getActive() {
        return this.active;
    }

    /**
     * <p> Returns last active time of user.</p>
     *
     * <P> Called by: {@link Server.ServerGUI}</p>
     *
     * @return long of user's last active time.
     */
    public long getLastActive() {
        return this.lastActive;
    }
}
