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
 * LinkPolygons - Graphical user interface to exercise openCV Imgproc.approxPolyDP method.
 */
public class LinkPolygons extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //
    
    private JulipTrackBar epsilonTB;    
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
        
    private final int epsilonDefault = 0;
        
    private List<MatOfPoint> contours;    
    private List<MatOfPoint> drawnContours;    

    //
    //------------------------------------------------
    
    public LinkPolygons(String[] args) {

        // Initialize fields:
        //
        drawnContours = new ArrayList<>();
        codeFilename = "code_LinkPolygons.java";
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
                
        //
        // JPanel to hold:
        //      epsilon JSlider + Label for epsilon
        //
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        epsilonTB = new JulipTrackBar(0, 50, Integer.parseInt(myLinkMap.get("EPSILON")), 10, 5, this);
        sliderPanel.add(epsilonTB.label);
        sliderPanel.add(epsilonTB.slider);
        
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

    /**
     * loadImage - Import Contour file and update Sliders
     * @return boolean - True if successful parsing of Contour input file, else false
     */
    @Override
    public boolean loadImage(String filename) {
        boolean status = buildContours(textFieldImageIn.getText());
        verifySettings();                
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
    //   genImportList()
    //   genCodeString()
            
    /**
     * buildMyLinkMap - native default values for Link Gui
     */
    public void buildMyLinkMap() {
        Map<String, String> defaultMap = new HashMap<String, String>() {{
                put("TYPE",     "POLYGONS");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkpolygons.txt");
                put("EPSILON",   "");
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
        // Check for format and range validity for all 1 track bar
        //        
        intCheck("EPSILON", 0, 50, epsilonDefault);
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
            writer.write("EPSILON\t"     + epsilonTB.value + "\n");
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
        ContourHandler.saveContours(contourfilename, matImgDst, drawnContours);        
    }
        
    /**
     * refreshSettings - Overrides method in LinkClass; copy and error check myLinkMap settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        epsilonTB.label.setText(""+epsilonTB.value);
        return true;
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        epsilonTB.setValue(Integer.parseInt(myLinkMap.get("EPSILON")));
    }
    
    /**
     * refreshImage - refresh label texts and shown image
     */
    public void refreshImage() {
        Mat hierarchy = new Mat();
        matImgDst = Mat.zeros(matImgSrc.size(), CvType.CV_8UC3);
        Scalar color = new Scalar(0,255,255);      
        
        drawnContours.clear();
        double epsilon = epsilonTB.value;

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint contour = contours.get(i);
            MatOfPoint2f curve = new MatOfPoint2f();
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            contour.convertTo(curve, CvType.CV_32FC2);
            Imgproc.approxPolyDP(
                curve,                       // input contour (MatOfPoint2f)
                approxCurve,                 // output contour (MatOfPoint2f)
                epsilon,                     // output Mat of contour hierarchy
                true                         // boolean, true if closed contour
            );
            MatOfPoint approxContour = new MatOfPoint();
            approxCurve.convertTo(approxContour, CvType.CV_32S);
            drawnContours.add(approxContour);
        }
        for (int i = 0; i < drawnContours.size(); i++) {
            Imgproc.drawContours(
                matImgDst,          // input/output mat image
                drawnContours,      // input List of Mats of contours
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
        importsL.add("java.util.List");
        importsL.add("org.opencv.core.Core");
        importsL.add("org.opencv.core.CvType");
        importsL.add("org.opencv.core.Mat");
        importsL.add("org.opencv.core.MatOfPoint");
        importsL.add("org.opencv.core.MatOfPoint2");
        importsL.add("org.opencv.core.Point");
        importsL.add("org.opencv.core.Scalar");
        importsL.add("org.opencv.imgproc.Imgproc");
        return importsL;
    }     
    
    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {
            returnStr = "List<MatOfPoint>";
            objectStr = "polygonContoursList";
            StringBuilder msb = new StringBuilder();
            msb.append("doLinkPolygons");
            if (!reference.equals("")) { msb.append("_"+reference); }
            methodStr = msb.toString();

            StringBuilder sb = new StringBuilder();
            sb.append("    public "+returnStr+" "+methodStr.toString());  
            sb.append("(List<MatOfPoint> contours) {\n");
            sb.append("        List<MatOfPoint> polygonContours = new ArrayList<>();\n");
            sb.append("        MatOfPoint2f curve = new MatOfPoint2f();\n");
            sb.append("        MatOfPoint2f approxCurve = new MatOfPoint2f();\n");
            sb.append("        MatOfPoint approxContour = new MatOfPoint();\n");
            sb.append("        for (int i = 0; i < contours.size(); i++) {\n");
            sb.append("            MatOfPoint contour = contours.get(i);\n");
            sb.append("            contour.convertTo(curve, CvType.CV_32FC2);\n");
            sb.append("            Imgproc.approxPolyDP(\n");
            sb.append("                curve,              // input contour (MatOfPoint2f)\n");
            sb.append("                approxCurve,        // output contour (MatOfPoint2f)\n");
            sb.append("                "+epsilonTB.value+"         // double, parameter specifying approximation accuracy\n");
            sb.append("                true                // boolean, true if closed contour\n");
            sb.append("            );\n");
            sb.append("            approxCurve.convertTo(approxContour, CvType.CV_32S);\n");
            sb.append("            polygonContours.add(approxContour);\n");
            sb.append("        }\n");
            sb.append("        return polygonContours;\n");
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
                new LinkPolygons(args);
            }
        });
    }
}
