/*
 * 2023/10/13
 * Anton Mishchenko
 */

package serviceClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * <p> Custom progress bar with a clear button that allows user to remove the progress bar form the GUI.
 * Progress bars are created by client and manipulated by FileTransferHandler classes.</p>
 */
public class ProgressBar extends JPanel {
    JProgressBar progressBar;

    /**
     * <p> Constructor for the custom progress bar together with ActionListener used by the clearing button.</p>
     * <p> When the <i>deleteButton</i> is pressed, the progress bar is removed from the GUI and the list of active
     * progress bars.</p>
     *
     * <p> Called by: {@link serviceClient.ClientGUI}</p>
     *
     * @param progressBarList list of created progress bars
     * @param downloadProgressPanel panel that contains the progress bars
     */
    public ProgressBar(final List<ProgressBar> progressBarList, final JPanel downloadProgressPanel) {
        // layout for this UI element
        setLayout(new BorderLayout());
        // progress bar portion of the UI element
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.CENTER);
        // button portion of the UI element
        JButton deleteButton = new JButton("Clear Icon");
        // action listener for the removal button
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // remove this object from the list
                progressBarList.remove(ProgressBar.this);
                downloadProgressPanel.remove(ProgressBar.this);
                downloadProgressPanel.revalidate();
                downloadProgressPanel.repaint();
            }
        });
        add(deleteButton, BorderLayout.EAST);
    }

    /**
     * <p> Getter for the progress bar.</p>
     *
     * <p> Called by: {@link serviceClient.FileTransferHandler}</p>
     *
     * @return JProgressBar progress bar object.
     */
    public JProgressBar getProgressBar() {
        return progressBar;
    }
}