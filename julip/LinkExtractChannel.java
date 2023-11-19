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
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * LinkExtractchannel - Select one of multiple Mat channels.
 */
public class LinkExtractChannel extends LinkClass {

    //------------------------------------------------
    // All fields here are specific to this Link Gui.
    // All fields are either final or not initialized.
    //

    private JulipTrackBar channelTB;
    
    private final int channelDefault = 0;
    
    private int numChannels = 1;
    
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
        
    //
    //------------------------------------------------
    
    public LinkExtractChannel (String[] args) {

        // Initialize fields:
        //
        codeFilename = "code_LinkExtractChannel.java";
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
        // (actually, wait on load until panel components are declared)
        matImgSrc = Mat.zeros(new Size(512, 512), CvType.CV_8U);        
//        loadImage(myLinkMap.get("IMAGE_IN"));
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
        
        // JPanel to hold:
        //    Slider + Label for channel selection
        channelTB = new JulipTrackBar(0, numChannels, Integer.parseInt(myLinkMap.get("SELECT_CHANNEL")), 1, 1, this);
        //
        sliderPanel.add(channelTB.label);            
        sliderPanel.add(channelTB.slider);            
        
        //
        // All Link Gui's need to add the JPanel returned from
        // LinkClass buildLinkPanel() method.
        //
        sliderPanel.add(buildLinkPanel());
        loadImage(myLinkMap.get("IMAGE_IN"));

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
     * loadImage - Import image from file; writes to matImgSrc.
     * @param  filename name of image file to import
     * @return   - true if file successfully read, else false
     */
    @Override 
    public boolean loadImage(String filename) {
        boolean tryLoad = super.loadImage(filename);
        if (tryLoad == false) { return false; }

System.out.println("loadImage channels="+matImgSrc.channels());        
        // get number of channels in source Mat
        numChannels = matImgSrc.channels();
        // Reconfigure the sliders to span the number of channels
        channelTB.setMaximum(numChannels);
        channelTB.setValue(numChannels);
        //
        verifySettings();
        
        return true;
    }
            
            
    /**
     * buildMyLinkMap - native default values for Link Gui
     */
    public void buildMyLinkMap() {
        Map<String, String> defaultMap = new HashMap<String, String>() {{
                put("TYPE",     "EXTRACTCHANNEL");
                put("IMAGE_IN", "none");
                put("IMAGE_OUT", "null.png");
                put("LINK_FILE", "nolinkchannel.txt");
                put("SELECT_CHANNEL", "0");
                put("NUM_CHANNELS",   "");
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
        // Check for format and range validity for track bars
        //                
        // The maximum setting depends on the number of channels in the source image, not a fixed number
        int hiLimit = 0;
        if (numChannels != 0) {
            hiLimit = numChannels;
        }            
        intCheck("SELECT_CHANNEL", 0, hiLimit, 0);
        intCheck("NUM_CHANNELS", 0, hiLimit, hiLimit);
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
            writer.write("TYPE\tEXTRACTCHANNEL\n");
            writer.write("IMAGE_IN\t"    + textFieldImageIn.getText() + "\n");
            writer.write("IMAGE_OUT\t"   + textFieldImageOut.getText() + "\n");
            writer.write("LINK_FILE\t"   + linkfilename + "\n");
            writer.write("SELECT_CHANNEL\t"    + channelTB.value + "\n");
            writer.write("NUM_CHANNELS\t"      + numChannels + "\n");
            writer.close();
        } catch (IOException e) {}            
    }
    
    /**
     * saveImage - write output Image to file 
     */    
    @Override
    public void saveImage() {
System.out.println(textFieldImageOut.getText());
System.out.println(matImgDst.toString());
        Imgcodecs.imwrite(textFieldImageOut.getText(), matImgDst);
    }
    
    /**
     * mapToSettings - Overrides method in LinkClass; update gui settings from myLinkMap
     */
    @Override
    public void mapToSettings() {
        channelTB.setValue(Integer.parseInt(myLinkMap.get("SELECT_CHANNEL")));
        numChannels = Integer.parseInt(myLinkMap.get("NUM_CHANNELS"));
    }
    
    /**
     * refreshSettings - Overrides method in LinkClass; copy and error check myLinkMap settings.
     * @return        - True if settings are valid.
     */    
    @Override
    public boolean refreshSettings() {
        if (channelTB.value == numChannels) {
            channelTB.label.setText("Channel selected: ALL");
        } else {
            channelTB.label.setText("Channel selected: " + channelTB.value);
        }
        return true;
    }
        
    /**
     * refreshImage - refresh label texts and shown image
     */
    public void refreshImage() {
        
        if (channelTB.value < numChannels) {
            matImgDst = Mat.zeros(matImgSrc.size(), CvType.CV_8UC1);
            Core.extractChannel(
                matImgSrc,                   // input Mat image
                matImgDst,                   // output Mat image
                channelTB.value              // channel selection
            );
            
        } else {
            matImgDst = Mat.zeros(matImgSrc.size(), matImgSrc.type());
            matImgSrc.copyTo(matImgDst);
        }    
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
        importsL.add("org.opencv.core.Core");
        importsL.add("org.opencv.core.Mat");
        return importsL;
    }     

    /**
     * genCodeString - generate a method that represents this Link's prototype code
     */
    @Override
    public String genCodeString(String reference) {
        returnStr = "Mat";
        objectStr = "channelMat";
        StringBuilder msb = new StringBuilder();
        msb.append("doExtractChannel");
        if (!reference.equals("")) { msb.append("_"+reference); }
        methodStr = msb.toString();
            
        StringBuilder sb = new StringBuilder();
        if (channelTB.value < numChannels) {
            sb.append("    public "+returnStr+" "+methodStr.toString()); 
            sb.append("(Mat matImgSrc) {\n");
            sb.append("        // Initialize output Mat to all zeros; and to same Size as input Mat\n");
            sb.append("        Mat matImgDst = Mat.zeros(\n");
            sb.append("            matImgSrc.rows(), // int - number of rows\n");
            sb.append("            matImgSrc.cols(), // int - number of columns\n");
            sb.append("            CvType.CV_8UC1    // int - Mat data type (one channel)\n");
            sb.append("        );\n");
            sb.append("        Core.extractChannel(\n");
            sb.append("            matImgSrc,    // Mat - input image\n");
            sb.append("            matImgDst,    // Mat - output image\n");
            sb.append("            "+channelTB.value+"     // int - channel selection\n");
            sb.append("        );\n");
            sb.append("        return matImgDst;\n");
            sb.append("    }\n");
        } else {
            sb.append(" // no single channel selected in LinkExtractChannel\n");
        }
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
                new LinkExtractChannel(args);
            }
        });
    }
}
