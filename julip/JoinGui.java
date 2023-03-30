package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;


/**
 * JoinGui - Class to handle a merged chain of Link Gui's that feeds one's output to the next one's input.
 */
public class JoinGui extends ChainGui {

    String[] inputKeyStr;
    JComboBox<String> imageKeyCB;
    JLabel firstGuiWarningLabel;
    
    /**
     *  JoinGui constructor 
     *  @param args - List of command line arguments
     */
    public JoinGui(String[] args) {
        // Execute the constructor for the superclass ChainGui
        super(args);
        isJoinGui = true;
    }
    
    
    //--------------------------------------------------------
    //
    //  Methods overridden by JoinGui over ChainGui...
    //    buildImageScrollPane()
    //    buildImageControlPanel()
    //    buildGuiKeys()
    //    getFrameName()
    //
    
    /**
     * buildImageScrollPane - create an JScrollPane to hold image content for the gui.
     *                        the ImageScrollPane for JoinGui overrides the ImageScrollPane for ChainGui.
     * @return - constructed JScrollPane
     */
    @Override
    public JScrollPane buildImageScrollPane() {
        
        //------------------------------------- image JTable and JScrollPane -------------------------
        //        
        // JScrollpane constructed with JTable
        
        // Create TableModel with non-editable cells
        imageTM = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        imgTable = new JTable(imageTM);
        imageTM.addColumn("Image");
        imageTM.addColumn("Key");
        if (sourceImages.size() != sourceKeys.size()) {
            System.out.println("Whoa, sourceImages List is NOT the same size as sourceKeys List. Fail.");
            return null;
        }
        for (int i = 0; i < sourceImages.size(); i++) {
            imageTM.addRow(new Object[]{sourceImages.get(i), sourceKeys.get(i)});                                              
        }
        JScrollPane sourceSP = new JScrollPane(imgTable);
        sourceSP.setPreferredSize(new Dimension(NOWIDTH,100));
        
        for (int i = 0; i < sourcesSelected.size(); i++) {
            if (sourceImages.contains(sourcesSelected.get(i))) {
                int idx = sourceImages.indexOf(sourcesSelected.get(i));
                imgTable.addRowSelectionInterval(idx, idx);
            }
            else {
                System.out.println(sourcesSelected.get(i)+" is not in list of IMAGES");
            }
        }        
/*        
        for (int i = 0; i < inputKeyStr.length; i++) {
            String keyStr = "SELECTED_"+inputKeyStr[i];
            if (myChainMap.containsKey(keyStr)) {
        
                suppressListeners = true;
                int idx = sourceImages.indexOf(keyStr);
                if (idx != -1) {
                    imgTable.setRowSelectionInterval(idx, idx);
                } else {
                    System.out.println(keyStr+":"+myChainMap.get(keyStr)+" is not in list of IMAGES");
                }
                suppressListeners = false;
            }
        }
*/            
        return sourceSP;
        
    }

