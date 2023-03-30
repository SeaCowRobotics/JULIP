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
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkErodilate - Graphical user interface to view pixel values.
 */
public class LinkToSpreadsheet extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //
    private final String[] COLORSPACE_STR = { 
        "Color Space: BGR", 
        "Color Space: HSV",
        "Color Space: YCrCb",
        "Color Space: Gray"
    };
       
    private JulipTrackBar xminTB;
    private JulipTrackBar xmaxTB;
    private JulipTrackBar xposTB;
    private JulipTrackBar yminTB;
    private JulipTrackBar ymaxTB;
    private JulipTrackBar yposTB;
    private JulipComboBox colorSpaceCB;
    
    private int wasXmin, wasXmax, wasXpos;
    private int wasYmin, wasYmax, wasYpos;
    
    private Mat matPixel;
    
    private JLabel imgLabel;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private final int colorSpaceIdxDefault = 0;
    //
    //------------------------------------------------
    
    public LinkToSpreadsheet(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkToSpreadsheet.java";
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
        boolean firstLoadOK = loadImage(myLinkMap.get("IMAGE_IN"));
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

        int cols = 5*((int)Math.ceil((float)matImgSrc.cols()/5.0f));
        int rows = 5*((int)Math.ceil((float)matImgSrc.rows()/5.0f));
        if (firstLoadOK) {
            xminTB = new JulipTrackBar(0, cols, Integer.parseInt(myLinkMap.get("XMIN")), cols/5, -1, this);
            xmaxTB = new JulipTrackBar(0, cols, Integer.parseInt(myLinkMap.get("XMAX")), cols/5, -1, this);
            xposTB = new JulipTrackBar(0, cols, Integer.parseInt(myLinkMap.get("XPOS")), cols/5, -1, this);
            yminTB = new JulipTrackBar(0, rows, Integer.parseInt(myLinkMap.get("YMIN")), rows/5, -1, this);
            ymaxTB = new JulipTrackBar(0, rows, Integer.parseInt(myLinkMap.get("YMAX")), rows/5, -1, this);
            yposTB = new JulipTrackBar(0, rows, Integer.parseInt(myLinkMap.get("YPOS")), rows/5, -1, this);
        } else {
            xminTB = new JulipTrackBar(0, cols, 0,                cols/5, -1, this);
            xmaxTB = new JulipTrackBar(0, cols, matImgSrc.cols(), cols/5, -1, this);
            xposTB = new JulipTrackBar(0, cols, 0,                cols/5, -1, this);
            yminTB = new JulipTrackBar(0, rows, 0,                rows/5, -1, this);
            ymaxTB = new JulipTrackBar(0, rows, matImgSrc.rows(), rows/5, -1, this);
            yposTB = new JulipTrackBar(0, rows, 0,                rows/5, -1, this);
        }
        colorSpaceCB = new JulipComboBox(COLORSPACE_STR, myLinkMap.get("COLORSPACE_TYPE"), this);    
        
        //
        // JPanel to hold:
        //      region-of-interest JPanel
        //      array of pixel labels JPanel
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        // JPanel to hold:
        //      xmin   JLabel + JSlider
        //      xmax   JLabel + JSlider
        //      xpos   JLabel + JSlider
        //      ymin   JLabel + JSlider
        //      ymax   JLabel + JSlider
        //      ypos   JLabel + JSlider
        //      color space JComboBox
        sliderPanel.add(xminTB.slider);
        sliderPanel.add(xminTB.label);
        sliderPanel.add(xmaxTB.slider);
        sliderPanel.add(xmaxTB.label);
        sliderPanel.add(xposTB.slider);
        sliderPanel.add(xposTB.label);
        sliderPanel.add(new JLabel("  "));
        sliderPanel.add(new JLabel("  "));
        sliderPanel.add(yminTB.slider);
        sliderPanel.add(yminTB.label);
        sliderPanel.add(ymaxTB.slider);
        sliderPanel.add(ymaxTB.label);
        sliderPanel.add(yposTB.slider);
        sliderPanel.add(yposTB.label);
        sliderPanel.add(new JLabel("  "));
        sliderPanel.add(new JLabel("  "));
        sliderPanel.add(colorSpaceCB.comboBox);
                
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
    
        //
        // The absolute vale of the channels param indicates the 
        // number of channels for the Mat.
        //
        matImgSrc = Imgcodecs.imread(filename);
        System.out.println("loadimage:"+filename);
        if (matImgSrc.empty()) {
            System.out.println("No image file:" + filename);
            matImgSrc = Mat.zeros(new Size(512, 512), CvType.CV_8UC3);
            return false;
        }
        //
        // Reconfigure the sliders to span the new image size
        //
        if ((xminTB != null) && (yminTB != null)) {
            xminTB.setMaximum(matImgSrc.cols());
            xmaxTB.setMaximum(matImgSrc.cols());
            xposTB.setMaximum(matImgSrc.cols());
            yminTB.setMaximum(matImgSrc.rows());
            ymaxTB.setMaximum(matImgSrc.rows());
            yposTB.setMaximum(matImgSrc.rows());

            xminTB.setValue(0);
            xmaxTB.setValue(matImgSrc.cols());
            xposTB.setValue(0);
            yminTB.setValue(0);
            ymaxTB.setValue(matImgSrc.rows());
            yposTB.setValue(0);
        
            int cols = 5*((int)Math.ceil((float)matImgSrc.cols()/5.0f));
            int rows = 5*((int)Math.ceil((float)matImgSrc.rows()/5.0f));
        
            xminTB.slider.setMajorTickSpacing(cols/5);
            xmaxTB.slider.setMajorTickSpacing(cols/5);
            xposTB.slider.setMajorTickSpacing(cols/5);
            yminTB.slider.setMajorTickSpacing(rows/5);
            ymaxTB.slider.setMajorTickSpacing(rows/5);
            yposTB.slider.setMajorTickSpacing(rows/5);
        }

        return true;
    }
    
        
    //-----------------------------------------------------------------------------------
    //
    // Cluster of overriden LinkClass methods 
    //   buildMyLinkMap()
    //   verifySettings()
    //   saveSettings()
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
                put("TYPE", "VIEWER");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.csv");
                put("LINK_FILE", "nolinktospreadsheet.txt");
                put("XMIN", "0");
                put("XMAX", "0");
                put("XPOS", "0");
                put("YMIN", "0");
                put("YMAX", "0");
                put("YPOS", "0");
                put("COLORSPACE_TYPE", COLORSPACE_STR[colorSpaceIdxDefault]);
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
        // Check for format and range validity for all 1 track bars
        //                
        intCheck("XMIN", 0, Integer.MAX_VALUE, 0);
        intCheck("XMAX", 0, Integer.MAX_VALUE, 0);
        intCheck("XPOS", 0, Integer.MAX_VALUE, 0);
        intCheck("YMIN", 0, Integer.MAX_VALUE, 0);
        intCheck("YMAX", 0, Integer.MAX_VALUE, 0);
        intCheck("YPOS", 0, Integer.MAX_VALUE, 0);
        
        //
        // Check for format and range validity for all 1 combo boxes
        //        
        comboCheck("COLORSPACE_TYPE", COLORSPACE_STR, colorSpaceIdxDefault);
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
            writer.write("TYPE\tVIEWER\n");
            writer.write("IMAGE_IN\t"        + textFieldImageIn.getText()  + "\n");
            writer.write("IMAGE_OUT\t"       + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"       + linkfilename + "\n");
            writer.write("XMIN\t"            + xminTB.value + "\n");
            writer.write("XMAX\t"            + xmaxTB.value + "\n");
            writer.write("XPOS\t"            + xposTB.value + "\n");
            writer.write("YMIN\t"            + yminTB.value + "\n");
            writer.write("YMAX\t"            + ymaxTB.value + "\n");
            writer.write("YPOS\t"            + yposTB.value + "\n");
            writer.write("COLOR_SPACE_TYPE\t"+ COLORSPACE_STR[colorSpaceCB.index] + "\n");
            writer.close();
        } catch (IOException e) {}
    }
    
    /**
     * saveImage - write output Image to file 
     */
    @Override
    public void saveImage() {
System.out.println(yminTB.value);    
System.out.println(ymaxTB.value);    
System.out.println(xminTB.value);    
System.out.println(xmaxTB.value);    
        Mat outMat = matPixel.submat(
            yminTB.value,   // int rowStart
            ymaxTB.value,   // int rowEnd
            xminTB.value,   // int colStart
            xmaxTB.value    // int colEnd
        );
        MatHandler.writeMatToCSV(
            outMat,                      // Mat mat
            xminTB.value,                // int colOffset
            yminTB.value,                // int rowOffset
            textFieldImageOut.getText()  // String filename
        );
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        xminTB.setValue(Integer.parseInt(myLinkMap.get("XMIN")));
        xmaxTB.setValue(Integer.parseInt(myLinkMap.get("XMAX")));
        xposTB.setValue(Integer.parseInt(myLinkMap.get("XPOS")));
        yminTB.setValue(Integer.parseInt(myLinkMap.get("YMIN")));
        ymaxTB.setValue(Integer.parseInt(myLinkMap.get("YMAX")));
        yposTB.setValue(Integer.parseInt(myLinkMap.get("YPOS")));
        colorSpaceCB.setValue(myLinkMap.get("COLORSPACE_TYPE"));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; update gui settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
    
        if (xminTB.value != wasXmin) {
            if (xminTB.value > xmaxTB.value) {
                xmaxTB.setValueSilently(xminTB.value);
            }
            xposTB.setValueSilently(xminTB.value);
        }        
        else if (xmaxTB.value != wasXmax) {
            if (xmaxTB.value < xminTB.value) {
                xminTB.setValueSilently(xmaxTB.value);
                xposTB.setValueSilently(xmaxTB.value);
            }
        }
        else if (xposTB.value != wasXpos) {
            xmaxTB.setValueSilently(Math.min(xposTB.value + xmaxTB.value - xminTB.value, xmaxTB.slider.getMaximum()));
            xminTB.setValueSilently(xposTB.value);
        }
        
        if (yminTB.value != wasYmin) {
            if (yminTB.value > ymaxTB.value) {
                ymaxTB.setValueSilently(yminTB.value);
             }
             yposTB.setValueSilently(yminTB.value);
        }        
        else if (ymaxTB.value != wasYmax) {
            if (ymaxTB.value < yminTB.value) {
                yminTB.setValueSilently(ymaxTB.value);
                yposTB.setValueSilently(ymaxTB.value);
            }
        }
        else if (yposTB.value != wasYpos) {
            ymaxTB.setValueSilently(Math.min(yposTB.value + ymaxTB.value - yminTB.value, ymaxTB.slider.getMaximum()));
            yminTB.setValueSilently(yposTB.value);
        }
        
        xminTB.label.setText("X Min: " + xminTB.value);
        xmaxTB.label.setText("X Max: " + xmaxTB.value);
        xposTB.label.setText("X Pos: " + xposTB.value);
        yminTB.label.setText("Y Min: " + xminTB.value);
        ymaxTB.label.setText("Y Max: " + xmaxTB.value);
        yposTB.label.setText("Y Pos: " + xposTB.value);
        
        wasXmin = xminTB.value;
        wasXmax = xmaxTB.value;
        wasXpos = xposTB.value;
        wasYmin = yminTB.value;
        wasYmax = ymaxTB.value;
        wasYpos = yposTB.value;
        
        return true;
    }
    
    /**
     * refreshImage - Overrides method in LinkClass; recalculate algorithm and refresh shown image.
     */
    public void refreshImage() {
        matImgDst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
        matPixel  = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
       
        // Present the image in any 3-channel color space as BGR
        // And present in Gray as grayscale
        if (colorSpaceCB.index == 3) {
            Imgproc.cvtColor(matImgSrc, matImgDst, Imgproc.COLOR_BGR2GRAY);
        }
        else {
            matImgSrc.copyTo(matImgDst);
        }
        
        switch (colorSpaceCB.index) {
            case 0:
                matImgSrc.copyTo(matPixel);
                break;
            case 1:
                Imgproc.cvtColor(matImgSrc, matPixel, Imgproc.COLOR_BGR2HSV);
                break;
            case 2:
                Imgproc.cvtColor(matImgSrc, matPixel, Imgproc.COLOR_BGR2YCrCb);
                break;
            case 3:
                Imgproc.cvtColor(matImgSrc, matPixel, Imgproc.COLOR_BGR2GRAY);
                break;
        }    
        
        Imgproc.rectangle (
            matImgDst,
            new Point (xminTB.value, yminTB.value),
            new Point (xmaxTB.value, ymaxTB.value),
            new Scalar (255, 255, 255),
            1
        );
        Imgproc.rectangle (
            matImgDst,
            new Point (xminTB.value-1, yminTB.value-1),
            new Point (xmaxTB.value+1, ymaxTB.value+1),
            new Scalar (0, 0, 0),
            1
        );
                
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
        return importsL;
    }     
    
    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {
    
            StringBuilder sb = new StringBuilder();
            sb.append("    public void doLinkViewer");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("() {\n");
            
            sb.append("        // Meh\n");
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
        System.out.println("args:");
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
        }
        // Load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LinkToSpreadsheet(args);
            }
        });        
    }
}
