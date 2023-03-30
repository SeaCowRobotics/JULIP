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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkRelicPictograph - Graphical user interface to show solutions to image processing for Relic Recovery pictograph identification.
 */
public class LinkRelicPictograph extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //            
    private static final String[] PICTOGRAPH_STR = { 
        "NULL", 
        "LEFT",
        "CENTER",
        "RIGHT"
    };
    private int pictographIndex;
        
    private JLabel labelImageIs;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private List<Point> points;
    List<Double> xlist;
    List<Double> ylist;    
    private double medianx;
    private double mediany;
    private double meanx;
    private double meany;
    //
    //------------------------------------------------
        
    public LinkRelicPictograph(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkRelicPictograph.java";
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
        buildPoints(myLinkMap.get("IMAGE_IN"));
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

        
        labelImageIs    = new JLabel("Pictograph is ");
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
        frame.setVisible(true);
        frameHeightMinusImage = frame.getSize().height - imgSP.getSize().height; 
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
        boolean status = buildPoints(textFieldImageIn.getText());
        return status;
    }
    
    /**
     * buildPoints - read points file and set up image and point data
     * @param filename - julip-formatted points file name
     * @return boolean - true if file is properly read, else false
     */
    public boolean buildPoints(String filename) {
    
        boolean status = false;
        // loadContours returns null if the file couldn't be parsed
        // else it returns a List<Point>
        points = PointHandler.loadPoints(filename);
        if (points != null) {
        
            xlist = new ArrayList<>();
            ylist = new ArrayList<>();
                
            // Deal with the special case of the first element of the list:
            //
            // The first point is faked-up as it holds the Size of the image Mat
            double[] fakeSize = {points.get(0).x, points.get(0).y};
            // Size is <col>x<row> and Mat methods are (row,col) so swap order of args
            matImgSrc = Mat.zeros((int)fakeSize[1], (int)fakeSize[0], CvType.CV_8UC3);
            // delete faked-up contour that holds the image Mat Size 
            points.remove(0);
                        
            double sumx = 0;
            double sumy = 0;
            for (int i = 0; i < points.size(); i++) {
                double x = points.get(i).x;
                double y = points.get(i).y;
                xlist.add(x);
                ylist.add(y);            
                sumx += x;
                sumy += y;
            }
                
            Collections.sort(xlist);
            Collections.sort(ylist);
                
            medianx = xlist.get((xlist.size()-1)/2);
            mediany = ylist.get((ylist.size()-1)/2);
            
            meanx = sumx / points.size();
            meany = sumy / points.size();
            status = true;
        } else {
            // If there was a file exception the set the
            // image to a default blackness of arbitary size.
            matImgSrc = Mat.zeros(400, 400, CvType.CV_8UC3);
            // Set points to a non-null List
            points = new ArrayList<>();
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
            writer.write(pictographIndex + "\n");
            writer.write(PICTOGRAPH_STR[pictographIndex] + "\n");
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
        Scalar circleColor  = new Scalar(0,255,0);     // green
        
        for (int i = 0; i < points.size(); i++) {
            // keep for debugging
            //System.out.println("x,y = " + circleCenters.get(i).x + "," + circleCenters.get(i).y + " radius + " + circleRadii.get(i));
            Imgproc.circle(
                matImgDst,        // Mat img - input/output image
                points.get(i),    // Point center
                3,                // int radius
                circleColor,      // Scalar color
                3                 // int thickness
            );
        }
        
        pictographIndex = 0;
        
        if (points.size() == 7) {
            pictographIndex = 1;
        } else if (points.size() == 11) {
            if (meanx < medianx) {
                pictographIndex = 2;
            } else {
                pictographIndex = 3;
            }
        }
        
        double textmeanx = Math.round(meanx * 10.0) / 10.0;
        double textmedianx = Math.round(medianx * 10.0) / 10.0;
        
        labelImageIs.setText("Points "+points.size()+" MeanX "+textmeanx+" MedianX "+textmedianx+ 
                             "\nPictograph is "+PICTOGRAPH_STR[pictographIndex] + "  ("+pictographIndex+")\n");
        
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
            sb.append("    public int doLinkRelicPictograph");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("(List<Point> points) {\n");
            
            sb.append("        int pictographID = 0;\n");
            sb.append("        List<double> xlist = new ArrayList<>();\n");
            sb.append("        double sumx = 0;\n");
            sb.append("        for (int i = 0; i < points.size(); i++) {\n");
            sb.append("            double x = points.get(i).x;\n");
            sb.append("            xlist.add(x);\n");
            sb.append("            sumx += x;\n");
            sb.append("        }\n");
            sb.append("        Collections.sort(xlist);\n");
            sb.append("        double medianx = xlist.get((xlist.size()-1)/2);\n");
            sb.append("        double meanx = sumx / points.size();\n");
            sb.append("        if (points.size() == 7) {\n");
            sb.append("            pictographID = 1;\n");
            sb.append("        } else if (points.size() == 11) {\n");
            sb.append("             if (meanx < medianx) {\n");
            sb.append("                 pictographID = 2;\n");
            sb.append("             } else {\n");
            sb.append("                pictographID = 3;\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("        return pictographID;\n");
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
                new LinkRelicPictograph(args);
            }
        });
    }
}
