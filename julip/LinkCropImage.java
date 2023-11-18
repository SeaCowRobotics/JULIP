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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkCropImage - Graphical user interface to blank out some region of the image.
 */
public class LinkCropImage extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private JulipTrackBar cropLeftTB;    
    private JulipTrackBar cropRightTB;    
    private JulipTrackBar cropTopTB;    
    private JulipTrackBar cropBottomTB;    
    
    private int cropLeftDefault = 0;
    private int cropRightDefault = -1;
    private int cropTopDefault = 0;
    private int cropBottomDefault = -1;
    //
    //------------------------------------------------
    
    public LinkCropImage(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkCropImage.java";   
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
                        
        cropLeftTB = new JulipTrackBar(0, matImgSrc.cols(), Integer.parseInt(myLinkMap.get("CROP_LEFT")), 30, 6, this);
        cropRightTB = new JulipTrackBar(0, matImgSrc.cols(), Integer.parseInt(myLinkMap.get("CROP_RIGHT")), 30, 6, this);
        cropTopTB = new JulipTrackBar(0, matImgSrc.rows(), Integer.parseInt(myLinkMap.get("CROP_TOP")), 30, 6, this);
        cropBottomTB = new JulipTrackBar(0, matImgSrc.rows(), Integer.parseInt(myLinkMap.get("CROP_BOTTOM")), 30, 6, this);
    

        //
        // JPanel to hold:
        //      Crop Left  JLabel + JSlider
        //      Crop Right JLabel + JSlider
        //      Crop Up    JLabel + JSlider
        //      Crop Down  JLabel + JSlider
        //      link JPanel
        //
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        sliderPanel.add(cropLeftTB.label);
        sliderPanel.add(cropLeftTB.slider);
        sliderPanel.add(cropRightTB.label);
        sliderPanel.add(cropRightTB.slider);
        sliderPanel.add(cropTopTB.label);
        sliderPanel.add(cropTopTB.slider);
        sliderPanel.add(cropBottomTB.label);
        sliderPanel.add(cropBottomTB.slider);
        
        //
        // All Link Gui's need to add the JPanel returned from
        // LinkClass buildLinkPanel() method.
        //
        sliderPanel.add(buildLinkPanel());
        
        // Build frame; the imgLabel is required to be added somewhere to the frame
        frame.add(sliderPanel, BorderLayout.CENTER);        
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
                put("TYPE", "INRANGEHSV");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkinrange.txt");
                put("CROP_LEFT",  Integer.toString(cropLeftDefault));
                put("CROP_RIGHT", Integer.toString(cropRightDefault));
                put("CROP_TOP",    Integer.toString(cropTopDefault));
                put("CROP_BOTTOM",  Integer.toString(cropBottomDefault));
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
        // Check for format and range validity for all 8 track bars
        //                
        intCheck("CROP_LEFT", 0, 10000, cropLeftDefault);
        intCheck("CROP_TOP", 0, 10000, cropTopDefault);
        intCheck("CROP_RIGHT", 0, 10000, matImgSrc.cols());
        intCheck("CROP_BOTTOM", 0, 10000, matImgSrc.rows());
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
            writer.write("TYPE\tINRANGEHSV\n");
            writer.write("IMAGE_IN\t"   + textFieldImageIn.getText()  + "\n");
            writer.write("IMAGE_OUT\t"  + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"  + linkfilename + "\n");
            writer.write("CROP_LEFT\t"  + cropLeftTB.value + "\n");
            writer.write("CROP_RIGHT\t" + cropRightTB.value + "\n");
            writer.write("CROP_TOP\t"    + cropTopTB.value + "\n");
            writer.write("CROP_BOTTOM\t"  + cropBottomTB.value + "\n");
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
        cropLeftTB.setValue(Integer.parseInt(myLinkMap.get("HUE1_MIN")));
        cropRightTB.setValue(Integer.parseInt(myLinkMap.get("HUE1_MAX")));
        cropTopTB.setValue(Integer.parseInt(myLinkMap.get("HUE2_MIN")));
        cropBottomTB.setValue(Integer.parseInt(myLinkMap.get("HUE2_MAX")));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; update gui settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        cropLeftTB.label.setText("Crop Left: " + cropLeftTB.value);
        cropRightTB.label.setText("Crop Right: " + cropRightTB.value);
        cropTopTB.label.setText("Crop Top: " + cropTopTB.value);
        cropBottomTB.label.setText("Crop Bottom: " + cropBottomTB.value);
        return true;
    }
    
    /**
     * refreshImage - Overrides method in LinkClass; recalculate algorithm and refresh shown image.
     */
    @Override
    public void refreshImage() {
        int mark;
        Scalar blank = new Scalar(0,0,0);

        matImgDst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
        matImgSrc.copyTo(matImgDst);

        mark = cropLeftTB.value;
        if (mark > 0) {
            Mat leftMat = matImgDst.submat(new Rect(0,0, mark, matImgSrc.rows()));
            leftMat.setTo(blank);    
        }
        mark = cropRightTB.value;
        if (mark < matImgSrc.cols()) {
            Mat rightMat = matImgDst.submat(new Rect(mark, 0, matImgSrc.cols()-mark-1, matImgSrc.rows()));
            rightMat.setTo(blank);
        }
        mark = cropTopTB.value;
        if (mark > 0) {
            Mat topMat = matImgDst.submat(new Rect(0, 0, matImgSrc.cols(), mark));
            topMat.setTo(blank);
        }
        mark = cropBottomTB.value;
        if (mark < matImgSrc.rows()) {
            Mat bottomMat = matImgDst.submat(new Rect(0, mark, matImgSrc.cols(), matImgSrc.rows()-mark-1));
            bottomMat.setTo(blank);
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
        importsL.add("org.opencv.core.Rect");
        importsL.add("org.opencv.core.Scalar");
        return importsL;
    }     
        
    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {
    
            StringBuilder sb = new StringBuilder();
            sb.append("    public Mat doLinkInRangeHHSV");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("(Mat matImgSrc) {\n");
    
            sb.append("        int mark;\n");
            sb.append("        Scalar blank = new Scalar(0,0,0);\n");
            sb.append("        Mat matImgDst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);\n");
            sb.append("        matImgSrc.copyTo(matImgDst);\n");
            sb.append("        mark = "+cropLeftTB.value+";\n");
            sb.append("        if (mark > 0) {\n");
            sb.append("            Mat leftMat = matImgDst.submat(new Rect(0,0, mark, matImgSrc.rows()));\n");
            sb.append("            leftMat.setTo(blank);\n");
            sb.append("        }\n");
            sb.append("        mark = "+cropRightTB.value+";\n");
            sb.append("        if (mark < matImgSrc.cols()) {\n");
            sb.append("            Mat rightMat = matImgDst.submat(new Rect(mark, 0, matImgSrc.cols()-mark-1, matImgSrc.rows()));\n");
            sb.append("            rightMat.setTo(blank);\n");
            sb.append("        }\n");
            sb.append("        mark = "+cropTopTB.value+";\n");
            sb.append("        if (mark > 0) {\n");
            sb.append("            Mat topMat = matImgDst.submat(new Rect(0, 0, matImgSrc.cols(), mark));\n");
            sb.append("            topMat.setTo(blank);\n");
            sb.append("        }\n");
            sb.append("        mark = "+cropBottomTB.value+";\n");
            sb.append("        if (mark < matImgSrc.rows()) {\n");
            sb.append("            Mat bottomMat = matImgDst.submat(new Rect(0, mark, matImgSrc.cols(), matImgSrc.rows()-mark-1));\n");
            sb.append("            bottomMat.setTo(blank);\n");
            sb.append("        }\n");    
            sb.append("        return matImgDst;\n");
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
                new LinkCropImage(args);
            }
        });        
    }
}
