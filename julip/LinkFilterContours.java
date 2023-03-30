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
 * LinkFilterContours - Graphical user interface to preferentially filter opencv contours based on area and perimeter.
 */
public class LinkFilterContours extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //    
    private final String[] AREA_STR = { 
        "Area:Slider", 
        "Area:Fixed"
    };

    private final String[] PERIMETER_STR = { 
        "Perimeter:Slider", 
        "Perimeter:Fixed"
    };
    
    private JulipTrackBar areaMinTB;    
    private JulipTrackBar areaMaxTB;    
    private JulipComboBox areaCB;
    private JTextField areaMinTF;
    private JTextField areaMaxTF;
    
    private JulipTrackBar periMinTB;    
    private JulipTrackBar periMaxTB;    
    private JulipComboBox periCB;
    private JTextField periMinTF;
    private JTextField periMaxTF;
    
    private int textWidth;
    
    private double minArea;         // calculated minimum contour area for filter
    private double maxArea;         // calculated maximum contour area for filter
    private double minPerimeter;    // calculated minimum contour perimeter for filter
    private double maxPerimeter;    // calculated maximum contour perimeter for filter
    
    private JLabel labelContours;
    private JLabel labelDrawnContours;
    
    private JLabel imgLabel;
    private Point anchor;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    List<MatOfPoint> contours;    
    List<Double> contourAreas;
    List<Double> contourPerimeters;
    List<Integer> drawnContourIndices;
    //
    //------------------------------------------------
    
    public LinkFilterContours(String[] args) {
    
        // Initialize fields:
        //
        anchor = new Point(-1,-1);
        contourAreas = new ArrayList<>();
        contourPerimeters = new ArrayList<>();
        drawnContourIndices = new ArrayList<>();
        textWidth = 15;
        codeFilename = "code_LinkFilterContours.java";
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
        //    JComboBox to select sliders vs. text fields
        //    Slider + Label for min Area contour
        //    Slider + Label for max Area contour
        areaCB    = new JulipComboBox(AREA_STR, myLinkMap.get("AREA_SELECT"), this);        
        areaMinTB = new JulipTrackBar(0, contours.size(), Integer.parseInt(myLinkMap.get("AREA_MIN")), contours.size()/5, -1, this);
        areaMaxTB = new JulipTrackBar(0, contours.size(), Integer.parseInt(myLinkMap.get("AREA_MAX")), contours.size()/5, -1, this);
        //
        sliderPanel.add(areaCB.comboBox);
        sliderPanel.add(areaMinTB.label);            
        sliderPanel.add(areaMinTB.slider);            
        sliderPanel.add(areaMaxTB.label);            
        sliderPanel.add(areaMaxTB.slider);            
        
        // JPanel for fixed Area Contour filters
        //    TextField for min Area
        //    JButton to assert min/max Area filters
        //    TextField for max Area
        JPanel areaFixedPanel = new JPanel();
        areaMinTF = new JTextField(myLinkMap.get("AREA_FIXED_MIN"),textWidth);
        areaMaxTF = new JTextField(myLinkMap.get("AREA_FIXED_MAX"),textWidth);
        areaFixedPanel.add(areaMinTF);
        JButton buttonAreaFixed = new JButton("< Area Range <");
        buttonAreaFixed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSettings();
                refreshImage();
            }
        });
        areaFixedPanel.add(buttonAreaFixed);
        areaFixedPanel.add(areaMaxTF);
        sliderPanel.add(areaFixedPanel);

        // Controls for dynamic Perimeter Contour filters
        //    JComboBox to select sliders vs. text fields
        //    Slider + Label for min Perimeter contour
        //    Slider + Label for max Perimeter contour
        periCB    = new JulipComboBox(PERIMETER_STR, myLinkMap.get("PERIMETER_SELECT"), this);        
        periMinTB = new JulipTrackBar(0, contours.size(), Integer.parseInt(myLinkMap.get("PERIMETER_MIN")), contours.size()/5, -1, this);
        periMaxTB = new JulipTrackBar(0, contours.size(), Integer.parseInt(myLinkMap.get("PERIMETER_MAX")), contours.size()/5, -1, this);
        //
        sliderPanel.add(periCB.comboBox);
        sliderPanel.add(periMinTB.label);            
        sliderPanel.add(periMinTB.slider);            
        sliderPanel.add(periMaxTB.label);            
        sliderPanel.add(periMaxTB.slider);   
        
        // JPanel for fixed Perimeter Contour filters
        //    TextField for min Perimeter
        //    JButton to assert min/max Perimeter filters
        //    TextField for max Perimeter            
        JPanel periFixedPanel = new JPanel();
        periMinTF = new JTextField(myLinkMap.get("PERIMETER_FIXED_MIN"),textWidth);
        periMaxTF = new JTextField(myLinkMap.get("PERIMETER_FIXED_MAX"),textWidth);                    
        periFixedPanel.add(periMinTF);
        JButton buttonPeriFixed = new JButton("< Perimeter Range <");
        buttonPeriFixed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSettings();
                refreshImage();
            }
        });
        periFixedPanel.add(buttonPeriFixed);
        periFixedPanel.add(periMaxTF);
        sliderPanel.add(periFixedPanel);
            
            
        labelContours      = new JLabel();            
        labelDrawnContours = new JLabel();            
        sliderPanel.add(labelContours);
        sliderPanel.add(labelDrawnContours);
        
        //
        // All Link Gui's need to add the JPanel returned from
        // LinkClass buildLinkPanel() method.
        //
        sliderPanel.add(buildLinkPanel());
                        
        // Build frame; the imgLabel is required to be added somewhere to the frame
        frame.add(sliderPanel, BorderLayout.PAGE_START);
        frame.add(imgSP, BorderLayout.PAGE_END);

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
        areaMinTB.setMaximum(contours.size());
        areaMaxTB.setMaximum(contours.size());
        periMinTB.setMaximum(contours.size());
        periMaxTB.setMaximum(contours.size());

        areaMinTB.setValue(0);
        areaMaxTB.setValue(contours.size());
        periMinTB.setValue(0);
        periMaxTB.setValue(contours.size());
        
        areaMinTB.slider.setMajorTickSpacing(contours.size()/5);
        areaMaxTB.slider.setMajorTickSpacing(contours.size()/5);
        periMinTB.slider.setMajorTickSpacing(contours.size()/5);
        periMaxTB.slider.setMajorTickSpacing(contours.size()/5);

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
                        
            // iterate over list of contours and generate a list
            // of areas and a list of perimeters
            contourAreas.clear();
            contourPerimeters.clear();
            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint contour = contours.get(i);
                contourAreas.add(Imgproc.contourArea(contour, false));
                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                contourPerimeters.add(Imgproc.arcLength(contour2f, true));
            }
            Collections.sort(contourAreas);
            Collections.sort(contourPerimeters);                    
            
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
                put("TYPE", "FILTERCONTOURS");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkcontours.txt");
                put("AREA_SELECT", "Area:Slider");
                put("AREA_MIN", "0");
                put("AREA_MAX", "");
                put("AREA_FIXED_MIN", "");
                put("AREA_FIXED_MAX", "");
                put("PERIMETER_SELECT", "Perimeter:Slider");
                put("PERIMETER_MIN", "0");
                put("PERIMETER_MAX", "");
                put("PERIMETER_FIXED_MIN", "");
                put("PERIMETER_FIXED_MAX", "");
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
        intCheck("AREA_MIN", 0, hiLimit, 0);
        intCheck("AREA_MAX", 0, hiLimit, hiLimit);
        intCheck("PERIMETER_MIN", 0, hiLimit, 0);
        intCheck("PERIMETER_MAX", 0, hiLimit, hiLimit);
        //
        // Check for format and range validity for all 4 text fields
        //
        numberOrVoidCheck("AREA_FIXED_MIN");
        numberOrVoidCheck("AREA_FIXED_MAX");
        numberOrVoidCheck("PERIMETER_FIXED_MIN");
        numberOrVoidCheck("PERIMETER_FIXED_MAX");
        //
        // Check for format and range validity for all 2 combo boxes
        //        
        comboCheck("AREA_SELECT", AREA_STR, 0);
        comboCheck("PERIMETER_SELECT", PERIMETER_STR, 0);
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
            writer.write("TYPE\tFILTERCONTOURS\n");
            writer.write("IMAGE_IN\t"    + textFieldImageIn.getText() + "\n");
            writer.write("IMAGE_OUT\t"   + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"   + linkfilename + "\n");
            writer.write("CONTOUR_FILE\t"+ "nope" + "\n");
            writer.write("AREA_SELECT\t" + AREA_STR[areaCB.index] + "\n");
            writer.write("AREA_MIN\t"    + areaMinTB.value + "\n");
            writer.write("AREA_MAX\t"    + areaMaxTB.value + "\n");
            if (!areaMinTF.getText().trim().equals("")) {
                writer.write("AREA_FIXED_MIN\t"      + areaMinTF.getText() + "\n");
            }
            if (!areaMaxTF.getText().trim().equals("")) {
                writer.write("AREA_FIXED_MAX\t"      + areaMaxTF.getText() + "\n");
            }
            writer.write("PERIMETER_SELECT\t"    + PERIMETER_STR[periCB.index] + "\n");
            writer.write("PERIMETER_MIN\t"       + periMinTB.value + "\n");
            writer.write("PERIMETER_MAX\t"       + periMaxTB.value + "\n");
            if (!periMinTF.getText().trim().equals("")) {
                writer.write("PERIMETER_FIXED_MIN\t" + periMinTF.getText() + "\n"); 
            }
            if (!periMaxTF.getText().trim().equals("")) {
                writer.write("PERIMETER_FIXED_MAX\t" + periMaxTF.getText() + "\n"); 
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
        areaMinTB.setValue(Integer.parseInt(myLinkMap.get("AREA_MIN")));
        areaMaxTB.setValue(Integer.parseInt(myLinkMap.get("AREA_MAX")));
        periMinTB.setValue(Integer.parseInt(myLinkMap.get("PERIMETER_MIN")));
        periMaxTB.setValue(Integer.parseInt(myLinkMap.get("PERIMETER_MAX")));
        areaMinTF.setText(myLinkMap.get("AREA_FIXED_MIN"));
        areaMaxTF.setText(myLinkMap.get("AREA_FIXED_MAX"));
        periMinTF.setText(myLinkMap.get("PERIMETER_FIXED_MIN"));
        periMaxTF.setText(myLinkMap.get("PERIMETER_FIXED_MAX"));
        areaCB.setValue(myLinkMap.get("AREA_SELECT"));
        periCB.setValue(myLinkMap.get("PERIMETER_SELECT"));
    }
            
    
    /**
     * refreshSettings - Overrides method in LinkClass; copy and error check myLinkMap settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {    
    
    
        //
        // Update Area Labels
        //
        String lowerMinAreaLimit = "-1";
        String upperMinAreaLimit = "infinite";        
        if (areaMinTB.value < contours.size()) {
            upperMinAreaLimit = String.format("%.1f", contourAreas.get(areaMinTB.value));
        }
        if (areaMinTB.value > 0) {
            lowerMinAreaLimit = String.format("%.1f", contourAreas.get(areaMinTB.value-1));
        }
        areaMinTB.label.setText("("+areaMinTB.value+")  "+ lowerMinAreaLimit + " < Min Area Threshold <= " + upperMinAreaLimit);
        
        String lowerMaxAreaLimit = "-1";
        String upperMaxAreaLimit = "infinite";
        if (areaMaxTB.value < contours.size()) {
            upperMaxAreaLimit = String.format("%.1f", contourAreas.get(areaMaxTB.value));
        }
        if (areaMaxTB.value > 0) {
            lowerMaxAreaLimit = String.format("%.1f", contourAreas.get(areaMaxTB.value-1));
        }
        areaMaxTB.label.setText("("+areaMaxTB.value+")  "+ lowerMaxAreaLimit + " <= Max Area threshold < " + upperMaxAreaLimit);
        
        //
        // Update Perimeter Labels
        //
        String lowerMinPeriLimit = "-1";
        String upperMinPeriLimit = "infinite";        
        if (periMinTB.value < contours.size()) {
            upperMinPeriLimit = String.format("%.1f", contourPerimeters.get(periMinTB.value));
        }
        if (periMinTB.value > 0) {
            lowerMinPeriLimit = String.format("%.1f", contourPerimeters.get(periMinTB.value-1));
        }
        periMinTB.label.setText("("+periMinTB.value+")  "+ lowerMinPeriLimit + " < Min Perimeter Threshold <= " + upperMinPeriLimit);
        
        String lowerMaxPeriLimit = "-1";
        String upperMaxPeriLimit = "infinite";        
        if (periMaxTB.value < contours.size()) {
            upperMaxPeriLimit = String.format("%.1f", contourPerimeters.get(periMaxTB.value));
        }
        if (periMaxTB.value > 0) {
            lowerMaxPeriLimit = String.format("%.1f", contourPerimeters.get(periMaxTB.value-1));
        }
        periMaxTB.label.setText("("+periMaxTB.value+")  "+ lowerMaxPeriLimit + " <= Max Perimeter threshold < " + upperMaxPeriLimit);
        
        labelContours.setText("Number of initial contours: " + contours.size()); 
        
        return true;
    }
    
    /**
     * refreshImage - refresh label texts and shown image
     */
    public void refreshImage() {
        Mat hierarchy = new Mat();
        matImgDst = Mat.zeros(matImgSrc.size(), CvType.CV_8UC3);
        Scalar color = new Scalar(0,255,255);      
        
        calculateAreaPerimeterThresholds();
        
        drawnContourIndices.clear();
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i),false);
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            double perimeter = Imgproc.arcLength(contour2f,true);

            if (( area > minArea ) &&
                (( maxArea < 0) || ( area < maxArea)) &&
                ( perimeter > minPerimeter) &&
                (( maxPerimeter < 0) || ( perimeter < maxPerimeter))
            ) {            
                drawnContourIndices.add(i);
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
    
            StringBuilder sb = new StringBuilder();
            sb.append("    public List<MatOfPoint> doLinkFilterContours");
            if (!reference.equals("")) { sb.append("    _"+reference); }            
            sb.append("(List<MatOfPoint> contours) {\n");
            
            sb.append("        List<MatOfPoint> filteredContours = new ArrayList<>();\n");
            sb.append("        double area;\n");
            sb.append("        double perimeter;\n");
            sb.append("        MatOfPoint2f contour2f;\n");
            sb.append("        for (int i = 0; i < contours.size(); i++) {\n");
            sb.append("            // calculate area and perimeter of each contour\n");
            sb.append("            area = Imgproc.contourArea(contours.get(i),false);\n");
            sb.append("            contour2f = new MatOfPoint2f(contours.get(i).toArray());\n");
            sb.append("            perimeter = Imgproc.arcLength(contour2f,true);\n");
            sb.append("            // only add contours within desired area and perimeter to list of filtered contours\n");
            sb.append("            if (( area > "+minArea+" ) &&\n");
            if (maxArea < 0) {
                sb.append("                (area < Double.POSITIVE_INFINITY) &&\n");
            } else {
                sb.append("                (area < "+maxArea+") &&\n");
            }
            sb.append("                ( perimeter > "+minPerimeter+") &&\n");
            if (maxPerimeter < 0) {
                sb.append("                (perimeter < Double.POSITIVE_INFINITY)) {\n");
            } else {
                sb.append("                (perimeter < "+maxPerimeter+")) {\n");
            }
            sb.append("                filteredContours.add(contours.get(i));\n");
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
     * calculateAreaPerimeterThresholds - utility method to determin min,max Area and Perimeter for contour filtering.
     */
    public void calculateAreaPerimeterThresholds() {
        minArea = -1;
        maxArea = -1;
        if (areaCB.index == 0) {
            if (areaMinTB.value > 0) {
                minArea = contourAreas.get(areaMinTB.value-1);
            }            
            if (areaMaxTB.value != contours.size()) {
                maxArea = contourAreas.get(areaMaxTB.value);
            }
        } else {
            if (areaMinTF.getText().equals("")) {
                minArea = -1;
            } else {
                minArea = Double.parseDouble(areaMinTF.getText());                
            }
            if (areaMaxTF.getText().equals("")) {
                maxArea = -1;
            } else {
                maxArea = Double.parseDouble(areaMaxTF.getText());                
            }
        }
        
        minPerimeter = -1;
        maxPerimeter = -1;
        if (periCB.index == 0) {
            if (periMinTB.value > 0) {
                minPerimeter = contourPerimeters.get(periMinTB.value-1);
            }            
            if (periMaxTB.value != contours.size()) {
                maxPerimeter = contourPerimeters.get(periMaxTB.value);
            }
        } else {
            if (periMinTF.getText().equals("")) {
                minPerimeter = -1;
            } else {
                minPerimeter = Double.parseDouble(periMinTF.getText());                
            }
            if (periMaxTF.getText().equals("")) {
                maxPerimeter = -1;
            } else {
                maxPerimeter = Double.parseDouble(periMaxTF.getText());                
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
                new LinkFilterContours(args);
            }
        });
    }
}
