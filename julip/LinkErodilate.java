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
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkErodilate - Graphical user interface to exercise openCV Imgproc.mophologyEx method.
 */
public class LinkErodilate extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //
    private final String[] MORPH_STR = {
        "Operation:Erosion",
        "Operation:Dilation",
        "Operation:Opening",
        "Operation:Closing",
        "Operation:Gradient",
        "Operation:Top_Hat",
        "Operation:Black_Hat"
    };
    private final int[] MORPH_TYPE = { 
        Imgproc.MORPH_ERODE,
        Imgproc.MORPH_DILATE,
        Imgproc.MORPH_OPEN, 
        Imgproc.MORPH_CLOSE,
        Imgproc.MORPH_GRADIENT, 
        Imgproc.MORPH_TOPHAT, 
        Imgproc.MORPH_BLACKHAT 
    };
    private final String[] MORPH_TYPE_NAME = { 
        "Imgproc.MORPH_ERODE",
        "Imgproc.MORPH_DILATE",
        "Imgproc.MORPH_OPEN", 
        "Imgproc.MORPH_CLOSE",
        "Imgproc.MORPH_GRADIENT", 
        "Imgproc.MORPH_TOPHAT", 
        "Imgproc.MORPH_BLACKHAT" 
    };
    
    private final String[] KERNEL_STR = { 
        "Kernel:Rectangle", 
        "Kernel:Cross", 
        "Kernel:Ellipse" 
    };
    private final int[] KERNEL_TYPE = {
        Imgproc.CV_SHAPE_RECT,
        Imgproc.CV_SHAPE_CROSS,
        Imgproc.CV_SHAPE_ELLIPSE
    };
    private final String[] KERNEL_TYPE_NAME = {
        "Imgproc.CV_SHAPE_RECT",
        "Imgproc.CV_SHAPE_CROSS",
        "Imgproc.CV_SHAPE_ELLIPSE"
    };
    
    private final String[] IMAGE_STR = { 
        "Output_Image",
        "Input_Image"
    };
    
    private JulipTrackBar kernelTB;    
    private JulipComboBox morphCB;
    private JulipComboBox kernelCB;
    private JulipComboBox imageCB;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private final int morphIdxDefault     = 0;
    private final int kernelIdxDefault    = 0;
    private final int kernelRadiusDefault = 0;
    private final int imageIdxDefault     = 0;    
    //
    //------------------------------------------------
    
    public LinkErodilate(String[] args) {
    
        // Initialize fields:
        codeFilename = "code_LinkErodilate.java";
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

        morphCB  = new JulipComboBox(MORPH_STR, myLinkMap.get("MORPH_OPERATION"), this);        
        kernelTB = new JulipTrackBar(0, 10, Integer.parseInt(myLinkMap.get("KERNEL_RADIUS")), 2, 1, this);
        kernelCB = new JulipComboBox(KERNEL_STR, myLinkMap.get("KERNEL_TYPE"), this);    
        imageCB  = new JulipComboBox(IMAGE_STR,  myLinkMap.get("IMAGE_TYPE"), this);

        //
        // JPanel to hold:
        //      morphology JComboBox
        //      kernel JLabel + JSlider
        //      kernel JComboBox
        //      image  JComboBox
        //
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
        
        sliderPanel.add(morphCB.comboBox);
        sliderPanel.add(kernelTB.slider);
        sliderPanel.add(kernelTB.label);
        sliderPanel.add(kernelCB.comboBox);
        sliderPanel.add(imageCB.comboBox);
        
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
                put("TYPE", "ERODILATE");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkerodilate.txt");
                put("MORPH_OPERATION", MORPH_STR[morphIdxDefault]);
                put("KERNEL_RADIUS", Integer.toString(kernelRadiusDefault));
                put("KERNEL_TYPE", KERNEL_STR[kernelIdxDefault]);
                put("IMAGE_TYPE", IMAGE_STR[imageIdxDefault]);
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
        intCheck("KERNEL_RADIUS", 0, 10, kernelRadiusDefault);
        
        //
        // Check for format and range validity for all 3 combo boxes
        //        
        comboCheck("MORPH_OPERATION", MORPH_STR, morphIdxDefault);
        comboCheck("KERNEL_TYPE", KERNEL_STR, kernelIdxDefault);
        comboCheck("IMAGE_TYPE", IMAGE_STR, imageIdxDefault);
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
            writer.write("MORPH_OPERATION\t" + MORPH_STR[morphCB.index] + "\n");
            writer.write("KERNEL_RADIUS\t"   + kernelTB.value + "\n");
            writer.write("KERNEL_TYPE\t"     + KERNEL_STR[kernelCB.index] + "\n");
            writer.write("IMAGE_TYPE\t"      + IMAGE_STR[imageCB.index] + "\n");
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
        morphCB.setValue(myLinkMap.get("MORPH_OPERATION"));
        kernelTB.setValue(Integer.parseInt(myLinkMap.get("KERNEL_RADIUS")));
        kernelCB.setValue(myLinkMap.get("KERNEL_TYPE"));
        imageCB.setValue(myLinkMap.get("IMAGE_TYPE"));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; update gui settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        kernelTB.label.setText("Kernel Radius: " + kernelTB.value);
        return true;
    }
    
    /**
     * refreshImage - Overrides method in LinkClass; recalculate algorithm and refresh shown image.
     */
    public void refreshImage() {
        matImgDst = Mat.zeros(matImgSrc.rows(), matImgSrc.cols(), CvType.CV_8U);
        Size kernelSize = new Size(
                            2 * kernelTB.value + 1, // double - width
                            2 * kernelTB.value + 1  // double - height
                          );
        Mat element = Imgproc.getStructuringElement(
                        KERNEL_TYPE[kernelCB.index],  // int  - shape
                        kernelSize                    // Size - ksize
                      );
        if (imageCB.index == 0) {
            Imgproc.morphologyEx(
                matImgSrc,                     // Mat - source
                matImgDst,                     // Mat - destination
                MORPH_TYPE[morphCB.index],     // int - operation
                element                        // Mat - kernel
            );
        } else {
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
        importsL.add("org.opencv.core.Size");
        importsL.add("org.opencv.imgproc.Imgproc");
        return importsL;
    }     

    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {
            returnStr = "Mat";
            objectStr = "erodilateMat";
            StringBuilder msb = new StringBuilder();
            msb.append("doLinkErodilate");
            if (!reference.equals("")) { msb.append("_"+reference); }
            methodStr = msb.toString();

            StringBuilder sb = new StringBuilder();
            sb.append("    public "+returnStr+" "+methodStr.toString());  
            sb.append("(Mat matImgSrc) {\n");
            sb.append("        // Initialize output Mat to all zeros; and to same Size as input Mat\n");
            sb.append("        Mat matImgDst = Mat.zeros(\n");
            sb.append("            matImgSrc.rows(), // int - number of rows\n");
            sb.append("            matImgSrc.cols(), // int - number of columns\n");
            sb.append("            CvType.CV_8U      // int - Mat data type\n");
            sb.append("        );\n");
            sb.append("        // Create a kernel for morphologyEx operation\n");
            sb.append("        Size kernelSize = new Size(\n");
            sb.append("            "+(2 * kernelTB.value + 1)+", // double - width\n");
            sb.append("            "+(2 * kernelTB.value + 1)+", // double - height\n");
            sb.append("        );\n");
            sb.append("        Mat element = Imgproc.getStructuringElement(\n");
            sb.append("            "+KERNEL_TYPE_NAME[kernelCB.index]+",    // int  - shape\n");
            sb.append("            kernelSize,             // Size - ksize\n");
            sb.append("        );\n");
            sb.append("        // Execute morphologyEx operation\n");
            sb.append("        Imgproc.morphologyEx(\n");
            sb.append("            matImgSrc,    // Mat - source\n");
            sb.append("            matImgDst,    // Mat - destination\n");
            sb.append("            "+MORPH_TYPE_NAME[morphCB.index]+",    // int - operation\n");
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
                new LinkErodilate(args);
            }
        });        
    }
}
