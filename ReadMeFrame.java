package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * ReadMeFrame - Graphical Frame to show important user-guide JULIP documentation.
 */
public class ReadMeFrame {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //                   
        
    public ReadMeFrame() {
    
        //
        //-----------------
        //
        // Create and set up the Java window.
        //
        JFrame frame = new JFrame("READ ME");

        JPanel readmePanel = new JPanel();

        JTextArea readmeTA = new JTextArea();
        JButton closeB = new JButton("Close");
        closeB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
 

        readmeTA.append("Read Me for JULIP ChainGui\n\n");
        readmeTA.append("----------------------------------------\n");
        readmeTA.append("At the command line (terminal in Mac)...\n\n");
        readmeTA.append(" To see all the available command line arguments use\n");
        readmeTA.append("    java ChainGui --help\n\n");
        readmeTA.append(" To load a previously saved ChainGui, like 'chain_blue.txt' use\n");
        readmeTA.append("    java ChainGui chain_blue.txt\n\n");
        readmeTA.append("----------------------------------------\n");
        readmeTA.append("Don't use the 'default' chain reference,\n");
        readmeTA.append(" update the chain reference with your own choice\n\n");
        readmeTA.append("Do use 'Save Settings' often!\n");
        readmeTA.setBorder(BorderFactory.createCompoundBorder(
            readmeTA.getBorder(), 
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        readmePanel.setLayout(new BoxLayout(readmePanel, BoxLayout.PAGE_AXIS));
        readmePanel.add(readmeTA);
        readmePanel.add(closeB);
        
        frame.add(readmePanel, BorderLayout.PAGE_START);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);       
        frame.setLocation(50,50);         
        frame.setVisible(true);
    }

            
    /**
     * main - method to allow command line application launch.
     *        (credit to opencv tutorial code provided by the OpenCV website: docs.opencv.org)
     */
    public static void main(String[] args) {
        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ReadMeFrame();
            }
        });
    }
}
