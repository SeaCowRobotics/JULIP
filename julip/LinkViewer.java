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
public class LinkViewer extends LinkClass {

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
       
    private JLabel[][] pixelValueLabels;
    
    private JulipTrackBar xpixelTB;
    private JulipTrackBar ypixelTB;
    private JulipTrackBar sizeTB;
    private JulipComboBox colorSpaceCB;
    
    private Mat matPixel;
    
    private JLabel imgLabel;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private final int colorSpaceIdxDefault = 0;
    //
    //------------------------------------------------
    
    public LinkViewer(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkViewer.java";
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

        xpixelTB = new JulipTrackBar(0, matImgSrc.cols(), Integer.parseInt(myLinkMap.get("XPIXEL")), matImgSrc.cols()/5, -1, this);
        ypixelTB = new JulipTrackBar(0, matImgSrc.rows(), Integer.parseInt(myLinkMap.get("YPIXEL")), matImgSrc.rows()/5, -1, this);
        sizeTB   = new JulipTrackBar(1, 7, 4, 1, 1, this);
        colorSpaceCB = new JulipComboBox(COLORSPACE_STR, myLinkMap.get("COLORSPACE_TYPE"), this);    
        pixelValueLabels = new JLabel[8][8];
        
        //
        // JPanel to hold:
        //      region-of-interest JPanel
        //      array of pixel labels JPanel
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        // JPanel to hold:
        //      xpixel JLabel + JSlider
        //      ypixel JLabel + JSlider
        //      size   JLabel + JSlider
        //      color space JComboBox
//        JPanel roiP = new JPanel();
//        roiP.setLayout(new BoxLayout(roiP, BoxLayout.PAGE_AXIS));
        sliderPanel.add(xpixelTB.slider);
        sliderPanel.add(xpixelTB.label);
        sliderPanel.add(ypixelTB.slider);
        sliderPanel.add(ypixelTB.label);
//        sliderPanel.add(sizeTB.slider);
//        sliderPanel.add(sizeTB.label);
        sliderPanel.add(colorSpaceCB.comboBox);
        
        // JPanel to hold:
        //      array of pixel JlLabels
        JPanel pixelP = new JPanel();
        pixelP.setLayout(new GridLayout(8,8));
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                pixelValueLabels[x][y] = new JLabel(" . ");
                if (x == 0) {
                    pixelValueLabels[x][y].setPreferredSize(new Dimension(20, 20));
                }
                else {
                    pixelValueLabels[x][y].setPreferredSize(new Dimension(95, 20));
                }
                pixelP.add(pixelValueLabels[x][y]);
            }            
        }
//        JPanel userP = new JPanel();
//        userP.add(roiP, BorderLayout.LINE_START);
//        userP.add(pixelP, BorderLayout.LINE_END);
        
        sliderPanel.add(pixelP);
        
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
            matImgSrc = Mat.zeros(new Size(512, 512), CvType.CV_8U);
            return false;
        }
        //
        // Reconfigure the sliders to span the new image size
        //
        if ((xpixelTB != null) && (ypixelTB != null)) {
            xpixelTB.setMaximum(matImgSrc.cols());
            ypixelTB.setMaximum(matImgSrc.rows());

            xpixelTB.setValue(0);
            ypixelTB.setValue(0);
        
            xpixelTB.slider.setMajorTickSpacing(matImgSrc.cols()/5);
            ypixelTB.slider.setMajorTickSpacing(matImgSrc.rows()/5);
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
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkviewer.txt");
                put("XPIXEL", "0");
                put("YPIXEL", "0");
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
        intCheck("XPIXEL", 0, Integer.MAX_VALUE, 0);
        intCheck("YPIXEL", 0, Integer.MAX_VALUE, 0);
        
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
            writer.write("XPIXEL\t"          + xpixelTB.value + "\n");
            writer.write("YPIXEL\t"          + ypixelTB.value + "\n");
            writer.write("COLOR_SPACE_TYPE\t"+ COLORSPACE_STR[colorSpaceCB.index] + "\n");
            writer.close();
        } catch (IOException e) {}
    }
    
    /**
     * saveImage - write output Image to file 
     */
    @Override
    public void saveImage() {
        Imgcodecs.imwrite(textFieldImageOut.getText(), matImgDst);
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        xpixelTB.setValue(Integer.parseInt(myLinkMap.get("XPIXEL")));
        ypixelTB.setValue(Integer.parseInt(myLinkMap.get("YPIXEL")));
        colorSpaceCB.setValue(myLinkMap.get("COLORSPACE_TYPE"));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; update gui settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        xpixelTB.label.setText("X Pixel: " + xpixelTB.value);
        ypixelTB.label.setText("Y Pixel: " + ypixelTB.value);
        sizeTB.label.setText("Region Radius");
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
            new Point (xpixelTB.value-4, ypixelTB.value-4),
            new Point (xpixelTB.value+4, ypixelTB.value+4),
            new Scalar (255, 255, 255),
            1
        );
        Imgproc.rectangle (
            matImgDst,
            new Point (xpixelTB.value-5, ypixelTB.value-5),
            new Point (xpixelTB.value+5, ypixelTB.value+5),
            new Scalar (0, 0, 0),
            1
        );
        
        byte[] pixel = new byte[3];
        
//        int length = sizeTB.value * 2 - 1;
        
        // Update pixel value labels
        for (int ix = 0; ix < 8; ix++) {
            for (int iy = 0; iy < 8; iy++) {
                int x = xpixelTB.value-4+ix;
                int y = ypixelTB.value-4+iy;
                if ((ix == 0) && (iy == 0)) {
                    pixelValueLabels[ix][iy].setText("   ");
                }
                else if ((ix == 0) && (iy != 0)) {
                    pixelValueLabels[ix][iy].setText(Integer.toString(y));
                }
                else if ((ix != 0) && (iy == 0)) {
                    pixelValueLabels[ix][iy].setText(Integer.toString(x));
                }
                else if ((x < 0) || (y < 0) ||
                    (x >= matImgSrc.cols()) ||
                    (y >= matImgSrc.rows())) {
                    pixelValueLabels[ix][iy].setText("-,-,-");
                }
                else { 
                    matPixel.get(y, x, pixel);
                    pixelValueLabels[ix][iy].setText(Integer.toString((int)(pixel[0]&0xff))+","+Integer.toString((int)(pixel[1]&0xff))+","+Integer.toString((int)(pixel[2]&0xff)));
                }
            }
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
        importsL.add("org.opencv.core.Core");
        importsL.add("org.opencv.core.CvType");
        importsL.add("org.opencv.core.Mat");
        importsL.add("org.opencv.core.Size");
        importsL.add("org.opencv.imgproc.Imgproc");
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
                new LinkViewer(args);
            }
        });        
    }
}
