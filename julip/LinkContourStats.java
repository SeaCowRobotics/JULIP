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
 * LinkContourStats - Graphical user interface to display population statistics about contours.
 */
public class LinkContourStats extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //    
    private static final String[] CONTOUR_STR = { 
        "Contours:Show", 
        "Contours:Hide"
    };

    private static final String[] CIRCLE_STR = { 
        "Circles:Show", 
        "Circles:Hide"
    };
    
    private static final String[] MEDIANX_STR = { 
        "Median_X:Show", 
        "Median_X:Hide"
    };
    
    private static final String[] MEANX_STR = { 
        "Mean_X:Show", 
        "Mean_X:Hide"
    };

    private static final String[] MEDIANY_STR = { 
        "Median_Y:Show", 
        "Median_Y:Hide"
    };

    private static final String[] MEANY_STR = { 
        "Mean_Y:Show", 
        "Mean_Y:Hide"
    };
    
    private JulipComboBox contourCB;
    private JulipComboBox circleCB;
    private JulipComboBox medianxCB;
    private JulipComboBox meanxCB;
    private JulipComboBox medianyCB;
    private JulipComboBox meanyCB;
    
    private double medianx;
    private double meanx;
    private double mediany;
    private double meany;
    
    private JLabel labelContours;
    
    private JLabel imgLabel;
    private Point anchor;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    List<MatOfPoint> contours;
    
    List<Point>   circleCenters;
    List<Double>  circleRadii;
    List<Integer> drawnContourIndices;
        
    List<Double> xlist;
    List<Double> ylist;
    
    //
    //------------------------------------------------
        
    public LinkContourStats(String[] args) {
    
        // Initialize fields:
        //
        anchor = new Point(-1,-1);
        circleCenters = new ArrayList<>();
        circleRadii   = new ArrayList<>();
        drawnContourIndices = new ArrayList<>();        
        xlist = new ArrayList<>();
        ylist = new ArrayList<>();
        codeFilename = "code_LinkContourStats.java";
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
        
        contourCB  = new JulipComboBox(CONTOUR_STR, myLinkMap.get("CONTOUR_VISIBLE"), this);  
        circleCB   = new JulipComboBox(CIRCLE_STR,  myLinkMap.get("CIRCLE_VISIBLE"),  this);  
        medianxCB  = new JulipComboBox(MEDIANX_STR,  myLinkMap.get("MEDIANX_VISIBLE"),  this);  
        meanxCB    = new JulipComboBox(MEANX_STR,  myLinkMap.get("MEANX_VISIBLE"),  this);  
        medianyCB  = new JulipComboBox(MEDIANY_STR,  myLinkMap.get("MEDIANY_VISIBLE"),  this);  
        meanyCB    = new JulipComboBox(MEANY_STR,  myLinkMap.get("MEANY_VISIBLE"),  this);  
        
        sliderPanel.add(contourCB.comboBox);
        sliderPanel.add(circleCB.comboBox);
        sliderPanel.add(medianxCB.comboBox);
        sliderPanel.add(meanxCB.comboBox);
        sliderPanel.add(medianyCB.comboBox);
        sliderPanel.add(meanyCB.comboBox);
            
        labelContours    = new JLabel();            
        sliderPanel.add(labelContours);
        
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
                        
            // Reset all list used to calculate stats
            circleCenters.clear();
            circleRadii.clear();
            xlist.clear();
            ylist.clear();
            
            float[] radius = new float[1];
            float sumx = 0;
            float sumy = 0;
            for (int i = 0; i < contours.size(); i++) {
                Point center = new Point();
                MatOfPoint   contour = contours.get(i);
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                Imgproc.minEnclosingCircle(
                    contour2f, // MatOfPoint2f points,
                    center,    // Point center,
                    radius     // float[] radius
                );
                //System.out.println("x = " + center.x + "radii = " + radius[0]);
                xlist.add(center.x);
                ylist.add(center.y);
                sumx += center.x;
                sumy += center.y;
                circleCenters.add(center);
                circleRadii.add((double)radius[0]);            
            }
        
            Collections.sort(xlist);
            Collections.sort(ylist);
        
            // need to account for even size
            if (xlist.size() % 2 > 0) {
                medianx = xlist.get((xlist.size()-1)/2);
            }
            if (ylist.size() % 2 > 0) {
                mediany = ylist.get((ylist.size()-1)/2);
            }
            meanx = sumx / xlist.size();
            meany = sumy / ylist.size();
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
                put("TYPE", "CONTOURSTATS");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkcontours.txt");
                put("CONTOUR_VISIBLE", CONTOUR_STR[0]);
                put("CIRCLE_VISIBLE", CIRCLE_STR[0]);
                put("MEDIANX_VISIBLE", MEDIANX_STR[0]);
                put("MEANX_VISIBLE", MEANX_STR[0]);
                put("MEDIANY_VISIBLE", MEDIANY_STR[0]);
                put("MEANY_VISIBLE", MEANY_STR[0]);
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
        // Check for format and range validity for all 6 combo boxes
        //        
        comboCheck("CONTOUR_VISIBLE", CONTOUR_STR, 0);
        comboCheck("CIRCLE_VISIBLE",  CIRCLE_STR,  0);
        comboCheck("MEDIANX_VISIBLE", MEDIANX_STR, 0);
        comboCheck("MEANX_VISIBLE",   MEANX_STR,   0);
        comboCheck("MEDIANY_VISIBLE", MEDIANY_STR, 0);
        comboCheck("MEANY_VISIBLE",   MEANY_STR,   0);
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
            writer.write("TYPE\tCONTOURSTATS\n");
            writer.write("IMAGE_IN\t"    + textFieldImageIn.getText() + "\n");
            writer.write("IMAGE_OUT\t"   + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"   + linkfilename + "\n");
            writer.write("CONTOUR_VISIBLE\t" + CONTOUR_STR[contourCB.index] + "\n");
            writer.write("CIRCLE_VISIBLE\t"  + CIRCLE_STR[circleCB.index] + "\n");
            writer.write("MEDIANX_VISIBLE\t" + MEDIANX_STR[medianxCB.index] + "\n");
            writer.write("MEANX_VISIBLE\t"   + MEANX_STR[meanxCB.index] + "\n");
            writer.write("MEDIANY_VISIBLE\t" + MEDIANY_STR[medianyCB.index] + "\n");
            writer.write("MEANY_VISIBLE\t"   + MEANY_STR[meanyCB.index] + "\n");
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
        contourCB.setValue(myLinkMap.get("CONTOUR_VISIBLE"));
        circleCB.setValue(myLinkMap.get("CIRCLE_VISIBLE"));
        medianxCB.setValue(myLinkMap.get("MEDIANX_VISIBLE"));
        meanxCB.setValue(myLinkMap.get("MEANX_VISIBLE"));
        medianyCB.setValue(myLinkMap.get("MEDIANY_VISIBLE"));
        meanyCB.setValue(myLinkMap.get("MEANY_VISIBLE"));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; copy and error check myLinkMap settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {        
        labelContours.setText("Number of detected contours: " + contours.size()); 
        return true;
    }
    
    /**
     * refreshImage - refresh label texts and shown image
     */
    public void refreshImage() {
        Mat hierarchy = new Mat();
        Scalar contourColor = new Scalar(0,255,255);   // yellow
        Scalar circleColor  = new Scalar(0,255,0);     // green
        matImgDst = Mat.zeros(matImgSrc.size(), CvType.CV_8UC3);
        
        if (contourCB.index == 0) {
            Imgproc.drawContours(
                matImgDst,          // Mat input/output image
                contours,           // input List of Mats of contours
                -1,                 // index into List of Mats of contours
                contourColor,       // Scalar color of drawn contour
                2                   // pixel thickness of drawn contour
            );        
        }
        
        if (circleCB.index == 0) {
            for (int i = 0; i < circleCenters.size(); i++) {
                // keep for debugging
                //System.out.println("x,y = " + circleCenters.get(i).x + "," + circleCenters.get(i).y + " radius + " + circleRadii.get(i));
                Imgproc.circle(
                    matImgDst,               // Mat img - input/output image
                    circleCenters.get(i),    // Point center
                    (int)Math.round(circleRadii.get(i)), // int radius
                    circleColor,             // Scalar color
                    1                        // int thickness
                );
            }
        }
        
        if (medianxCB.index == 0) {
            Imgproc.line(
                matImgDst,
                new Point (medianx, 0),
                new Point (medianx, matImgDst.rows()),
                new Scalar (255, 255, 0),
                1
            );
        }
        
        if (meanxCB.index == 0) {
            Imgproc.line(
                matImgDst,
                new Point (meanx, 0),
                new Point (meanx, matImgDst.rows()),
                new Scalar (255, 0, 0),
                1
            );
        }

        if (medianyCB.index == 0) {
            Imgproc.line(
                matImgDst,
                new Point (matImgDst.cols(), mediany),
                new Point (0, mediany),
                new Scalar (255, 255, 0),
                1
            );
        }

        if (meanyCB.index == 0) {
            Imgproc.line(
                matImgDst,
                new Point (matImgDst.cols(), meany),
                new Point (0, meany),
                new Scalar (255, 0, 0),
                1
            );
        }
        
        // keep for debugging
        //System.out.println("rows = " + matImgDst.rows());
        //System.out.println("cols = " + matImgDst.cols());
        //System.out.println("meany = " + meany);
        //System.out.println("mediany = " + mediany);
        
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
            sb.append("    public List<Point> doLinkContourStats");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("(List<MatOfPoint> contours) {\n");
            
            sb.append("        List<Point>   circleCenters = new ArrayList<>();\n");
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
            sb.append("        }\n");
            sb.append("        return circleCenters;\n");
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
