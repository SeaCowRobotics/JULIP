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
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkFilterContours - Graphical user interface to preferentially filter opencv contours based on minimum enclosing geometries.
 */
public class LinkGeometryContours extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //    
    private final String[] GEOMETRY_STR = { 
        "Bounding_Rect", 
        "Bounding_Rotated_Rect",
        "Bounding_Circle"
    };
    private final String[] PERCENT_STR = { 
        "Percent:Slider", 
        "Percent:Fixed"
    };

    private JulipComboBox geometryCB;
    private JulipComboBox percentCB;    
    private JulipTrackBar pctMinTB;    
    private JulipTrackBar pctMaxTB;    
    private JTextField pctMinTF;
    private JTextField pctMaxTF;
        
    private int textWidth;
    
    private double minPct;         // calculated minimum percent area for filter
    private double maxPct;         // calculated maximum percent area for filter
    
    private JLabel labelContours;
    private JLabel labelDrawnContours;
    
    private JLabel imgLabel;
    private Point anchor;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    List<MatOfPoint> contours;    
    List<Double> contourPcts;
    List<Double> sortedContourPcts;
    List<Integer> drawnContourIndices;
    //
    //------------------------------------------------
    
    public LinkGeometryContours(String[] args) {
    
        // Initialize fields:
        //
        anchor = new Point(-1,-1);
        contourPcts = new ArrayList<>();
        drawnContourIndices = new ArrayList<>();
        textWidth = 15;
        codeFilename = "code_LinkGeometryContours.java";
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
        
        // Controls for dynamic Area Contour filters
        //    JComboBox to select bounding geometry
        //    JComboBox to select sliders vs. text fields
        //    Slider + Label for min Pct contour
        //    Slider + Label for max Pct contour
        geometryCB = new JulipComboBox(GEOMETRY_STR, myLinkMap.get("GEOMETRY_SELECT"), this);      
        buildSortedContours();
        percentCB = new JulipComboBox(PERCENT_STR, myLinkMap.get("PERCENT_SELECT"), this);        
        pctMinTB = new JulipTrackBar(0, contours.size(), Integer.parseInt(myLinkMap.get("PCT_MIN")), contours.size()/5, -1, this);
        pctMaxTB = new JulipTrackBar(0, contours.size(), Integer.parseInt(myLinkMap.get("PCT_MAX")), contours.size()/5, -1, this);
        //
        sliderPanel.add(geometryCB.comboBox);
        sliderPanel.add(percentCB.comboBox);
        sliderPanel.add(pctMinTB.label);            
        sliderPanel.add(pctMinTB.slider);            
        sliderPanel.add(pctMaxTB.label);            
        sliderPanel.add(pctMaxTB.slider);            
        
        // JPanel for fixed Area Contour filters
        //    TextField for min Area
        //    JButton to assert min/max Area filters
        //    TextField for max Area
        JPanel pctFixedPanel = new JPanel();
        pctMinTF = new JTextField(myLinkMap.get("PCT_FIXED_MIN"),textWidth);
        pctMaxTF = new JTextField(myLinkMap.get("PCT_FIXED_MAX"),textWidth);
        pctFixedPanel.add(pctMinTF);
        JButton pctFixedB = new JButton("< % Range <");
        pctFixedB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSettings();
                refreshImage();
            }
        });
        pctFixedPanel.add(pctFixedB);
        pctFixedPanel.add(pctMaxTF);
        sliderPanel.add(pctFixedPanel);
           
        labelContours      = new JLabel();            
        labelDrawnContours = new JLabel();            
        sliderPanel.add(labelContours);
        sliderPanel.add(labelDrawnContours);
        
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
        //
        // Reconfigure the sliders to span the new contour list size
        //
        pctMinTB.setMaximum(contours.size());
        pctMaxTB.setMaximum(contours.size());

        pctMinTB.setValue(0);
        pctMaxTB.setValue(contours.size());
        
        pctMinTB.slider.setMajorTickSpacing(contours.size()/5);
        pctMaxTB.slider.setMajorTickSpacing(contours.size()/5);

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
        
    public void buildSortedContours() {
        
        if (contours != null) {
            // iterate over list of contours and generate a list
            // of ratio of contour area to bounding geometry area
            contourPcts.clear();
            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint contour = contours.get(i);
                double area = Imgproc.contourArea(contour, false);
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                
                double boundingArea = 1.0;
                // Check if the ComboBox is set to 0 for a Rect bounding geometry
                if (geometryCB.index == 0) {
                    Rect boundingRect = Imgproc.boundingRect(contours.get(i));
                    boundingArea = boundingRect.area();
                }
                // Check if the ComboBox is set to 0 for a RotatedRect bounding geometry
                else if (geometryCB.index == 1) {      
                    RotatedRect rotatedRect = Imgproc.minAreaRect(contour2f);
                    // To get the vertices use points method which returns an array of 4 Points
                    // The Points are in sequential order around the perimenter of the RotatedRect
                    Point[] rectPoints = new Point[4];
                    rotatedRect.points(rectPoints);
                    // calculate distances between points = sqrt((x2-x1)^2 + (y2-y1)^2)
                    double d1 = Math.sqrt(Math.pow(rectPoints[0].x-rectPoints[1].x,2) +
                                Math.pow(rectPoints[0].y-rectPoints[1].y,2));
                    double d2 = Math.sqrt(Math.pow(rectPoints[2].x-rectPoints[1].x,2) +
                                Math.pow(rectPoints[2].y-rectPoints[1].y,2));
                    // area = length(side1) * length(side2)
                    boundingArea = d1 * d2;
                }
                // Check if the ComboBox is set to 2 for a Circle bounding geometry
                else if (geometryCB.index == 2) {
                    Point circleCenter = new Point();
                    float[] circleRadii = new float[1];
                    Imgproc.minEnclosingCircle(contour2f, circleCenter, circleRadii);
                    float circleRadius = circleRadii[0];
                    boundingArea = Math.PI * circleRadius * circleRadius;
                }
                contourPcts.add(area/boundingArea);                
            }
            sortedContourPcts = new ArrayList<>();
            sortedContourPcts.addAll(contourPcts);
            Collections.sort(sortedContourPcts);        
        } 
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
                put("TYPE", "FILTERCONTOURS");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkcontours.txt");
                put("GEOMETRY_SELECT", GEOMETRY_STR[0]);
                put("PCT_SELECT", PERCENT_STR[0]);
                put("PCT_MIN", "0");
                put("PCT_MAX", "");
                put("PCT_FIXED_MIN", "");
                put("PCT_FIXED_MAX", "");
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
        // Check for format and range validity for all 4 track bars
        //                
        // The maximum setting depends on the number of contours in the source image, not a fixed number
        int hiLimit = 1;
        if (contours != null) {
            hiLimit = contours.size();
        }            
        intCheck("PCT_MIN", 0, hiLimit, 0);
        intCheck("PCT_MAX", 0, hiLimit, hiLimit);
        //
        // Check for format and range validity for all 4 text fields
        //
        numberOrVoidCheck("PCT_FIXED_MIN");
        numberOrVoidCheck("PCT_FIXED_MAX");
        //
        // Check for format and range validity for all 2 combo boxes
        //        
        comboCheck("GEOMETRY_SELECT", GEOMETRY_STR, 0);
        comboCheck("PERCENT_SELECT", PERCENT_STR, 0);
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
            writer.write("TYPE\tGEOMETRYCONTOURS\n");
            writer.write("IMAGE_IN\t"    + textFieldImageIn.getText() + "\n");
            writer.write("IMAGE_OUT\t"   + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"   + linkfilename + "\n");
            writer.write("CONTOUR_FILE\t"+ "nope" + "\n");
            writer.write("GEOMETRY_SELECT\t" + GEOMETRY_STR[geometryCB.index] + "\n");
            writer.write("PERCENT_SELECT\t" + PERCENT_STR[percentCB.index] + "\n");
            writer.write("PCT_MIN\t"    + pctMinTB.value + "\n");
            writer.write("PCT_MAX\t"    + pctMaxTB.value + "\n");
            if (!pctMinTF.getText().trim().equals("")) {
                writer.write("PCT_FIXED_MIN\t"      + pctMinTF.getText() + "\n");
            }
            if (!pctMaxTF.getText().trim().equals("")) {
                writer.write("PCT_FIXED_MAX\t"      + pctMaxTF.getText() + "\n");
            }
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
        List<MatOfPoint> drawnContours = new ArrayList<>();
         for (int i = 0; i < drawnContourIndices.size(); i++) {            
            drawnContours.add(contours.get(drawnContourIndices.get(i)));
        }
        ContourHandler.saveContours(contourfilename, matImgSrc, drawnContours);        
    }
            
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        pctMinTB.setValue(Integer.parseInt(myLinkMap.get("PCT_MIN")));
        pctMaxTB.setValue(Integer.parseInt(myLinkMap.get("PCT_MAX")));
        pctMinTF.setText(myLinkMap.get("PCT_FIXED_MIN"));
        pctMaxTF.setText(myLinkMap.get("PCT_FIXED_MAX"));
        percentCB.setValue(myLinkMap.get("PERCENT_SELECT"));
        geometryCB.setValue(myLinkMap.get("GEOMETRY_SELECT"));
    }
            
    
    /**
     * refreshSettings - Overrides method in LinkClass; copy and error check myLinkMap settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {    
    
        buildSortedContours();
        //
        // Update Pct Labels
        //
        String lowerMinPctLimit = "0";
        String upperMinPctLimit = "100";        
        if (pctMinTB.value < contours.size()) {
            upperMinPctLimit = String.format("%.1f", 100*sortedContourPcts.get(pctMinTB.value));
        }
        if (pctMinTB.value > 0) {
            lowerMinPctLimit = String.format("%.1f", 100*sortedContourPcts.get(pctMinTB.value-1));
        }
        pctMinTB.label.setText("("+pctMinTB.value+")  "+ lowerMinPctLimit + " < Min % Threshold <= " + upperMinPctLimit);
        
        String lowerMaxPctLimit = "-1";
        String upperMaxPctLimit = "infinite";
        if (pctMaxTB.value < contours.size()) {
            upperMaxPctLimit = String.format("%.1f", 100*sortedContourPcts.get(pctMaxTB.value));
        }
        if (pctMaxTB.value > 0) {
            lowerMaxPctLimit = String.format("%.1f", 100*sortedContourPcts.get(pctMaxTB.value-1));
        }
        pctMaxTB.label.setText("("+pctMaxTB.value+")  "+ lowerMaxPctLimit + " <= Max % threshold < " + upperMaxPctLimit);
        
        labelContours.setText("Number of initial contours: " + contours.size()); 
        
        return true;
    }
    
    /**
     * refreshImage - refresh label texts and shown image
     */
    public void refreshImage() {
                
        Mat hierarchy = new Mat();
        matImgDst = Mat.zeros(matImgSrc.size(), CvType.CV_8UC3);
        
        Scalar yellowColor = new Scalar(0,255,255);    
        Scalar greenColor  = new Scalar(0,255,0);
        
        calculatePercentThresholds();
        
        drawnContourIndices.clear();
        for (int i = 0; i < contours.size(); i++) {
            double percent = contourPcts.get(i);
            
            if (( percent > minPct ) &&
                (( maxPct < 0) || ( percent < maxPct))
            ) {            
                drawnContourIndices.add(i);
                Imgproc.drawContours(
                    matImgDst,          // input/output mat image
                    contours,           // input List of Mats of contours
                    i,                  // index into List of Mats of contours
                    yellowColor,        // Scalar color of drawn contour
                    2,                  // pixel thickness of drawn contour
                    Imgproc.LINE_8,     // LineType of drawn contour
                    hierarchy,          // input Mat of contour hierarchy
                    0,                  // hierarchy threshold of drawn contours
                    new Point()         // contour x,y offset
                );  
                
                MatOfPoint2f contourMat = new MatOfPoint2f(contours.get(i).toArray());
                
                if (geometryCB.index == 0) {
                    Rect boundingRect = Imgproc.boundingRect(contours.get(i));
                    Imgproc.rectangle(
                        matImgDst,          // Mat img
                        boundingRect,       // Rect rec
                        greenColor          // Scalar color
                    );
                }
                else if (geometryCB.index == 1) {        
                    RotatedRect rotatedRect = Imgproc.minAreaRect(contourMat);
                    Point[] rectPoints = new Point[4];
                    rotatedRect.points(rectPoints);                    
                    for (int p = 0; p < 4; p++) {
                        Imgproc.line(
                            matImgDst,           // Mat img
                            rectPoints[p],       // Point pt1
                            rectPoints[(p+1)%4], // Point pt2
                            greenColor           // Scalar color
                        );
                    }
                }
                else if (geometryCB.index == 2) {
                    Point circleCenter = new Point();
                    float[] circleRadii = new float[1];
                    Imgproc.minEnclosingCircle(contourMat, circleCenter, circleRadii);
                    float circleRadius = circleRadii[0];
                    
                    Imgproc.circle(
                        matImgDst,           // Mat img
                        circleCenter,        // Point center
                        (int)circleRadius,   // int radius
                        greenColor           // Scalar color
                    );                    
                }
            }
        }
        
        labelDrawnContours.setText("Number of filtered contours: " + drawnContourIndices.size()); 
        
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
        importsL.add("org.opencv.core.Mat");
        importsL.add("org.opencv.core.MatOfPoint");
        if (geometryCB.index == 0) {
            importsL.add("org.opencv.core.Rect");
        }
        else if (geometryCB.index == 1) {
            importsL.add("org.opencv.core.MatOfPoint2f");
            importsL.add("org.opencv.core.Point");
            importsL.add("org.opencv.core.RotatedRect");
        }
        else if (geometryCB.index == 2) {
            importsL.add("org.opencv.core.MatOfPoint2f");
            importsL.add("org.opencv.core.Point");
        }        
        importsL.add("org.opencv.imgproc.Imgproc");
        return importsL;
    }     
    
    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {
            returnStr = "List<MatOfPoint>";
            objectStr = "geometryContoursList";
            StringBuilder msb = new StringBuilder();
            msb.append("doLinkGeometryContours");
            if (!reference.equals("")) { msb.append("_"+reference); }
            methodStr = msb.toString();
                
            StringBuilder sb = new StringBuilder();
            sb.append("    public "+returnStr+" "+methodStr.toString()); 
            sb.append("(List<MatOfPoint> contours) {\n");            
            sb.append("        List<MatOfPoin> filteredContours = new ArrayList<>();\n");
            sb.append("        double minPct = "+minPct+";\n");
            sb.append("        double maxPct = "+maxPct+";\n");           
            sb.append("        // Iterate over List of contours to calculate the percent overlap to bounding contour geometry.\n");
            sb.append("        for (int i = 0; i < contours.size(); i++)\n");
            sb.append("            MatOfPoint contour = contours.get(i);\n");
            sb.append("            // call contourArea to get area of the contour\n");
            sb.append("            double area = Imgproc.countourArea(contour, false);\n");
            // for bounding geometry of Rect:
            if (geometryCB.index == 0) {
                sb.append("            // Create a Rect bounding goemetry\n");
                sb.append("            Rect boundingRect = Imgproc.boundingRect(contour);\n");
                sb.append("            double boundingArea = boundingRect.area();\n");
            }
            // for bounding geometry of RotatedRect:
            else if (geometryCB.index == 1) {
                sb.append("            // Create a RotatedRect bounding geometry\n");
                sb.append("            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());\n");
                sb.append("            RotatedRect rotatedRect = Imgproc.minAreaRect(contour2f);\n");
                sb.append("            // to calculate the RotatedRect area we need the vertices of the rectangle\n");
                sb.append("            Point[] rectPoints = new Point[4];\n");
                sb.append("            rotatedRect.points(rectPoints);\n");
                sb.append("            // calculate length of one side of the rotatedRect\n");
                sb.append("            double d1 = Math.sqrt(Math.pow(rectPoints[0].x-rectPoints[1].x,2) +\n");
                sb.append("                        Math.pow(rectPoints[0].y-rectPoints[1].y,2));\n");
                sb.append("            // calculate length of second side of the rotatedRect\n");
                sb.append("            double d2 = Math.sqrt(Math.pow(rectPoints[2].x-rectPoints[1].x,2) +\n");
                sb.append("                        Math.pow(rectPoints[2].y-rectPoints[1].y,2));\n");
                sb.append("            double boundingArea = d1 * d2;\n");
            }
            else if (geometryCB.index == 2) {
                sb.append("            // Create a Circle bounding geometry\n");
                sb.append("            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());\n");
                sb.append("            Point circleCenter     = new Point();\n");
                sb.append("            // Calculate area of bounding Circle: A = pi*r*r\n");
                sb.append("            float[] circleRadii    = new float[1];\n");
                sb.append("            Imgproc.minEnclosingCircle(contour2f, circleCenter, circleRadii);\n");
                sb.append("            float circleRadius  = circleRadii[0];\n");
                sb.append("            double boundingArea = Math.PI * circleRadius * circleRadius;\n");
            }
            else {
                sb.append("            // No bounding geometry selected\n");
                sb.append("            double boundingArea = area;\n");
            }
            sb.append("            // Calculate ratio of area of contour to area of its bounding geometry\n");
            sb.append("            double contourPct = area/boundingArea;\n");
            sb.append("            if ((contourPct >= minPct) && (contourPct <= maxPct)) {\n");
            sb.append("                filteredContours.add(contour);\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("        return filteredContours;\n");
            sb.append("    }\n");    
        return sb.toString();
    }
    
    //
    //  end of overridden LinkClass methods
    //-----------------------------------------------------------------------------------

    /**
     * calculatePercentThresholds - utility method to determin min,max Area and Perimeter for contour filtering.
     */
    public void calculatePercentThresholds() {
        minPct = -1;
        maxPct = -1;
        if (percentCB.index == 0) {
            if (pctMinTB.value > 0) {
                minPct = sortedContourPcts.get(pctMinTB.value-1);
            }            
            if (pctMaxTB.value != contours.size()) {
                maxPct = sortedContourPcts.get(pctMaxTB.value);
            }
        } else {
            if (pctMinTF.getText().equals("")) {
                minPct = -1;
            } else {
                minPct = Double.parseDouble(pctMinTF.getText())/100;                
            }
            if (pctMaxTF.getText().equals("")) {
                maxPct = -1;
            } else {
                maxPct = Double.parseDouble(pctMaxTF.getText())/100;                
            }
        }        
    }
    
    
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
                new LinkGeometryContours(args);
            }
        });
    }
}
