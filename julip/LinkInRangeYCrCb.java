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
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkInRangeHSV - Graphical user interface to exercise openCV Core.inRange method in YCrCb color space.
 */
public class LinkInRangeYCrCb extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //
    private final String[] IMAGE_TYPE = { 
        "View:BGR  Output:BGR",
        "View:BGR  Output:YCrCb", 
        "View:mask Ouptut:Binary"
    };
    
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private JulipTrackBar yMinTB;    
    private JulipTrackBar yMaxTB;    
    private JulipTrackBar crMinTB;    
    private JulipTrackBar crMaxTB;    
    private JulipTrackBar cbMinTB;    
    private JulipTrackBar cbMaxTB;    
    private JulipComboBox imageCB;
    
    private final int yMinDefault = 0;
    private final int yMaxDefault = 255;
    private final int crMinDefault = 0;
    private final int crMaxDefault = 255;
    private final int cbMinDefault = 0;
    private final int cbMaxDefault = 255;
    private final int imageIdxDefault = 1;
    
    private Mat matYCrCb;
    
    //
    //------------------------------------------------
    
    public LinkInRangeYCrCb(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkInRangeYCrCb.java";
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
                        
        yMinTB = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("Y_MIN")), 50, 10, this);
        yMaxTB = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("Y_MAX")), 50, 10, this);
        crMinTB = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("CR_MIN")), 50, 10, this);
        crMaxTB = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("CR_MAX")), 50, 10, this);
        cbMinTB = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("CB_MIN")), 50, 10, this);
        cbMaxTB = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("CB_MAX")), 50, 10, this);
        imageCB  = new JulipComboBox(IMAGE_TYPE, myLinkMap.get("IMAGE_TYPE"), this);

        //
        // JPanel to hold:
        //      Y min JLabel + JSlider
        //      Y max JLabel + JSlider
        //      Cr min JLabel + JSlider
        //      Cr max JLabel + JSlider
        //      Cb min JLabel + JSlider
        //      Cb max JLabel + JSlider
        //      link JPanel
        //
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        sliderPanel.add(yMinTB.label);
        sliderPanel.add(yMinTB.slider);
        sliderPanel.add(yMaxTB.label);
        sliderPanel.add(yMaxTB.slider);
        sliderPanel.add(crMinTB.label);
        sliderPanel.add(crMinTB.slider);
        sliderPanel.add(crMaxTB.label);
        sliderPanel.add(crMaxTB.slider);
        sliderPanel.add(cbMinTB.label);
        sliderPanel.add(cbMinTB.slider);
        sliderPanel.add(cbMaxTB.label);
        sliderPanel.add(cbMaxTB.slider);
        sliderPanel.add(imageCB.comboBox);
        
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
                put("TYPE", "INRANGEYCRCB");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("MASK_OUT", "nomask.png");
                put("LINK_FILE", "nolinkinrange.txt");
                put("Y_MIN", Integer.toString(yMinDefault));
                put("Y_MAX", Integer.toString(yMaxDefault));
                put("CR_MIN", Integer.toString(crMinDefault));
                put("CR_MAX", Integer.toString(crMaxDefault));
                put("CB_MIN", Integer.toString(cbMinDefault));
                put("CB_MAX", Integer.toString(cbMaxDefault));
                put("IMAGE_TYPE", IMAGE_TYPE[imageIdxDefault]);
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
        // Check for format and range validity for all 6 track bars
        //                
        intCheck("Y_MIN", 0, 255, yMinDefault);
        intCheck("Y_MAX", 0, 255, yMaxDefault);
        intCheck("CR_MIN", 0, 255, crMinDefault);
        intCheck("CR_MAX", 0, 255, crMaxDefault);
        intCheck("CB_MIN", 0, 255, cbMinDefault);
        intCheck("CB_MAX", 0, 255, cbMaxDefault);
        
        //
        // Check for format and range validity for all 1 combo boxes
        //        
        comboCheck("IMAGE_TYPE", IMAGE_TYPE, imageIdxDefault);
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
            writer.write("Y_MIN\t"      + yMinTB.value + "\n");
            writer.write("Y_MAX\t"      + yMaxTB.value + "\n");
            writer.write("CR_MIN\t"     + crMinTB.value + "\n");
            writer.write("CR_MAX\t"     + crMaxTB.value + "\n");
            writer.write("CB_MIN\t"     + cbMinTB.value + "\n");
            writer.write("CB_MAX\t"     + cbMaxTB.value + "\n");
            writer.write("IMAGE_TYPE\t" + IMAGE_TYPE[imageCB.index] + "\n");
            writer.close();
        } catch (IOException e) {}
    }
    
    /**
     * saveImage - write output Image to file 
     */
    @Override
    public void saveImage() {
        // To save YCrCb data, cannot use imwrite, so use custom Mat exporter
        if (imageCB.index == 1) {
            MatHandler.writeMat(matYCrCb, textFieldImageOut.getText());
        }
        // BGR and binary can use Imgcodecs.imwrite
        else {
            Imgcodecs.imwrite(textFieldImageOut.getText(), matImgDst);
        }
        
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        yMinTB.setValue(Integer.parseInt(myLinkMap.get("Y_MIN")));
        yMaxTB.setValue(Integer.parseInt(myLinkMap.get("Y_MAX")));
        crMinTB.setValue(Integer.parseInt(myLinkMap.get("CR_MIN")));
        crMaxTB.setValue(Integer.parseInt(myLinkMap.get("CR_MAX")));
        cbMinTB.setValue(Integer.parseInt(myLinkMap.get("CB_MIN")));
        cbMaxTB.setValue(Integer.parseInt(myLinkMap.get("CB_MAX")));
        imageCB.setValue(myLinkMap.get("IMAGE_TYPE"));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; update gui settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        yMinTB.label.setText("Y Minimum: " + yMinTB.value);
        yMaxTB.label.setText("Y Maximum: " + yMaxTB.value);
        crMinTB.label.setText("Cr Minimum: " + crMinTB.value);
        crMaxTB.label.setText("Cr Maximum: " + crMaxTB.value);
        cbMinTB.label.setText("Cb Minimum: " + cbMinTB.value);
        cbMaxTB.label.setText("Cb Maximum: " + cbMaxTB.value);
        
        // The file extension of the output file must match combobox output type
        //
        // split output filename into base and extension
        String[] tokens =  textFieldImageOut.getText().split("\\.(?=[^\\.]+$)");
        if (imageCB.index == 1) { tokens[1] = ".mat"; }
        else                    { tokens[1] = ".png"; }
        textFieldImageOut.setText(tokens[0]+tokens[1]);
        
        return true;
    }
    
    /**
     * refreshImage - Overrides method in LinkClass; recalculate algorithm and refresh shown image.
     */
    public void refreshImage() {
        matYCrCb = new Mat();
        Mat dst = new Mat();
        Mat msk = new Mat();
        //matImgDst = new Mat();
        matImgDst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
        // If a 3-channel Mat is showing, then process the image
        if (matImgSrc.channels() == 3) {
            // Convert the file source image from BGR to YCrCb
            Imgproc.cvtColor(matImgSrc, matYCrCb, Imgproc.COLOR_BGR2YCrCb);

            // Create a mask from the min/max settings of YCrCb
            Core.inRange(matYCrCb, new Scalar (yMinTB.value, crMinTB.value, cbMinTB.value),
                                   new Scalar (yMaxTB.value, crMaxTB.value, cbMaxTB.value), msk);
            // Need to fill size the destination Mat before using the
            // copyTo with mask argument, otherwise the mask won't apply.
            dst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
            // All color values in msk Mat = 0 will set the corresponding color value
            // in the destination Mat to 0. All non-zero color values in the msk Mat
            // will copy the source color value to the destination color value.
            Core.copyTo(matImgSrc,dst,msk);
            
            // For BGR view, show the resultant Mat of mask applied to source
            if (imageCB.index < 2) {
                dst.copyTo(matImgDst);
            } 
            // For mask view, show the binary mask
            else {
                msk.copyTo(matImgDst);
            }
        }
        // If a 1-channel Mat is showing (like 'none') then don't process it
        else {
            matImgSrc.copyTo(matImgDst);
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
            sb.append("    public Mat doLinkInRangeYCrCb");
            if (!reference.equals("")) { sb.append("_"+reference); }
             sb.append("(Mat matImgSrc) {\n");
            
            sb.append("        Mat src = new Mat();\n");
            sb.append("        Mat msk = new Mat();\n");
            sb.append("        // Initialize output Mat to all zeros; and to same Size as input Mat\n");
            sb.append("        Mat matImgDst = Mat.zeros(\n");
            sb.append("            matImgSrc.rows(), // int - number of rows\n");
            sb.append("            matImgSrc.cols(), // int - number of columns\n");
            sb.append("            CvType.CV_8U      // int - Mat data type\n");
            sb.append("        );\n");
            sb.append("        // If the source image was a file then the Mat is BGR (as this code assumes)\n");
            sb.append("        // BUT if the source image was a camera then the Mat is likely RGB, so instead use COLOR_RGB2YCrCb\n");
            sb.append("        // Convert source Mat in BGR color space to YCrCb color space\n");
            sb.append("        Imgproc.cvtColor(\n");
            sb.append("            matImgSrc,              // Mat - source\n");
            sb.append("            src,                    // Mat - destination\n");
            sb.append("            Imgproc.COLOR_BGR2YCrCb // int - code space conversion code\n");
            sb.append("        );\n");
            sb.append("        // Create masking Mat msk of all pixels within Scalar boundaries\n");
            sb.append("        Scalar lowerb = new Scalar ("+yMinTB.value+", "+crMinTB.value+", "+cbMinTB.value+");\n");
            sb.append("        Scalar upperb = new Scalar ("+yMaxTB.value+", "+crMaxTB.value+", "+cbMaxTB.value+");\n");
            sb.append("        Core.inRange(\n");
            sb.append("            src,       // Mat    - input Mat\n");
            sb.append("            lowerb,    // Scalar - inclusive lower boundary scalar\n");
            sb.append("            upperb,    // Scalar - inclusive upper boundary scalar\n");
            sb.append("            msk        // Mat    - output Mat, same size as src, and of CV_8U type\n");
            sb.append("        );\n");
            if (imageCB.index == 0) { // use source BGR image
                sb.append("        // Copy matImgSrc pixels to matImgDst, filtered by msk\n");
                sb.append("        Core.copyTo(\n");
                sb.append("            matImgSrc,  // Mat - source Mat\n");
                sb.append("            matImgDst,  // Mat - destination Mat\n");
                sb.append("            msk         // Mat - masking Mat\n");
                sb.append("        );\n");
            }
            else if (imageCB.index == 1) { // use YCbCr image
                sb.append("        // Copy YCbCr pixels to matImgDst, filtered by msk\n");
                sb.append("        Core.copyTo(\n");
                sb.append("            src,        // Mat - source Mat\n");
                sb.append("            matImgDst,  // Mat - destination Mat\n");
                sb.append("            msk         // Mat - masking Mat\n");
                sb.append("        );\n");            
            }
            else { // use mask image
                sb.append("        // Copy masking Mat msk to matImgDst\n");
                sb.append("        msk.copyTo(matImgDst);\n");
            }
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
                new LinkInRangeYCrCb(args);
            }
        });        
    }
}
