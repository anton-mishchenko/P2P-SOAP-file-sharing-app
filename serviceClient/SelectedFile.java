/*
 * 2023/10/13
 * Anton Mishchenko
 */

package serviceClient;

/**
 * <p> SelectedFile class is used by P2PClient to store information about a file that has
 * been selected.</p>
 */
public class SelectedFile {
    private int id;
    private String name;
    private String type;
    private String path;
    private long size;

    /**
     * <p> Constructor for SelectedFile class. Initializes the file's: name, type, path and size of the file.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * @param name String name of the file.
     * @param type String type of the file.
     * @param path String path of the file.
     * @param size long size of the file.
     */
    SelectedFile(int id, String name, String type, String path, long size) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.size = size;
    }

    /**
     * <p> Getter for the id of the file.</p>
     *
     * @return int id of the file.
     */
    public int getId() {
        return this.id;
    }

    /**
     * <p> Getter for the name of the file.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * @return String name of the file.
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p> Getter for the type of the file.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * @return String type of the file.
     */
    public String getType() {
        return this.type;
    }

    /**
     * <p> Getter for the path of the file.</p>
     *
     * @return String path of the file.
     */
    public String getPath() {
        return this.path;
    }
    /**
     * <p> Getter for the size of the file.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * @return long size of the file.
     */
    public long getSize() {
        return this.size;
    }

    /**
     * <p> Setter for the id of the file.</p>
     *
     * @param id int id of the file.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * <p> Setter for the name of the file.</p>
     *
     * @param name String name of the file.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p> Setter for the type of the file.</p>
     *
     * @param type String type of the file.
     */
    public void setType(String type) {
        this.type = type;
    }
}