    /**
     * buildImageControlPanel - create the control panel for add/removing images.
     * @return - constructed JPanel
     */
    @Override
    public JPanel buildImageControlPanel() {
                
        //------------------------------------- imageControlPanel -------------------------
        //        
        // JPanel of type GridLayout (3-row,2-col) to hold:
        //   JButton - to add Images
        //   JTextField - name of image to add to JTable
        //   JComboBox - hold reference Keys
        //   JButton - to remove image selected in JTable
        //
        JPanel imageControlPanel = new JPanel(new GridLayout(3, 2));
        
        //
        // Grid element 1,1
        // JTextfield to hold image name
        //
        JTextField imageAddTF = new JTextField();
        imageAddTF.setPreferredSize(new Dimension(120, 25));
        imageControlPanel.add(imageAddTF);
        
        //
        // Grid element 1,2
        // JComboBox to hold reference keys
        //        
        if (inputKeyStr == null) {
            imageKeyCB = new JComboBox<String>();
        } else {
            imageKeyCB = new JComboBox<String>(inputKeyStr);
        }
        imageControlPanel.add(imageKeyCB);
        
        //
        // Grid element 2,1
        // JButton to add entries to Image JTable
        //
        JButton imageAddB = new JButton("Add Image ^");
        imageAddB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // check if image entry in imageAddTF already exists in list of images
                // and if there is a non-null entry in the imageAddTF Textfield.                
                String imgStr = imageAddTF.getText().trim();
                if (imgStr.equals("")) {
                    System.out.println("No entry in Add Image Text Field.");
                    imageAddB.setBackground(Color.RED);                    
                } 
                else if (sourceImages.contains(imgStr)) {
                    System.out.println("Entry "+imgStr+" already exists in Image JTable.");
                    imageAddB.setBackground(Color.RED);                    
                } 
                else if (imageKeyCB.getSelectedIndex() < 0) {
                    System.out.println("No entry selected in ComboBox.");
                    imageAddB.setBackground(Color.RED);                                                                
                }                    
                else {
                    imageTM.addRow(new Object[]{imgStr, imageKeyCB.getSelectedItem().toString()});
                    sourceImages.add(imgStr);
                    sourceKeys.add(imageKeyCB.getSelectedItem().toString());
                    imageAddB.setBackground(null);                    
                }
            }
        });
        imageControlPanel.add(imageAddB);
        
        //
        // JButton to remove entries from Image JTable
        // Grid element 2,2
        //
        JButton imageRemoveB = new JButton("Kill Image");
        imageRemoveB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Delete the selected image from the list of images and the jtable
                int[] rows = imgTable.getSelectedRows();
                if (rows.length > 0) {
                    for (int i = rows.length-1; i >= 0; i--) {
                        imageTM.removeRow(rows[i]);
                        sourceImages.remove(rows[i]);
                        sourceKeys.remove(rows[i]);
                        imageRemoveB.setBackground(null);
                    }
                }
                else {
                    System.out.println("No row selected for removal in Image JTable.");
                    imageRemoveB.setBackground(Color.RED);
                }
            }
        });
        imageControlPanel.add(imageRemoveB);
        
        //
        // JLabel to warn of needed (join)LinkGui
        // Grid element 3,1
        //
        firstGuiWarningLabel = new JLabel("Add (Join)LinkGui");
        firstGuiWarningLabel.setForeground(Color.RED);        
        imageControlPanel.add(firstGuiWarningLabel);
        
        //
        // JLabel
        // Grid element 3,2
        imageControlPanel.add(new JLabel());
        
        return imageControlPanel;
    }
    
    /**
     * verifyGuiScrollPane - not needed for ChainGui; overridden by JoinGui.
     */
    @Override
    public void verifyGuiScrollPane() {
        if (guiLinks != null) {
            //
            // Probably should check to make sure first gui in chain is a joinGui!!!
            //
            firstGuiWarningLabel.setText("");
        }
    }
    
    /**
     * buildGuiKeys - not needed for ChainGui; overridden by JoinGui.
     */
    @Override
    public void buildGuiKeys(String guiName) {
        if (guiName.equals("JoinRelicJewel")) {
            inputKeyStr = JoinRelicJewel.inputKeyStr;
        }
        if (inputKeyStr != null) {
            firstGuiWarningLabel.setText("");
            imageKeyCB.removeAllItems();
            for (String key : inputKeyStr) {
                imageKeyCB.addItem(key);
            }
        }
    }

    /**
     * buildLinkInputs - construct command line arguments to instantiate a LinkGui and
     *                   provide the input file to the LinkGui
     * @return - list of String of arguments
     */
    @Override
    public List<String> buildLinkInputs() {
        
        List<String> argList = new ArrayList<>();
    
        //
        // Get image inputs
        //
        int[] idx = imgTable.getSelectedRows();
        for (int i = 0; i < idx.length; i++) {
            argList.add("-k");                                   // assert key command
            argList.add(imgTable.getValueAt(idx[i], 1).toString()); // append key name
            argList.add(imgTable.getValueAt(idx[i], 0).toString()); // append key file
        }
        return argList;
    }
    
    /**
     * getFrameName - returns name of frame (overridable)
     * @return String  - name of frame
     */
     @Override
     public String getFrameName() {
        return ("(Join)ChainGui");
     }

     
    /**
     * saveSettings - write chain gui settings to file
     */
    @Override
    public void saveSettings() {
        String chainfilename = chainLinkTF.getText();
        
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(chainfilename));
            writer.write("<CHAIN_REFERENCE>\t" + refChainTF.getText() +"\t</CHAIN_REFERENCE>\n");
            for (int i = 0; i < imgTable.getRowCount(); i++) {
                writer.write("<JOIN_INPUT>\n");
                writer.write("\t<JOIN_FILE>\t"+imgTable.getValueAt(i, 0).toString()+"\t</JOIN_FILE>\n");
                writer.write("\t<JOIN_KEY>\t"+imgTable.getValueAt(i, 1).toString()+"\t</JOIN_KEY>\n");
                writer.write("</JOIN_INPUT>\n");
            }
            int[] srcSelectionIndex = imgTable.getSelectedRows();
            String sourceFilename = "";
            for (int i = 0; i < srcSelectionIndex.length; i++) {
                sourceFilename = imgTable.getValueAt(srcSelectionIndex[i], 0).toString();
                writer.write("<JOIN_SELECTED>\t" + sourceFilename +"\t</JOIN_SELECTED>\n");
            }             
            for (int i = 0; i < guiTable.getRowCount(); i++) {
                writer.write("<LINK_GUI>\n");
                writer.write("\t<NAME>\t"+guiTable.getValueAt(i, 0).toString()+"\t</NAME>\n");
                writer.write("\t<LINK>\t"+guiTable.getValueAt(i, 1).toString()+"\t</LINK>\n");
                writer.write("\t<OUT>\t"+guiTable.getValueAt(i, 2).toString()+"\t</OUT>\n");
                writer.write("</LINK_GUI>\n");
            }
            writer.write("<CHAIN_FILE>\t" + chainLinkTF.getText() +"\t</CHAIN_FILE>\n");
            writer.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }    
    //
    //  End of overridden methods
    //--------------------------------------------------------
    
    
    /**
     * main - method to allow command line application launch
     */
    public static void main(String[] args) {
        // Load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new JoinGui(args);
            }
        });
    }
}

