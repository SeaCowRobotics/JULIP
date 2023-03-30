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
 * LinkInRangeHSV - Graphical user interface to exercise openCV Core.inRange method in HSV color space.
 *                  Uses two Hue ranges and merges result.
 */
public class LinkInRangeHHSV extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //
    private final String[] HUE_STR = { 
        "HUE_1_or_HUE_2",
        "HUE_1_only", 
        "HUE_2_only"
    };
    
    private final String[] IMAGE_TYPE = { 
        "HSV", 
        "Binary"
    };
    
    private JLabel hueLabel;                // HSV Hue color bar of values ranging from 0-180
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private JulipTrackBar hue1MinTB;    
    private JulipTrackBar hue1MaxTB;    
    private JulipTrackBar hue2MinTB;    
    private JulipTrackBar hue2MaxTB;    
    private JulipComboBox hueCB;
    private JulipTrackBar satMinTB;    
    private JulipTrackBar satMaxTB;    
    private JulipTrackBar valMinTB;    
    private JulipTrackBar valMaxTB;    
    private JulipComboBox imageCB;
    
    private final int hueMinDefault = 0;
    private final int hueMaxDefault = 180;
    private final int hueIdxDefault = 0;
    private final int satMinDefault = 0;
    private final int satMaxDefault = 255;
    private final int valMinDefault = 0;
    private final int valMaxDefault = 255;
    private final int imageIdxDefault = 0;    
    //
    //------------------------------------------------
    
    public LinkInRangeHHSV(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkInRangeHHSV.java";   
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
                        
        hue1MinTB = new JulipTrackBar(0, 180, Integer.parseInt(myLinkMap.get("HUE1_MIN")), 30, 6, this);
        hue1MaxTB = new JulipTrackBar(0, 180, Integer.parseInt(myLinkMap.get("HUE1_MAX")), 30, 6, this);
        hue2MinTB = new JulipTrackBar(0, 180, Integer.parseInt(myLinkMap.get("HUE2_MIN")), 30, 6, this);
        hue2MaxTB = new JulipTrackBar(0, 180, Integer.parseInt(myLinkMap.get("HUE2_MAX")), 30, 6, this);
        hueCB     = new JulipComboBox(HUE_STR, myLinkMap.get("HUE_SELECT"), this);
        satMinTB  = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("SAT_MIN")), 50, 10, this);
        satMaxTB  = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("SAT_MAX")), 50, 10, this);
        valMinTB  = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("VAL_MIN")), 50, 10, this);
        valMaxTB  = new JulipTrackBar(0, 255, Integer.parseInt(myLinkMap.get("VAL_MAX")), 50, 10, this);
        imageCB   = new JulipComboBox(IMAGE_TYPE, myLinkMap.get("IMAGE_TYPE"), this);

        hueLabel  = new JLabel(drawHueColorBar());        

        //
        // JPanel to hold:
        //      Hue min JLabel + JSlider
        //      Hue max JLabel + JSlider
        //      Sat min JLabel + JSlider
        //      Sat max JLabel + JSlider
        //      Val min JLabel + JSlider
        //      Val max JLabel + JSlider
        //      link JPanel
        //
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        sliderPanel.add(hue1MinTB.label);
        sliderPanel.add(hue1MinTB.slider);
        sliderPanel.add(hue1MaxTB.label);
        sliderPanel.add(hue1MaxTB.slider);
        sliderPanel.add(hue2MinTB.label);
        sliderPanel.add(hue2MinTB.slider);
        sliderPanel.add(hue2MaxTB.label);
        sliderPanel.add(hue2MaxTB.slider);
        sliderPanel.add(hueCB.comboBox);
        sliderPanel.add(satMinTB.label);
        sliderPanel.add(satMinTB.slider);
        sliderPanel.add(satMaxTB.label);
        sliderPanel.add(satMaxTB.slider);
        sliderPanel.add(valMinTB.label);
        sliderPanel.add(valMinTB.slider);
        sliderPanel.add(valMaxTB.label);
        sliderPanel.add(valMaxTB.slider);
        sliderPanel.add(imageCB.comboBox);
        
        //
        // All Link Gui's need to add the JPanel returned from
        // LinkClass buildLinkPanel() method.
        //
        sliderPanel.add(buildLinkPanel());
        
        // Build frame; the imgLabel is required to be added somewhere to the frame
        frame.add(hueLabel, BorderLayout.PAGE_START);
        frame.add(sliderPanel, BorderLayout.CENTER);        
        frame.add(imgSP, BorderLayout.PAGE_END);

        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                Dimension sizeSP = imgSP.getSize();
                imgSP.setPreferredSize(new Dimension(sizeSP.width, frame.getSize().height - frameHeightMinusImage));
                hueLabel.setIcon(drawHueColorBar());                
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
     * drawHueColorBar - set JLabel to hold a colors of Hue from HSV from values 0 to 180.
     * @return ImageIcon of hue color bar sized to shown output image
     */
    public ImageIcon drawHueColorBar() {
        //
        // Create a JLabel that shows how Hue converts
        // to RGB for all hue values 0-180 with Sat, Val = 255.
        //
        // Size the width of this JLabel to the wider of the input image
        // OR the myPanel minus 24 columns to account for gaps between
        // sliders and border edge.
        //
        double[] data = new double[3];
        int hueCols = Math.max(matImgSrc.cols()-24, frame.getPreferredSize().width-36);
        int hueRows = 10;
        int x;
        int y;
        Mat hueMat = Mat.zeros(10, hueCols, CvType.CV_8UC3);
        for (x = 0; x < hueCols; x++) {
            for (y = 0; y < hueRows; y++) {
                //data = hueMat.get(x,y);
                data[0] = x*180/hueCols;
                data[1] = 255;
                data[2] = 255;
                hueMat.put(y, x, data);
            }
        }
        Imgproc.cvtColor(hueMat, hueMat, Imgproc.COLOR_HSV2BGR);
        Image hueImg = HighGui.toBufferedImage(hueMat);    
        return new ImageIcon(hueImg);
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
                put("MASK_OUT", "nomask.png");
                put("LINK_FILE", "nolinkinrange.txt");
                put("HUE1_MIN", Integer.toString(hueMinDefault));
                put("HUE1_MAX", Integer.toString(hueMaxDefault));
                put("HUE2_MIN", Integer.toString(hueMinDefault));
                put("HUE2_MAX", Integer.toString(hueMaxDefault));
                put("HUE_SELECT", HUE_STR[hueIdxDefault]);
                put("SAT_MIN", Integer.toString(satMinDefault));
                put("SAT_MAX", Integer.toString(satMaxDefault));
                put("VAL_MIN", Integer.toString(valMinDefault));
                put("VAL_MAX", Integer.toString(valMaxDefault));
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
        // Check for format and range validity for all 8 track bars
        //                
        intCheck("HUE1_MIN", 0, 180, hueMinDefault);
        intCheck("HUE1_MAX", 0, 180, hueMaxDefault);
        intCheck("HUE2_MIN", 0, 180, hueMinDefault);
        intCheck("HUE2_MAX", 0, 180, hueMaxDefault);
        intCheck("SAT_MIN", 0, 255, satMinDefault);
        intCheck("SAT_MAX", 0, 255, satMaxDefault);
        intCheck("VAL_MIN", 0, 255, valMinDefault);
        intCheck("VAL_MAX", 0, 255, valMaxDefault);
        
        //
        // Check for format and range validity for all 2 combo boxes
        //        
        comboCheck("HUE_SELECT", HUE_STR, hueIdxDefault);
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
            writer.write("HUE1_MIN\t"   + hue1MinTB.value + "\n");
            writer.write("HUE1_MAX\t"   + hue1MaxTB.value + "\n");
            writer.write("HUE2_MIN\t"   + hue2MinTB.value + "\n");
            writer.write("HUE2_MAX\t"   + hue2MaxTB.value + "\n");
            writer.write("HUE_SELECT\t" + HUE_STR[hueCB.index] + "\n");
            writer.write("SAT_MIN\t"    + satMinTB.value + "\n");
            writer.write("SAT_MAX\t"    + satMaxTB.value + "\n");
            writer.write("VAL_MIN\t"    + valMinTB.value + "\n");
            writer.write("VAL_MAX\t"    + valMaxTB.value + "\n");
            writer.write("IMAGE_TYPE\t" + IMAGE_TYPE[imageCB.index] + "\n");
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
        hue1MinTB.setValue(Integer.parseInt(myLinkMap.get("HUE1_MIN")));
        hue1MaxTB.setValue(Integer.parseInt(myLinkMap.get("HUE1_MAX")));
        hue2MinTB.setValue(Integer.parseInt(myLinkMap.get("HUE2_MIN")));
        hue2MaxTB.setValue(Integer.parseInt(myLinkMap.get("HUE2_MAX")));
        hueCB.setValue(myLinkMap.get("HUE_SELECT"));
        satMinTB.setValue(Integer.parseInt(myLinkMap.get("SAT_MIN")));
        satMaxTB.setValue(Integer.parseInt(myLinkMap.get("SAT_MAX")));
        valMinTB.setValue(Integer.parseInt(myLinkMap.get("VAL_MIN")));
        valMaxTB.setValue(Integer.parseInt(myLinkMap.get("VAL_MAX")));
        imageCB.setValue(myLinkMap.get("IMAGE_TYPE"));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; update gui settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        hue1MinTB.label.setText("Hue_1 Minimum: " + hue1MinTB.value);
        hue1MaxTB.label.setText("Hue_1 Maximum: " + hue1MaxTB.value);
        hue2MinTB.label.setText("Hue_2 Minimum: " + hue2MinTB.value);
        hue2MaxTB.label.setText("Hue_2 Maximum: " + hue2MaxTB.value);
        satMinTB.label.setText("Saturation Minimum: " + satMinTB.value);
        satMaxTB.label.setText("Saturation Maximum: " + satMaxTB.value);
        valMinTB.label.setText("Value Minimum: " + valMinTB.value);
        valMaxTB.label.setText("Value Maximum: " + valMaxTB.value);
        return true;
    }
    
    /**
     * refreshImage - Overrides method in LinkClass; recalculate algorithm and refresh shown image.
     */
    @Override
    public void refreshImage() {
        Mat src = new Mat();
        Mat dst = new Mat();
        Mat msk = new Mat();
        Mat msk2 = new Mat();
        matImgDst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
        // If a 3-channel Mat is showing, then process the image
        if (matImgSrc.channels() == 3) {        
            // Convert the file source image from BGR to HSV
            Imgproc.cvtColor(matImgSrc, src, Imgproc.COLOR_BGR2HSV);

            // Create a mask from the min/max settings of HSV
            Core.inRange(src, new Scalar (hue1MinTB.value, satMinTB.value, valMinTB.value),
                              new Scalar (hue1MaxTB.value, satMaxTB.value, valMaxTB.value), msk);
            // Create a mask from the min/max settings of HSV
            Core.inRange(src, new Scalar (hue2MinTB.value, satMinTB.value, valMinTB.value),
                              new Scalar (hue2MaxTB.value, satMaxTB.value, valMaxTB.value), msk2);
            if (hueCB.index == 0) {  // hue 1 or hue 2
                Core.bitwise_or(msk, msk2, msk);
            }
            else if (hueCB.index == 2)  { // hue 2 only
                msk2.copyTo(msk);
            }
            else { // hue 1 only
                // do nothing, msk is already set for hue 1 only
            }
            // Need to fill size the destination Mat before using the
            // copyTo with mask argument, otherwise the mask won't apply.
            dst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
            // All color values in msk Mat = 0 will set the corresponding color value
            // in the destination Mat to 0. All non-zero color values in the msk Mat
            // will copy the source color value to the destination color value.
            Core.copyTo(matImgSrc,dst,msk);        
            if (imageCB.index == 0) {
                dst.copyTo(matImgDst);
            } else {
                msk.copyTo(matImgDst);
            }
        }
        // If a 1-channel Mat is showing (like 'none') then don't process it
        else {
            matImgSrc.copyTo(matImgDst);
        }
        Image img = HighGui.toBufferedImage(matImgDst);
        imgLabel.setIcon(new ImageIcon(img));
        // resize the Hue color bar according to size of shown image
        hueLabel.setIcon(drawHueColorBar());
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
            sb.append("    public Mat doLinkInRangeHHSV");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("(Mat matImgSrc) {\n");
            
            sb.append("        Mat src = new Mat();\n");
            sb.append("        Mat msk = new Mat();\n");
            sb.append("        Mat msk2 = new Mat();\n");
            sb.append("        // Initialize output Mat to all zeros; and to same Size as input Mat\n");
            sb.append("        Mat matImgDst = Mat.zeros(\n");
            sb.append("            matImgSrc.rows(), // int - number of rows\n");
            sb.append("            matImgSrc.cols(), // int - number of columns\n");
            sb.append("            CvType.CV_8U      // int - Mat data type\n");
            sb.append("        );\n");
            sb.append("        // If the source image was a file then the Mat is BGR (as this code assumes)\n");
            sb.append("        // BUT if the source image was a camera then the Mat is likely RGB, so instead use COLOR_RGB2HSV\n");
            sb.append("        // Convert source Mat in BGR color space to HSV color space\n");
            sb.append("        Imgproc.cvtColor(\n");
            sb.append("            matImgSrc,             // Mat - source\n");
            sb.append("            src,                   // Mat - destination\n");
            sb.append("            Imgproc.COLOR_BGR2HSV  // int - code space conversion code\n");
            sb.append("        );\n");
            if ((hueCB.index == 0) | (hueCB.index == 1)) { // use hue_1
                sb.append("        // Create masking Mat msk of all pixels within Scalar boundaries\n");
                sb.append("        Scalar lowerb1 = new Scalar ("+hue1MinTB.value+", "+satMinTB.value+", "+valMinTB.value+");\n");
                sb.append("        Scalar upperb1 = new Scalar ("+hue1MaxTB.value+", "+satMaxTB.value+", "+valMaxTB.value+");\n");
                sb.append("        Core.inRange(\n");
                sb.append("            src,       // Mat    - input Mat\n");
                sb.append("            lowerb1,   // Scalar - inclusive lower boundary scalar\n");
                sb.append("            upperb1,   // Scalar - inclusive upper boundary scalar\n");
                sb.append("            msk        // Mat    - output Mat, same size as src, and of CV_8U type\n");
                sb.append("        );\n");
            }
            else if (hueCB.index == 2) { // use hue_2 only
                sb.append("        // Create masking Mat msk of all pixels within Scalar boundaries\n");
                sb.append("        Scalar lowerb2 = new Scalar ("+hue2MinTB.value+", "+satMinTB.value+", "+valMinTB.value+");\n");
                sb.append("        Scalar upperb2 = new Scalar ("+hue2MaxTB.value+", "+satMaxTB.value+", "+valMaxTB.value+");\n");
                sb.append("        Core.inRange(\n");
                sb.append("            src,       // Mat    - input Mat\n");
                sb.append("            lowerb2,   // Scalar - inclusive lower boundary scalar\n");
                sb.append("            upperb2,   // Scalar - inclusive upper boundary scalar\n");
                sb.append("            msk        // Mat    - output Mat, same size as src, and of CV_8U type\n");
                sb.append("        );\n");
            }
            if (hueCB.index == 0) { // use hue_1 and hue2
                sb.append("        // Create masking Mat msk of all pixels within Scalar boundaries\n");
                sb.append("        Scalar lowerb2 = new Scalar ("+hue2MinTB.value+", "+satMinTB.value+", "+valMinTB.value+");\n");
                sb.append("        Scalar upperb2 = new Scalar ("+hue2MaxTB.value+", "+satMaxTB.value+", "+valMaxTB.value+");\n");
                sb.append("        Core.inRange(\n");
                sb.append("            src,       // Mat    - input Mat\n");
                sb.append("            lowerb2,   // Scalar - inclusive lower boundary scalar\n");
                sb.append("            upperb2,   // Scalar - inclusive upper boundary scalar\n");
                sb.append("            msk2       // Mat    - output Mat, same size as src, and of CV_8U type\n");
                sb.append("        );\n");
                sb.append("        // Merge two mask Mats with logical-OR operation\n");
                sb.append("        Core.bitwise_or(\n");
                sb.append("            msk,       // Mat - input Mat #1\n");
                sb.append("            msk2,      // Mat - input Mat #2\n");
                sb.append("            msk        // Mat - output Mat\n");
                sb.append("        );\n");
            }
            if (imageCB.index == 0) { // use HSV image
                sb.append("        // Copy matImgSrc pixels to matImgDst, filtered by msk\n");
                sb.append("        Core.copyTo(\n");
                sb.append("            matImgSrc,  // Mat - source Mat\n");
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
                new LinkInRangeHHSV(args);
            }
        });        
    }
}
