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
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkCenterStageProp - Graphical user interface to determine if an image shows a Team Prop in LEFT CENTER or RIGHT positions.
 *                       Webcam is assumed to see only LEFT and CENTER.
 *                       inputs contours (ctr) and outputs a final result (nil)
 */
public class LinkCenterStageProp extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //    
    private static final String[] PROP_STR = { 
        "NULL", 
        "LEFT",
        "CENTER",
        "RIGHT"
    };
    private int propIndex;

    private JLabel labelImageIs;
       
    private int leftSpikeMin = 100;
    private int leftSpikeMax = 400;
    private int leftPropMin  = 3000;
    private int leftPropMax  = 9000;
    private int centerSpikeMin = 100;
    private int centerSpikeMax = 400;
    private int centerPropMin  = 3000;
    private int centerPropMax  = 9000;
    
    private JLabel labelContours;
    
    private JLabel imgLabel;
    private Point anchor;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    List<MatOfPoint> contours;
    
    List<Point>   circleCenters;
    List<Double>  contourAreas;
    
    //
    //------------------------------------------------
        
    public LinkCenterStageProp(String[] args) {
    
        // Initialize fields:
        //
        anchor = new Point(-1,-1);
        circleCenters = new ArrayList<>();
        contourAreas = new ArrayList<>();
        codeFilename = "code_LinkCenterStageProp.java";
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
        matImgSrc = Mat.zeros(new Size(512, 512), CvType.CV_8U);
        if (buildContours(myLinkMap.get("IMAGE_IN")) == false) {
            contours = new ArrayList<>();
        };
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

        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        labelImageIs    = new JLabel("Team Prop is ");
        sliderPanel.add(new JLabel(" "));
        sliderPanel.add(labelImageIs);
        sliderPanel.add(new JLabel(" "));
        
        sliderPanel.add(buildLinkPanel());

        // Build frame
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

    /**
     * loadImage - Import Contour file and update Sliders
     * @return boolean - True if successful parsing of Contour input file, else false
     */
    @Override
    public boolean loadImage(String filename) {
        boolean status = buildContours(textFieldImageIn.getText());
        return status;
    }
    
    /**
     * buildContours - read contour file and set up image and contour data
     * @param filename - julip-formatted contour file name
     * @return boolean - true if file is properly read, else false
     */
    public boolean buildContours(String filename) {
    
        boolean status = false;
        float[] radius = null;
        // loadContours returns null if the file couldn't be parsed
        // else it returns a List<MatOfPoints>
        contours = ContourHandler.loadContours(filename);
        if (contours != null) {
        
            // Deal with the special case of the first element of the list:
            //
            // The first contour is faked-up as it holds the Size of the image Mat
            double[] fakeSize = contours.get(0).get(0,0);
            // Size is <col>x<row> and Mat methods are (row,col) so swap order of args
            matImgSrc = Mat.zeros((int)fakeSize[1], (int)fakeSize[0], CvType.CV_8UC3);
            // delete faked-up contour that holds the image Mat Size 
            contours.remove(0);
                        
            // Reset all list used to calculate stats
            circleCenters.clear();
            
            for (int i = 0; i < contours.size(); i++) {
                Point center = new Point();
                MatOfPoint   contour = contours.get(i);
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                Imgproc.minEnclosingCircle(
                    contour2f, // MatOfPoint2f points,
                    center,    // Point center,
                    radius     // float[] radius
                );
                circleCenters.add(center);
                contourAreas.add(Imgproc.contourArea(contour, false));
            }
        
            status = true;
        } 
        return status;
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
                put("TYPE", "CENTERSTAGEPROP");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkcontours.txt");
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
            writer.write("TYPE\tCENTERSTAGEPROP\n");
            writer.write("IMAGE_IN\t"    + textFieldImageIn.getText() + "\n");
            writer.write("IMAGE_OUT\t"   + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"   + linkfilename + "\n");
            writer.close();
        } catch (IOException e) {}
    }
    
    /**
     * saveImage - write output Image to file 
     */
    @Override
    public void saveImage() {
        String outputfilename = textFieldImageOut.getText();
        PointHandler.savePoints(outputfilename, matImgSrc, circleCenters);
    }

    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
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
        Scalar circleColor  = new Scalar(0,255,0);     // green
        
        for (int i = 0; i < circleCenters.size(); i++) {
            // keep for debugging
            //System.out.println("x,y = " + circleCenters.get(i).x + "," + circleCenters.get(i).y + " radius + " + circleRadii.get(i));
            Imgproc.circle(
                matImgDst,        // Mat img - input/output image
                circleCenters.get(i),    // Point center
                3,                // int radius
                circleColor,      // Scalar color
                3                 // int thickness
            );
        }
        
        int leftIdx = -1;
        int centerIdx = -1;
        propIndex = 0;
        boolean leftIsProp = false;
        boolean leftIsSpike = false;
        boolean centerIsProp = false;
        boolean centerIsSpike = false;
        int area;

        boolean error = false;
        // Error Check:
        if (!error && (leftSpikeMax < leftSpikeMin)) {
            labelImageIs.setText("ERROR: leftSpikeMax = "+leftSpikeMax+" and is less than leftSpikeMin = "+leftSpikeMin);
            error = true;
        }
        if (!error && (leftPropMin < leftSpikeMax )) {
            labelImageIs.setText("ERROR: leftPropMin = "+leftPropMin+" and is less than leftSpikeMax = "+leftSpikeMax);
            error = true;
        }
        if (!error && (leftPropMax < leftPropMin )) {
            labelImageIs.setText("ERROR: leftPropMax = "+leftPropMax+" and is less than leftPropMin = "+leftPropMin);
            error = true;
        }
        if (!error && (centerSpikeMax < centerSpikeMin)) {
            labelImageIs.setText("ERROR: centerSpikeMax = "+centerSpikeMax+" and is less than centerSpikeMin = "+centerSpikeMin);
            error = true;
        }
        if (!error && (centerPropMin < centerSpikeMax )) {
            labelImageIs.setText("ERROR: centerPropMin = "+centerPropMin+" and is less than centerSpikeMax = "+centerSpikeMax);
            error = true;
        }
        if (!error && (centerPropMax < centerPropMin )) {
            labelImageIs.setText("ERROR: centerPropMax = "+centerPropMax+" and is less than centerPropMin = "+centerPropMin);
            error = true;
        }

        if (!error) {
            if (circleCenters.size() == 2) {
                if (circleCenters.get(0).x < circleCenters.get(1).x) {
                    leftIdx = 0;
                    centerIdx = 1;
                } else {
                    leftIdx = 1;
                    centerIdx = 0;
                }
                area = contourAreas.get(leftIdx).intValue();
                leftIsSpike = ((area >= leftSpikeMin) && (area <= leftSpikeMax));
                leftIsProp  = ((area >= leftPropMin) && (area <= leftPropMax));
                area = contourAreas.get(centerIdx).intValue();
                centerIsSpike = ((area >= centerSpikeMin) && (area <= centerSpikeMax));
                centerIsProp  = ((area >= centerPropMin) && (area <= centerPropMax));

                if (leftIsSpike && centerIsSpike) {
                    propIndex = 3;
                } else if (leftIsProp && centerIsSpike) {
                    propIndex = 1;
                } else if (leftIsSpike && centerIsProp) {
                    propIndex = 2;
                }
                else if (leftIsProp && centerIsProp) {
                    labelImageIs.setText("ERROR: left AND center are BOTH detecting Prop ");
                    error = true;
                } else if (!leftIsSpike && !leftIsProp) {
                    labelImageIs.setText("ERROR: left is out of range of Spike AND Prop ");
                    error = true;
                } else if (!centerIsSpike && !centerIsProp) {
                    labelImageIs.setText("ERROR: center is out of range of Spike AND Prop ");
                    error = true;
                }

            }
            else {
                labelImageIs.setText("ERROR: expecting only 2 contours, there are "+circleCenters.size());
                error = true;
            }
        }

        if (!error) {
            labelImageIs.setText("\nTeam Prop is "+PROP_STR[propIndex] + "  ("+propIndex+")\n");
        }
        
        Image img = HighGui.toBufferedImage(matImgDst);
        imgLabel.setIcon(new ImageIcon(img));        
//        frame.pack();
        frame.repaint();
    }    
    
    /**
     * genImportList - generate a list of import statements required to run the prototype code
     */
    @Override
    public List<String> genImportList() {
        List<String> importsL = new ArrayList<String>();
        importsL.add("java.util.Collections");
        importsL.add("java.util.List");
        importsL.add("org.opencv.core.Core");
        importsL.add("org.opencv.core.CvType");
        importsL.add("org.opencv.core.Point");
        return importsL;
    }     
    
    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {
    
            StringBuilder sb = new StringBuilder();
            sb.append("    public List<Point> doLinkCenterStageProp");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("(List<MatOfPoint> contours) {\n");
            sb.append("        int leftSpikeMin = "+leftSpikeMin+";\n");
            sb.append("        int leftSpikeMax = "+leftSpikeMax+";\n");
            sb.append("        int leftPropMin  = "+leftPropMin+";\n");
            sb.append("        int leftPropMax  = "+leftPropMax+";\n");
            sb.append("        int centerSpikeMin = "+centerSpikeMin+";\n");
            sb.append("        int centerSpikeMax = "+centerSpikeMax+";\n");
            sb.append("        int centerPropMin  = "+centerPropMin+";\n");
            sb.append("        int centerPropMax  = "+centerPropMax+";\n");
            sb.append("        List<Point>   circleCenters = new ArrayList<>();\n");
            sb.append("        List<Point>   contourAreas = new ArrayList<>();\n");
            sb.append("        for (int i = 0; i < contours.size(); i++) {\n");
            sb.append("            Point center = new Point();\n");
            sb.append("            float[] radius = new float[1];\n");
            sb.append("            MatOfPoint   contour = contours.get(i);\n");
            sb.append("            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());\n");
            sb.append("            Imgproc.minEnclosingCircle(\n");
            sb.append("                contour2f, // MatOfPoint2f - input mat of points,\n");
            sb.append("                center,    // Point        - output center,\n");
            sb.append("                radius     // float[]      - output radius\n");
            sb.append("            );\n");
            sb.append("            circleCenters.add(center);\n");
            sb.append("            contourAreas.add(Imgproc.contourArea(contour, false));\n");
            sb.append("        }\n");
            sb.append("        int leftIdx = -1;\n");
            sb.append("        int centerIdx = -1;\n");
            sb.append("        propIndex = 0;\n");
            sb.append("        boolean leftIsProp = false;\n");
            sb.append("        boolean leftIsSpike = false;\n");
            sb.append("        boolean centerIsProp = false;\n");
            sb.append("        boolean centerIsSpike = false;\n");
            sb.append("        int area;\n");
            sb.append("        if (circleCenters.size() != 2) {\n");
            sb.append("            return propIndex;\n");
            sb.append("        }\n");            
            sb.append("        if (circleCenters.get(0).x < circleCenters.get(1).x) {\n");
            sb.append("            leftIdx = 0;\n");
            sb.append("            centerIdx = 1;\n");
            sb.append("        } else {\n");
            sb.append("            leftIdx = 1;\n");
            sb.append("            centerIdx = 0;\n");
            sb.append("        }\n");
            sb.append("        area = contourAreas.get(leftIdx).intValue();\n");
            sb.append("        leftIsSpike = ((area >= leftSpikeMin) && (area <= leftSpikeMax));\n");
            sb.append("        leftIsProp  = ((area >= leftPropMin) && (area <= leftPropMax));\n");
            sb.append("        area = contourAreas.get(centerIdx).intValue();\n");
            sb.append("        centerIsSpike = ((area >= centerSpikeMin) && (area <= centerSpikeMax));\n");
            sb.append("        centerIsProp  = ((area >= centerPropMin) && (area <= centerPropMax));\n");
            sb.append("        if (leftIsSpike && centerIsSpike) {\n");
            sb.append("            propIndex = 3;\n");
            sb.append("        } else if (leftIsProp && centerIsSpike) {\n");
            sb.append("            propIndex = 1;\n");
            sb.append("        } else if (leftIsSpike && centerIsProp) {\n");
            sb.append("            propIndex = 2;\n");
            sb.append("        }\n");
            sb.append("        return propIndex;\n");
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
                new LinkContourStats(args);
            }
        });
    }
}
