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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * JoinRelicJewel - Graphical user interface to show solutions to image processing for Relic Recovery jewel identification.
 */
public class JoinRelicJewel extends LinkClass {

    public static String[] inputKeyStr = new String[]{"RED", "BLUE"};
    
    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //            
        
    private final String[] IMAGE_STR = {
        "Output",
        "Input:RED",
        "Input:BLUE"
    };
        
    private JTextField redContourTF;
    private JTextField blueContourTF;
    private JLabel labelImageIs;
    private JulipComboBox imageCB;
    
    private List<MatOfPoint> redContours;
    private List<MatOfPoint> blueContours;
    
    private Point redCenter;
    private Point blueCenter;
    
    private boolean isResolved;
    private int position;
    
    //
    //------------------------------------------------
        
    public JoinRelicJewel(String[] args) {
        
        // Initialize fields:
        inputJoinStr = "input keys: 'RED' for contour of red jewel, 'BLUE' for contour of blue jewel.";
        codeFilename = "code_JoinRelicJewel.java";
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
        isResolved = buildContours(myLinkMap.get("RED"), myLinkMap.get("BLUE"));
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
        //
        //----------------------------------------------
        //
        // Containers specifically for this Link Gui instantiated
        //
         
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));

        //------------ redContourPanel -----------------
        //   JTextField to hold image name
        //
        JPanel redContourPanel = new JPanel();
        redContourPanel.add(new JLabel("RED Contour Filename:"));
        redContourTF = new JTextField();
        redContourTF.setPreferredSize(new Dimension(300, 25));
        redContourTF.setText(myLinkMap.get("RED"));
        redContourPanel.add(redContourTF);
        sliderPanel.add(redContourPanel);
        
        //------------ blueContourPanel -----------------
        //   JTextField to hold image name
        //
        JPanel blueContourPanel = new JPanel();
        blueContourPanel.add(new JLabel("BLUE Contour Filename:"));
        blueContourTF = new JTextField();
        blueContourTF.setPreferredSize(new Dimension(300, 25));
        blueContourTF.setText(myLinkMap.get("BLUE"));
        blueContourPanel.add(blueContourTF);
        sliderPanel.add(blueContourPanel);
                
        imageCB = new JulipComboBox(IMAGE_STR, myLinkMap.get("IMAGE_TYPE"), this);
        sliderPanel.add(imageCB.comboBox);
                
        sliderPanel.add(new JLabel(" "));
        labelImageIs    = new JLabel("Red Jewel is < > Blue Jewel is < >");
        sliderPanel.add(labelImageIs);
        sliderPanel.add(new JLabel(" "));
        
        // Add a LinkPanel for a JoinClass
        sliderPanel.add(buildLinkPanel(true));
        
        // Build frame
        frame.add(sliderPanel, BorderLayout.PAGE_START);
        frame.add(imgLabel, BorderLayout.CENTER);        
        
        //
        //----------------------------------------------
        //
        // These final commands are required for all Link Gui's
        //
        frame.pack();
        frame.setVisible(true);
        refreshSettings();
        refreshImage();
        //
        //----------------------------------------------
    }

    /**
     * setImageInTF - Assign input text fields according to key names
     * @param args - List of arguments to parse for key names, files to assign to text fields
     */
    @Override
    public void setImageInTF(List<String> args) {
        boolean setKey      = false;
        boolean setKeyInput = false;
        String  thisKey = null;        
        
        for (String arg : args) {
            //
            // Look for -k command.
            // The next arg after -k is the key
            // The second arg after -k is the input file for the key
            //
            if (setKeyInput) {
                if (thisKey.equals("RED")) {
                    redContourTF.setText(arg);
                }
                else if (thisKey.equals("BLUE")) {
                    blueContourTF.setText(arg);
                }
                else {
                    System.out.println("Whoa, trying to assign a text field from a bad key:"+thisKey+". Fail");
                }
                setKeyInput = false;
                thisKey = null;
            }
            if (setKey) {
                thisKey = arg;
                setKeyInput = true;
                setKey = false;
            }
            if (arg.equals("-k")) {
                setKey = true;
            }                                
        }
    
    }
    
    /**
     * loadImage - Import Contour file and update Sliders
     * @return boolean - True if successful parsing of Contour input file, else false
     */
    @Override
    public boolean loadImage(String filename) {
        isResolved = buildContours(redContourTF.getText(), blueContourTF.getText());
        return isResolved;
    }
    
    /**
     * buildContours - read contour files and set up image and contour data
     * @param redfilename  - julip-formatted contour file name of red contour(s)
     * @param bluefilename - julip-formatted contour file name of blue contour(s)
     * @return boolean - true if file is properly read, else false
     */
    public boolean buildContours(String redfilename, String bluefilename) {
    
        // Mat.zeros takes (row,col,type)
        matImgSrc = Mat.zeros(NOHEIGHT, NOWIDTH, CvType.CV_8UC3);
    
        // loadContours returns null if the file couldn't be parsed
        // else it returns a List<MatOfPoints>
        redContours  = ContourHandler.loadContours(redfilename);
        blueContours = ContourHandler.loadContours(bluefilename);
        
        if ((redContours == null) || (blueContours == null)) {
            System.out.println("Whoa, loading contour files failed.");
            return false;
        } 
        
        // Deal with the special case of the first element of the list:
        //
        // The first contour is faked-up as it holds the Size of the image Mat
        double[] redSize  = redContours.get(0).get(0,0);
        double[] blueSize = blueContours.get(0).get(0,0);
        // delete faked-up contour that holds the image Mat Size 
        redContours.remove(0);
        blueContours.remove(0);
        if ((int)redSize[1] != (int)blueSize[1]) {
            System.out.println("Whoa, red rows:"+(int)redSize[1]+" not equal to blue rows:"+(int)blueSize[1]+". Fail.");
            return false;
        } else if ((int)redSize[0] != (int)blueSize[0]){
            System.out.println("Whoa, red cols:"+(int)redSize[0]+" not equal to blue rows:"+(int)blueSize[0]+". Fail.");
            return false;        
        }
    
        if (redContours.size() != 1) {
            System.out.println("Whoa, "+redContours.size()+" red contours. Should be 1. Fail.");
            return false;
        }
        if (blueContours.size() != 1) {
            System.out.println("Whoa, "+blueContours.size()+" blue contours. Should be 1. Fail.");
            return false;
        }
        
    
        // Size is <col>x<row> and Mat methods are (row,col) so swap order of args
        matImgSrc = Mat.zeros((int)redSize[1], (int)redSize[0], CvType.CV_8UC3);
                                                
        redCenter = new Point();
        MatOfPoint   redContour = redContours.get(0);
        MatOfPoint2f redContour2f = new MatOfPoint2f(redContour.toArray());
        float[] redRadius = new float[1];
        Imgproc.minEnclosingCircle(
            redContour2f, // MatOfPoint2f - input Points
            redCenter,    // Point        - center of circle
            redRadius     // float[]      - radius of circle
        );
        
        blueCenter = new Point();
        MatOfPoint   blueContour = blueContours.get(0);
        MatOfPoint2f blueContour2f = new MatOfPoint2f(blueContour.toArray());
        float[] blueRadius = new float[1];
        Imgproc.minEnclosingCircle(
            blueContour2f, // MatOfPoint2f - input Points
            blueCenter,    // Point        - center of circle
            blueRadius     // float[]      - radius of circle
        );        
        return true;
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
                put("TYPE", "FINDCONTOURS");
                put("RED", "noneRed.ctr");
                put("BLUE", "noneBlue.ctr");
                put("LINK_FILE", "nolink.txt");
                put("IMAGE_TYPE", IMAGE_STR[0]);                
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
        // Check for format and range validity for all 1 combo boxes
        //        
        comboCheck("IMAGE_TYPE", IMAGE_STR, 0);    
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
            writer.write("TYPE\tRELICJEWEL\n");
            writer.write("RED\t"    + redContourTF.getText() + "\n");
            writer.write("BLUE\t"   + blueContourTF.getText() + "\n");
            writer.write("IMAGE_OUT\t"    + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"    + linkfilename + "\n");
            writer.write("IMAGE_TYPE\t" + IMAGE_STR[imageCB.index] + "\n");            
            writer.close();
        } catch (IOException e) {}
    }
    
    /**
     * saveImage - write output Image to file 
     */
    @Override
    public void saveImage() {
        String outputfilename = textFieldImageOut.getText();
        
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(position);
            writer.write(labelImageIs.getText()+"\n");
            writer.close();
        } catch (IOException e) {}
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        redContourTF.setText(myLinkMap.get("RED"));
        blueContourTF.setText(myLinkMap.get("BLUE"));
        imageCB.setValue(myLinkMap.get("IMAGE_STR"));    
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
     * refreshImage - refresh label texts and shown image
     */
    public void refreshImage() {
    
        matImgDst = new Mat();
        matImgSrc.copyTo(matImgDst);
        Scalar blueColor  = new Scalar(255,0,0);    
        Scalar redColor   = new Scalar(0,0,255);

        // show red contour
        if (imageCB.index == 1) {        
            Imgproc.drawContours(
                matImgDst,    // Mat image  - destination
                redContours,  // List<MatOfPoint> contours
                -1,            // int contourIdx - which contour to draw
                redColor,     // Scalar color   - draw color
                1             // int thickness  - <0 infills contour
            );
        }
        // show blue contour
        else if (imageCB.index == 2) {
            Imgproc.drawContours(
                matImgDst,    // Mat image  - destination
                blueContours, // List<MatOfPoint> contours
                -1,            // int contourIdx - which contour to draw
                blueColor,    // Scalar color   - draw color
                1            // int thickness  - <0 infills contour
            );
        }                
        // If this gui is showing the output image then project the
        // infilled blue and red contous onto the image
        else {
            Imgproc.drawContours(
                matImgDst,    // Mat image  - destination
                blueContours, // List<MatOfPoint> contours
                0,            // int contourIdx - which contour to draw
                blueColor,    // Scalar color   - draw color
                -1            // int thickness  - <0 infills contour
            );
            
            Imgproc.drawContours(
                matImgDst,    // Mat image  - destination
                redContours,  // List<MatOfPoint> contours
                0,            // int contourIdx - which contour to draw
                redColor,     // Scalar color   - draw color
                -1            // int thickness  - <0 infills contour
            );
        }
        
        String msg = "Red and Blue Jewels are unresolved!!";
        position = -1;
        if (isResolved) {
            if (redCenter.x < blueCenter.x) {
                msg = "Red Jewel is LEFT Blue Jewel is RIGHT";
                position = 1;
            }
            else if (redCenter.x > blueCenter.x) {
                msg = "Red Jewel is RIGHT Blue Jewel is LEFT";
                position = 2;
            }
            else {
                msg = "Ack! Red Jewel is aligned to Blue Jewel";
                position = 0;
            }
        }        
        labelImageIs.setText(msg);
        
        Image img = HighGui.toBufferedImage(matImgDst);
        imgLabel.setIcon(new ImageIcon(img));        
        frame.pack();
        frame.repaint();
    }    
    
    /**
     * genImportList - generate a list of import statements required to run the prototype code
     */
    @Override
    public List<String> genImportList() {
        List<String> importsL = new ArrayList<String>();
        importsL.add("org.opencv.core.Mat");
        importsL.add("org.opencv.core.MatOfPoint");
        importsL.add("org.opencv.core.MatOfPoint2");
        importsL.add("org.opencv.core.Point");
        importsL.add("org.opencv.imgproc.Imgproc");
        return importsL;
    }     
    
    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {

            StringBuilder sb = new StringBuilder();
            sb.append("    public Integer doJoinRelicJewel");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("(List<MatOfPoint> redContours, List<MatOfPoint> blueContours) {\n");
            
            sb.append("        // If there is not exactly one red contour then this function returns null\n");
            sb.append("        if (redContours.size() != 1) {\n");
            sb.append("            return null;\n");
            sb.append("        }\n");
            sb.append("        // If there is not exactly one blue contour then this function returns null\n");
            sb.append("        if (blueContours.size() != 1) {\n");
            sb.append("            return null;\n");
            sb.append("        }\n");
            sb.append("        // Calculate center of red contour by bounding contour in a circle\n");
            sb.append("        Point redCenter = new Point();\n");
            sb.append("        MatOfPoint redContour = redContours.get(0);\n");
            sb.append("        MatOfPoint2f redContour2f = new MatOfPoint2f(redContour.toArray());\n");
            sb.append("        float[] redRadius = new float[1];\n");
            sb.append("        Imgproc.minEnclosingCircle(\n");
            sb.append("            redContour2f, // MatOfPoint2f - input Points\n");
            sb.append("            redCenter,    // Point        - center of circle\n");
            sb.append("            redRadius     // float[]      - radius of circle\n");
            sb.append("        );\n");
            sb.append("        // Calculate center of blue contour by bounding contour in a circle\n");
            sb.append("        Point blueCenter = new Point();\n");
            sb.append("        MatOfPoint   blueContour = blueContours.get(0);\n");
            sb.append("        MatOfPoint2f blueContour2f = new MatOfPoint2f(blueContour.toArray());\n");
            sb.append("        float[] blueRadius = new float[1];\n");
            sb.append("        Imgproc.minEnclosingCircle(\n");
            sb.append("            blueContour2f, // MatOfPoint2f - input Points\n");
            sb.append("            blueCenter,    // Point        - center of circle\n");
            sb.append("            blueRadius     // float[]      - radius of circle\n");
            sb.append("        );\n");
            sb.append("        if (redCenter.x < blueCenter.x) {\n");
            sb.append("            // Red Jewel is LEFT Blue Jewel is RIGHT\n");
            sb.append("            return 1;\n");
            sb.append("        }\n");
            sb.append("        else if (redCenter.x > blueCenter.x) {\n");
            sb.append("            // Red Jewel is RIGHT Blue Jewel is LEFT\n");
            sb.append("            return 2;\n");
            sb.append("        }\n");
            sb.append("        // ACK! Jewels are horizontally aligned onto each other\n");
            sb.append("        return 0;\n");
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
                new JoinRelicJewel(args);
            }
        });
    }
}
