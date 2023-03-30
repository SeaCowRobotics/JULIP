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
 * LinkRoverMineral - Graphical user interface to show solutions to image processing for Relic Recovery pictograph identification.
 */
public class LinkRoverMineral extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //            
    private static final String[] MINERAL_STR = { 
        "NULL", 
        "LEFT",
        "CENTER",
        "RIGHT"
    };
    private int mineralIndex;
        
    private JLabel labelImageIs;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    List<MatOfPoint> contours;
    List<Double> xlist;
    List<Double> ylist;    
    private double centerx;
    private double centery;
    //
    //------------------------------------------------
        
    public LinkRoverMineral(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkRoverMineral.java";
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

        
        labelImageIs    = new JLabel("Gold Mineral is ");
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
    
    /**
     * buildMyLinkMap - native default values for Link Gui
     */    
    public void buildMyLinkMap() {
        Map<String, String> defaultMap = new HashMap<String, String>() {{
                put("TYPE", "FINDCONTOURS");
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
            writer.write("TYPE\tRELICPICTOGRAPH\n");
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
        
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(outputfilename));
            writer.write(mineralIndex + "\n");
            writer.write(MINERAL_STR[mineralIndex] + "\n");
            writer.close();
        } catch (IOException e) {}
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {}
    
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
        Scalar yellowColor = new Scalar(0,255,255);    
        Scalar greenColor  = new Scalar(0,255,0);
        Scalar redColor    = new Scalar(0,0,255);
        mineralIndex = 0;
        
        if (contours.size() > 0) {
            MatOfPoint2f contourMat = new MatOfPoint2f(contours.get(0).toArray());
            Point circleCenter = new Point();
            float[] circleRadii = new float[1];
            Imgproc.minEnclosingCircle(contourMat, circleCenter, circleRadii);
                    
            Imgproc.circle(
                matImgDst,           // Mat img
                circleCenter,        // Point center
                2,                   // int radius
                redColor,          // Scalar color
                3                    // int thickness
            );                    
                
            double x1 = matImgDst.cols()/3.0;
            double x2 = 2.0 * x1;
            double y1 = 0.0;
            double y2 = (double)matImgDst.rows();
        
            // Vertical line delineating Left and Center boundary
            Imgproc.line(
                matImgDst,        // Mat: image
                new Point(x1,y1), // Point: endpoint of Line
                new Point(x1,y2), // Point: endpoint of Line
                greenColor        // Scalar: color
            );
            // Vertical line delineating Center and Right boundary
            Imgproc.line(
                matImgDst,        // Mat: image
                new Point(x2,y1), // Point: endpoint of Line
                new Point(x2,y2), // Point: endpoint of Line
                greenColor        // Scalar: color
            );
        
            Imgproc.drawContours(
                matImgDst,          // input/output mat image
                contours,           // input List of Mats of contours
                -1,                  // index into List of Mats of contours
                yellowColor,        // Scalar color of drawn contour
                2,                  // pixel thickness of drawn contour
                Imgproc.LINE_8      // LineType of drawn contour
            );  
                                    
            if (circleCenter.x < x1) {
                mineralIndex = 1;
            }
            else if (circleCenter.x > x2/3) {
                mineralIndex = 3;
            }
            else
                mineralIndex = 2;
        }
        labelImageIs.setText("\nMineral is "+MINERAL_STR[mineralIndex] + "  ("+mineralIndex+")\n");        
        
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
        importsL.add("java.util.List");
        importsL.add("org.opencv.core.Mat");
        importsL.add("org.opencv.core.MatOfPoint");
        importsL.add("org.opencv.core.MatOfPoint2f");
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
            sb.append("    public int doRoverMineral");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("(List<MatOfPoint> contours, Mat image) {\n");
            
            sb.append("        if (contours.size() == 0) {\n");
            sb.append("            return 0;\n");
            sb.append("        }\n");
            sb.append("        // Get center of Bounding Circle\n");
            sb.append("        MatOfPoint2f contourMat = new MatOfPoint2f(contours.get(0).toArray());\n");
            sb.append("        Point circleCenter = new Point();\n");
            sb.append("        float[] circleRadii = new float[1];\n");
            sb.append("        Imgproc.minEnclosingCircle(contourMat, circleCenter, circleRadii);\n");
            sb.append("        // Get width of image -- will divide into thirds\n");
            sb.append("        double x1 = image.cols()/3.0;\n");
            sb.append("        double x2 = 2.0 * x1;\n");
            sb.append("        if (circleCenter.x < x1) {\n");
            sb.append("            // LEFT\n");
            sb.append("            return 1;\n");
            sb.append("        }\n");
            sb.append("        else if (circleCenter.x > x2/3) {\n");
            sb.append("            // RIGHT\n");
            sb.append("            return 3;\n");
            sb.append("        }\n");
            sb.append("        else\n");
            sb.append("            // CENTER\n");
            sb.append("            return 2;\n");
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
                new LinkRoverMineral(args);
            }
        });
    }
}
