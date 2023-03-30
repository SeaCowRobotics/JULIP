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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkErodilate - Graphical user interface to exercise openCV Imgproc.mophologyEx method.
 */
public class LinkSkyStoneDetect extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //
    
    private JulipCheckBox region1KB;
    private JulipCheckBox region2KB;
    private JulipCheckBox region3KB;
    private JLabel mean1L;
    private JLabel mean2L;
    private JLabel mean3L;
    
    private JLabel skyStoneLabel;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    Mat region1Mat;
    Mat region2Mat;
    Mat region3Mat;
    
    int mean1;
    int mean2;
    int mean3;
    int skystoneRegion;
    
    //
    //------------------------------------------------
    
    public LinkSkyStoneDetect(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkSkyStoneDetect.java";
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

        region1KB = new JulipCheckBox("SHOW REGION 1", myLinkMap.get("SHOW_REGION1").equals("1"), this);        
        region2KB = new JulipCheckBox("SHOW REGION 2", myLinkMap.get("SHOW_REGION2").equals("1"), this);        
        region3KB = new JulipCheckBox("SHOW REGION 3", myLinkMap.get("SHOW_REGION3").equals("1"), this);        
        mean1L = new JLabel("  mean:   ");
        mean2L = new JLabel("  mean:   ");
        mean3L = new JLabel("  mean:   ");

        JPanel region1Panel = new JPanel();
        region1Panel.add(region1KB.checkBox);
        region1Panel.add(mean1L);

        JPanel region2Panel = new JPanel();
        region2Panel.add(region2KB.checkBox);
        region2Panel.add(mean2L);

        JPanel region3Panel = new JPanel();
        region3Panel.add(region3KB.checkBox);
        region3Panel.add(mean3L);
        
        JPanel ssPanel = new JPanel();
        ssPanel.add(new JLabel("SkyStone region: "));
        skyStoneLabel  = new JLabel("  ");
        ssPanel.add(skyStoneLabel);

        //
        // JPanel to hold:
        //      morphology JComboBox
        //      kernel JLabel + JSlider
        //      kernel JComboBox
        //      image  JComboBox
        //
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        sliderPanel.add(region1Panel);
        sliderPanel.add(region2Panel);
        sliderPanel.add(region3Panel);
        sliderPanel.add(ssPanel);
        
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
                put("TYPE", "SKYSTONEDETECT");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkskystonedetect.txt");
                put("SHOW_REGION1", "1");
                put("SHOW_REGION2", "1");
                put("SHOW_REGION3", "1");
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
            writer.write("TYPE\tINRANGEHSV\n");
            writer.write("IMAGE_IN\t"        + textFieldImageIn.getText()  + "\n");
            writer.write("IMAGE_OUT\t"       + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"       + linkfilename + "\n");
            writer.write("SHOW_REGION1\t"    + (region1KB.selected ? "1" : "0") + "\n");
            writer.write("SHOW_REGION2\t"    + (region2KB.selected ? "1" : "0") + "\n");
            writer.write("SHOW_REGION3\t"    + (region3KB.selected ? "1" : "0") + "\n");
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
            writer.write("mean region 1: "+mean1 + "\n");
            writer.write("mean region 2: "+mean2 + "\n");
            writer.write("mean region 3: "+mean3 + "\n");
            writer.write("SkyStone is in region: "+skystoneRegion+"\n");
            writer.close();
        } catch (IOException e) {}
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        region1KB.setValue(myLinkMap.get("SHOW_REGION1").equals("1"));
        region2KB.setValue(myLinkMap.get("SHOW_REGION2").equals("1"));
        region3KB.setValue(myLinkMap.get("SHOW_REGION3").equals("1"));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; update gui settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        return true;
    }
    
    /**
     * refreshImage - Overrides method in LinkClass; recalculate algorithm and refresh shown image.
     */
    public void refreshImage() {
        matImgDst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
        matImgSrc.copyTo(matImgDst);
        
        int x1t = 25; int y1t = 48;
        int x1b = 55; int y1b = 64;
        
        int x2t = 75;  int y2t = 40;
        int x2b = 105; int y2b = 56;
        
        int x3t = 130; int y3t = 32;
        int x3b = 160; int y3b = 48;
        
        
        if (region1KB.selected) {
            Imgproc.rectangle(
                matImgDst,
                new Point(x1t,y1t),
                new Point(x1b,y1b),
                new Scalar(0, 255, 0),
                1);
        }
        if (region2KB.selected) {
            Imgproc.rectangle(
                matImgDst,
                new Point(x2t,y2t),
                new Point(x2b,y2b),
                new Scalar(0, 255, 0),
                1);
        }
        if (region3KB.selected) {
            Imgproc.rectangle(
                matImgDst,
                new Point(x3t,y3t),
                new Point(x3b,y3b),
                new Scalar(0, 255, 0),
                1);
        }
        // rowS rowE colS colE
        region1Mat = matImgSrc.submat(new Rect(x1t, y1t, (x1b-x1t), (y1b-y1t)));
        region2Mat = matImgSrc.submat(new Rect(x2t, y2t, (x2b-x2t), (y2b-y2t)));
        region3Mat = matImgSrc.submat(new Rect(x3t, y3t, (x3b-x3t), (y3b-y3t)));
        
        int mean1 = (int) Core.mean(region1Mat).val[0];
        int mean2 = (int) Core.mean(region2Mat).val[0];
        int mean3 = (int) Core.mean(region3Mat).val[0];
        mean1L.setText("  mean: "+mean1);
        mean2L.setText("  mean: "+mean2);
        mean3L.setText("  mean: "+mean3);
        
        skystoneRegion = 1;
        if      ((mean1 < mean2) && (mean1 < mean3)) { skystoneRegion = 1; }
        else if ((mean2 < mean1) && (mean2 < mean3)) { skystoneRegion = 2; }
        else if ((mean3 < mean1) && (mean3 < mean2)) { skystoneRegion = 3; }
        else                                         { skystoneRegion = 0; }
        skyStoneLabel.setText(""+skystoneRegion);
        
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
            sb.append("    public int doLinkSkyStoneDetect");
            if (!reference.equals("")) { sb.append("_"+reference); }
            sb.append("(Mat matImgSrc) {\n");
            
            sb.append("        // Initialize output Mat to all zeros; and to same Size as input Mat\n");
            sb.append("        Mat matImgDst = Mat.zeros(\n");
            sb.append("            matImgSrc.rows(), // int - number of rows\n");
            sb.append("            matImgSrc.cols(), // int - number of columns\n");
            sb.append("            CvType.CV_8U      // int - Mat data type\n");
            sb.append("        );\n");
            sb.append("        // Create a kernel for morphologyEx operation\n");
            sb.append("        Size kernelSize = new Size(\n");
//            sb.append("            "+(2 * kernelTB.value + 1)+", // double - width\n");
//            sb.append("            "+(2 * kernelTB.value + 1)+", // double - height\n");
            sb.append("        );\n");
            sb.append("        Mat element = Imgproc.getStructuringElement(\n");
//            sb.append("            "+KERNEL_TYPE_NAME[kernelCB.index]+",    // int  - shape\n");
            sb.append("            kernelSize,             // Size - ksize\n");
            sb.append("        );\n");
            sb.append("        // Execute morphologyEx operation\n");
            sb.append("        Imgproc.morphologyEx(\n");
            sb.append("            matImgSrc,    // Mat - source\n");
            sb.append("            matImgDst,    // Mat - destination\n");
//            sb.append("            "+MORPH_TYPE_NAME[morphCB.index]+",    // int - operation\n");
            sb.append("            element       // Mat - kernel\n");
            sb.append("        );\n");
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
                new LinkSkyStoneDetect(args);
            }
        });        
    }
}
