package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkFindContours - Graphical user interface to exercise openCV Imgproc.findContours method.
 */
public class LinkFindContours extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //
    private static final String[] MODE_STR = {
        "Tree",
        "External",
        "List",
        "Connected"
        //"FloodFill"
    };
    private static final int[] MODE_CONST = {
        Imgproc.RETR_TREE,
        Imgproc.RETR_EXTERNAL,
        Imgproc.RETR_LIST,
        Imgproc.RETR_CCOMP
        //Imgproc.RETR_FLOODFILL
    };
    private static final String[] MODE_CONST_NAME = {
        "Imgproc.RETR_TREE",
        "Imgproc.RETR_EXTERNAL",
        "Imgproc.RETR_LIST",
        "Imgproc.RETR_CCOMP"
        //Imgproc.RETR_FLOODFILL
    };
    
    private static final String[] METHOD_STR = {
        "Chain_Approx_Simple",
        "Chain_Approx_None",
        "Chain_Approx_TC89_KCOS",
        "Chain_Approx_TC89_L1"
    };
    private static final int[] METHOD_CONST = {
        Imgproc.CHAIN_APPROX_SIMPLE,
        Imgproc.CHAIN_APPROX_NONE,
        Imgproc.CHAIN_APPROX_TC89_KCOS,
        Imgproc.CHAIN_APPROX_TC89_L1        
    };
    private static final String[] METHOD_CONST_NAME = {
        "Imgproc.CHAIN_APPROX_SIMPLE",
        "Imgproc.CHAIN_APPROX_NONE",
        "Imgproc.CHAIN_APPROX_TC89_KCOS",
        "Imgproc.CHAIN_APPROX_TC89_L1"
    };

    private JulipComboBox modeCB;
    private JulipComboBox methodCB;
    
    private final int modeIdxDefault = 0;
    private final int methodIdxDefault = 0;
    
    private JLabel labelContours;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
        
    private Point anchor;    
    private List<MatOfPoint> contours;
    //
    //------------------------------------------------
    
    public LinkFindContours(String[] args) {

        // Initialize fields:
        //
        anchor = new Point(-1,-1);
        contours = new ArrayList<>();
        codeFilename = "code_LinkFindContours.java";
        //
        //---------------------------------------------
        //
        // All Link Gui's go through a process of initialization.
        // This step acquires all settings needed for the
        // Link Gui display and algorithms.
        //        
        // Parse command line arguments and build the Link Gui HashMap
        // that defines all the settings for this Gui. A HashMap is simply
        // a set of Key, Value pairs
        //
        parseArgs(this.getClass().getSimpleName(), args);
        buildMyLinkMap();
        //
        // Load input file - could be an image, a Julip file of contours
        //                   or points, or a user-customized file.
        //
        loadImage(myLinkMap.get("IMAGE_IN"));
        //
        // Error-check and correct invalid settings.
        //
        verifySettings();
        //
        //-----------------
        //
        // Create and set up the Java window.
        //
        frame = new JFrame(setFrameName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //
        // All frames have a display image
        //
        Image img = HighGui.toBufferedImage(matImgSrc);
        imgLabel = new JLabel(new ImageIcon(img));  
        imgSP = new JScrollPane(imgLabel);
        imgSP.setPreferredSize(new Dimension(400,400));                
        //
        //----------------------------------------------
        //
        // Containers specifically for this Link Gui instantiated
        //
        
        modeCB      = new JulipComboBox(MODE_STR,   myLinkMap.get("MODE"), this);
        methodCB    = new JulipComboBox(METHOD_STR, myLinkMap.get("METHOD"), this);
        
        //
        // JPanel to hold:
        //      mode JComboBox
        //      method JComboBox
        //      JLabel
        //      link JPanel
        //
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));

        sliderPanel.add(modeCB.comboBox);            
        sliderPanel.add(methodCB.comboBox);        
        labelContours = new JLabel("Number of detected contours:");            
        sliderPanel.add(labelContours);
        
        //
        // All Link Gui's need to add the JPanel returned from
        // LinkClass buildLinkPanel() method.
        //
        sliderPanel.add(buildLinkPanel());
        
        // Build frame; the imgLabel is required to be added somewhere to the frame
        frame.add(sliderPanel, BorderLayout.PAGE_START);
        frame.add(imgSP, BorderLayout.CENTER);
        
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                Dimension sizeSP = imgSP.getSize();
                imgSP.setPreferredSize(new Dimension(sizeSP.width, frame.getSize().height - frameHeightMinusImage));
            }
        });
        
        //
        //----------------------------------------------
        //
        // These final commands are required for all Link Gui's
        //
        frame.pack();
        frameHeightMinusImage = frame.getSize().height - imgSP.getSize().height;             
        frame.setVisible(true);
        refreshSettings();
        refreshImage();
        //
        //----------------------------------------------
    }


    //-----------------------------------------------------------------------------------
    //
    // Cluster of overriden LinkClass methods 
    //   buildMyLinkMap()
    //   verifySettings()
    //   saveSettings(()
    //   saveImage()
    //   mapToSettings()
    //   refreshSettings()
    //   refreshImage()
    //   genImportList()
    //   genCodeString()
            
    /**
     * buildMyLinkMap - native default values for Link Gui
     */
    public void buildMyLinkMap() {
        Map<String, String> defaultMap = new HashMap<String, String>() {{
                put("TYPE",     "FINDCONTOURS");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkcontours.txt");
                put("MODE",      MODE_STR[0]);
                put("METHOD",    METHOD_STR[0]);
            }};
        for (String key : defaultMap.keySet()) {
            if (!myLinkMap.containsKey(key)) {
                myLinkMap.put(key, defaultMap.get(key));
            }
        }
    }

    /**
     * verifySettings - Overrides method in LinkClass; error check myLinkMap settings.
     */    
    @Override
    public void verifySettings() {
        
        //
        // Check for format and range validity for all 2 combo boxes
        //        
        comboCheck("MODE", MODE_STR, modeIdxDefault);
        comboCheck("METHOD", METHOD_STR, methodIdxDefault);
    }
    
    /**
     * saveSettings - write link settings to file 
     */
    @Override
    public void saveSettings() {
        String linkfilename  = textFieldLink.getText();
        
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(linkfilename));
            writer.write("TYPE\tFINDCONTOURS\n");
            writer.write("IMAGE_IN\t"    + textFieldImageIn.getText() + "\n");
            writer.write("IMAGE_OUT\t"   + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"   + linkfilename + "\n");
            writer.write("CONTOUR_FILE\t"+ "nope" + "\n");
            writer.write("MODE\t"        + MODE_STR[modeCB.index] + "\n");
            writer.write("METHOD\t"      + METHOD_STR[methodCB.index] + "\n");
            writer.close();
        } catch (IOException e) {}            
    }
    
    /**
     * saveImage - write output Contours to file 
     */    
    @Override
    public void saveImage() {        
        // Get output filename from 'Save Output As >' JTextField
        //
        String contourfilename = textFieldImageOut.getText();
        ContourHandler.saveContours(contourfilename, matImgDst, contours);        
    }
        
    /**
     * refreshSettings - Overrides method in LinkClass; copy and error check myLinkMap settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        return true;
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        modeCB.setValue(myLinkMap.get("MODE"));
        methodCB.setValue(myLinkMap.get("METHOD"));
    }
    
    /**
     * refreshImage - refresh label texts and shown image
     */
    public void refreshImage() {
        Mat gray = new Mat();        
        // If source is a color Mat, 3-channel, then convert it to grayscale, 1-channel
        if (matImgSrc.channels() == 3) {        
            Imgproc.cvtColor(matImgSrc, gray, Imgproc.COLOR_BGR2GRAY);
        }

        contours.clear();
        Mat hierarchy = new Mat();
        Imgproc.findContours(
            gray,                        // input Mat image
            contours,                    // output List of Mats of contours
            hierarchy,                   // output Mat of contour hierarchy
            MODE_CONST[modeCB.index],    // contour retrieval mode
            METHOD_CONST[methodCB.index] // contour approximation method
        );
        labelContours.setText("Number of detected contours: " + contours.size());         

        matImgDst = Mat.zeros(matImgSrc.size(), CvType.CV_8UC3);
        Scalar color = new Scalar(0,255,255);        
        for (int i = 0; i < contours.size(); i++) {
            //Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
            Imgproc.drawContours(
                matImgDst,          // input/output mat image
                contours,           // input List of Mats of contours
                i,                  // index into List of Mats of contours
                color,              // Scalar color of drawn contour
                2,                  // pixel thickness of drawn contour
                Imgproc.LINE_8,     // LineType of drawn contour
                hierarchy,          // input Mat of contour hierarchy
                0,                  // hierarchy threshold of drawn contours
                new Point()         // contour x,y offset
            );        
        }
        
        Image img = HighGui.toBufferedImage(matImgDst);
        imgLabel.setIcon(new ImageIcon(img));        
        frame.repaint();
    }
    
    /**
     * genImportList - generate a list of import statements required to run the prototype code
     */
    @Override
    public List<String> genImportList() {
        List<String> importsL = new ArrayList<String>();
        importsL.add("java.util.ArrayList");
        importsL.add("java.util.List");
        importsL.add("org.opencv.core.Mat");
        importsL.add("org.opencv.imgproc.Imgproc");
        return importsL;
    }     
    
    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {
            returnStr = "List<MatOfPoint>";
            objectStr = "findContoursList";
            StringBuilder msb = new StringBuilder();
            msb.append("doLinkFindContours");
            if (!reference.equals("")) { msb.append("_"+reference); }
            methodStr = msb.toString();
                
            StringBuilder sb = new StringBuilder();
            sb.append("    public "+returnStr+" "+methodStr.toString()); 
            sb.append("(Mat matImgSrc) {\n");
            sb.append("        Mat gray = new Mat();\n");
            sb.append("        Mat hierarchy = new Mat();\n");
            sb.append("        List<MatOfPoint> contours = new ArrayList<>();\n");
            sb.append("        // Convert source Mat in BGR color space to Gray color space\n");
            sb.append("        Imgproc.cvtColor(\n");
            sb.append("            matImgSrc,              // Mat - source\n");
            sb.append("            gray,                   // Mat - destination\n");
            sb.append("            Imgproc.COLOR_BGR2GRAY  // int - code space conversion code\n");
            sb.append("        );\n");
            sb.append("        Imgproc.findContours(\n");
            sb.append("            gray,         // Mat - input image\n");
            sb.append("            contours,     // List of MatOfPoints - output List of contours\n");
            sb.append("            hierarchy,    // Mat - output hierarchy Mat\n");
            sb.append("            "+MODE_CONST_NAME[modeCB.index]+",    // int - contour retrieval mode\n");
            sb.append("            "+METHOD_CONST_NAME[methodCB.index]+"    // int - contour approximation method\n");
            sb.append("        );\n");
            sb.append("        return contours;\n");
            sb.append("    }\n");
        return sb.toString();
    }
    
    //
    //  end of overridden LinkClass methods
    //-----------------------------------------------------------------------------------
    
    /**
     * main - method to allow command line application launch.
     *        (credit to opencv tutorial code provided by the OpenCV website: docs.opencv.org)
     */
    public static void main(String[] args) {
        // Load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LinkFindContours(args);
            }
        });
    }
}
