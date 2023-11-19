package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.Collections;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;


/**
 * ChainGui - Class to handle a chain of Link Gui's that feeds one's output to the next one's input.
 */
public class ChainGui {
    private static final String WINDOW_NAME = "ChainGui";
    
    public boolean isJoinGui = false;
    public boolean sourcesSelectedFlattenedByCmdMap = false;
    
    private List<String> linkGuisList;
    private String[] linkGuis = {      // List of all Link Gui classes available for this app.
                "LinkInRangeHSV       img>img",
                "LinkInRangeHHSV      img>img",
                "LinkCropImage        img>img",
                "LinkInRangeYCrCb     img>mat",
                "LinkErodilate        img>img",
                "LinkExtractChannel   img>img",
                "LinkFindContours     img>ctr",
                "LinkFilterContours   ctr>ctr",
                "LinkGeometryContours ctr>ctr",
                "LinkPolygons         ctr>ctr",                
                "LinkContourStats     ctr>pts",
                "JoinRelicJewel       ctr>nil",
                "LinkRelicPictograph  pts>nil",
                "LinkRoverMineral     ctr>nil",
                "LinkSkyStoneDetect   png>nil",
                "LinkCenterStageProp  ctr>nil"
            };
    Map<String, String> extensions = new HashMap<String, String>() {{
        put("img", "png");
        put("mat", "mat");
        put("ctr", "ctr");
        put("pts", "pts");
        put("nil", "txt");
    }};

    public Map<String, String> myChainMap;
    private Map<String, String> cmdMap;
    private List<String> cmdImage;
    public  List<String>   sourceImages = new ArrayList<>();
    public  List<String>   sourceKeys   = new ArrayList<>();
    public  List<String>   sourcesSelected = new ArrayList<>();
    public  List<LinkGui>  guiLinks     = new ArrayList<>();
    
    public  JTable imgTable;
    public  DefaultTableModel imageTM;
    public  JTable guiTable;
    
    
    private JComboBox guiComboBox;   // JComboBox to hold list of available Link Gui's to add to chain
    private String guiCBString = "";
    
    public  JTextField refChainTF;   // JTextField to hold chain reference. Ex: "relic"
    public  JTextField chainLinkTF;  // JTextField to hold chain settings file name. Ex: "chain_relic.txt"
    private String     chainRefStr;  // Last known chain reference
    
    public  JFrame frame;
    private JPanel myPanel;
    private JLabel imgLabel;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private Mat matImgSrc;
    private Image img;
    
    public  boolean suppressListeners = false;
    
    private boolean guiLinksAreSyncedToGuiTable;
    private boolean guisAreLinked;
    public final int NOWIDTH  = 420;
    public final int NOHEIGHT = 420;
    
    public String chainOutputFile;
    
    /**
     *  ChainGui constructor 
     *  @param args - List of command line arguments
     */
    public ChainGui(String[] args) {
    
        // ChainGui architecture:
        // 
        // ChainGui uses the LinkGui class to retain vital information about the Chain and the
        // sequence of Guis that process image content. Typically, an image will be input to the
        // lead Gui and it will output content to the next Gui, etc. ChainGui maintains a List
        // of LinkGui objects.
        //
        //     (image)--->LinkGui_#1--->LinkGui_#2--->LinkGui_#N
        //
        // At startup, if a chain setting file is specified in the argument list to call the
        // ChainGui application, that file will be parsed and the LinkGui list will be created
        // from this file.
        //
        //   Command line call to ChainGui with arguments
        //     ChainGui() constructor
        //         \--> ParseArgs()
        //                  \-->getChain() --> constructs List of LinkGui's
        //
        // A user interface is constructed to allow users to modify the chain. In particular,
        // two Java Jtables: Image Table and Gui Table hold editable information for the user.
        // At startup, after the List of LinkGui's is (optionally) built from a chain settings
        // file, the ChainGui constructor instantiates all the Java Containers and populates
        // the JTables.
        //
        //     ChainGui() constructor
        //         \--> List of LinkGui's --> user-interactive JTables
        //
        // After the Containers are instantiated, the constructor builds Julip Link Guis, based on
        // the content of the List of LinkGui's.
        //
        //     ChainGui() constructor
        //         \--> List of LinkGui's --> build Julip Link Gui windows
        //
        // The Java Containers are populated with event listeners to allow the Image Table and
        // Gui Table to be edited by the user. Edits to these tables set flags for the new content
        // of the JTables to be updated into the List of Link Guis and refresh or reconstruction of
        // Julip Link Guis.
        //
        //     JTables (edited) ---> (updates to) List of LinkGui's ---> (updates to) Julip Link Gui windows
        //    
        guisAreLinked = true;
        guiLinksAreSyncedToGuiTable = true;        
        linkGuisList = new ArrayList<>(Arrays.asList(linkGuis));    
        parseArgs(this.getClass().getSimpleName(), args);
        
        // Create and set up the window.
        frame = new JFrame(getFrameName());
        imgLabel = new JLabel();        
        imgSP = new JScrollPane(imgLabel);
        imgSP.setPreferredSize(new Dimension(400,400));    
        // ChainGui main panel = myPanel            
        myPanel = new JPanel();
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.PAGE_AXIS));

        // frame will be built as follows:
        //   myPanel
        //   imageScrollPane
        // myPanel will be built as follows:
        //   refControlPanel
        //   imageScrollPane
        //   imageControlPanel
        //   guiScrollPane
        //   guiComboBox
        //   guiControlPanel
        //   chainControlPanel

        //------------------------------------- refControlPanel -------------------------
        //        
        // JPanel with Flow Layout
        //   JButton - README access
        //   JButton - update chain reference
        //   JTextField - text of chain reference
        JPanel refControlPanel = new JPanel();
        JButton refChainB = new JButton("Update Chain Reference");
        JButton readMeB = new JButton("Read Me");
        readMeB.setPreferredSize(new Dimension(100,20));
        readMeB.setOpaque(true);
        readMeB.setBorder(null);
        readMeB.setBackground(Color.YELLOW);
        readMeB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ReadMeFrame readMeFrame = new ReadMeFrame();
            }
        });
        readMeB.repaint();
        refChainTF = new JTextField();
        refChainTF.setPreferredSize(new Dimension(120, 25));
        refChainTF.setText(myChainMap.get("CHAIN_REFERENCE"));
        chainRefStr = refChainTF.getText();                    // save chain reference, in case TextField changes
        refChainB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateRef();
            }
        });
        // build reference control Panel
        refControlPanel.add(readMeB);
        refControlPanel.add(refChainB);
        refControlPanel.add(refChainTF);

        // build main Panel
        myPanel.add(refControlPanel);
        myPanel.add(buildImageScrollPane());
        myPanel.add(buildImageControlPanel());
        
        
        //------------------------------------- gui JTable and JScrollPane -------------------------
        //                
        // The Link Gui JTable holds the sequenced list of Link Guis for this chain.
        // A DefaultTableModel is created to handle JTable editing.
        //
        DefaultTableModel guiTM = new DefaultTableModel() {
            // The first column of the JTable (Link Gui name) is NOT editable.
            // All other columns are editable.
            @Override
            public boolean isCellEditable(int row, int col) {
                switch (col) {
                    case 0:
                        return false;
                    default:
                        return true;
                }
            }
        };
        guiTable = new JTable(guiTM);
        guiTable.addFocusListener(new FocusAdapter() {
            // Changing the contents of the gui JTable put the guiLinks out-of-sync
            // to the JTable.
            @Override
            public void focusLost(FocusEvent e) {
                guiLinksAreSyncedToGuiTable = false;
            }
        });
        // Make the JTable selectable by a single row at a time
        guiTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);        
        // The JTable has 3 columns, in order:
        guiTM.addColumn("Link Gui");
        guiTM.addColumn("Link Settings File");
        guiTM.addColumn("Gui Output File");
        //
        // Iterate through List of guiLinks. For each guiLink an entry will be
        // created in the Link Gui JTable. We infill entries for incomplete
        // guiLinks: link setting filename and for output file name.
        //
        for (int i = 0; i < guiLinks.size(); i++) {
            String name = guiLinks.get(i).name;
            String link = guiLinks.get(i).linkFile;
            if (link.equals("")) {
                link = buildLinkFileName(name);
                guiLinks.get(i).linkFile = link;
            }
            String out = guiLinks.get(i).outputFile;
            if (out.equals("")) {
                out = buildOutputFileName(name);
                guiLinks.get(i).outputFile = out;
            }
            guiTM.addRow(new Object[]{name, link, out});
            // This applies to JoinGui's, and is effectively nothing for ChainGui's.
            if (i == 0) {
                buildGuiKeys(buildLinkGuiName(name));
            }
        }
        //
        // Even if link and output filenames were modified, the Link Gui Table
        // and the List of LinkGui's should be the same.
        //
        guiLinksAreSyncedToGuiTable = true;
        //
        // Add Link gui JTable into JScrollPane
        //
        JScrollPane guiSP = new JScrollPane(guiTable);
        guiSP.setPreferredSize(new Dimension(NOWIDTH,100));
        myPanel.add(guiSP);
        verifyGuiScrollPane();        
        //
        // JComboBox to hold list of all valid LinkGuis that can be added to a chain.
        //
        guiComboBox = new JComboBox<>(linkGuis);
        guiComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox cb = (JComboBox)e.getSource();
                guiCBString = (String)cb.getSelectedItem(); 
            }
        });
        myPanel.add(guiComboBox);
        
        //------------------------------------- guiControlPanel -------------------------
        //        
        // JPanel with Flow Layout
        //   JButton - to add gui
        //   JButton - to remove gui selected in JTable        
        //
        JPanel guiControlPanel = new JPanel();
        //
        // JButton to add the Link Gui selected in Link Gui ComboBox to JTable of Link Gui's
        //
        JButton addGuiB = new JButton("Add Gui ^");
        addGuiB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Make sure a Link Gui has been selected from the ComboBox
                if (guiCBString.equals("")) {
                    System.out.println("No selection made from Link Gui ComboBox.");
                    addGuiB.setBackground(Color.RED);
                } else {                
                    String linkguiname    = buildLinkGuiName(guiCBString);
                    String linkfilename   = buildLinkFileName(linkguiname);
                    String outputfilename = buildOutputFileName(guiCBString);
                    // This is a callout to JoinGui. In ChainGui buildGuiKeys doesn't do anything.
                    if (guiTable.getRowCount() == 0) {
                        buildGuiKeys(linkguiname);
                    }                    
                    guiTM.addRow(new Object[]{guiCBString, linkfilename, outputfilename});
                    guiLinksAreSyncedToGuiTable = false;
                    guisAreLinked = false;
                    addGuiB.setBackground(null);
                }
            }
        });
        //
        // JButton to remove currently selected row in Link Gui JTable
        //
        JButton removeGuiB = new JButton("Kill Gui");
        removeGuiB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = guiTable.getSelectedRow();
                if (row > -1) {
                    guiTM.removeRow(guiTable.getSelectedRow());
                    guiLinksAreSyncedToGuiTable = false;
                    guisAreLinked = false;
                    removeGuiB.setBackground(null);
                }
                else {
                    System.out.println("No row selected for removal in Link Gui JTable.");
                    removeGuiB.setBackground(Color.RED);
                }
            }
        });
        guiControlPanel.add(addGuiB);
        guiControlPanel.add(removeGuiB);
        myPanel.add(guiControlPanel);
        
        //------------------------------------- chainControlPanel -------------------------
        //
        // JPanel with Flow Layout
        //   JButton - to do stuff
        //   JButton - to save chainlink file
        //   JTextField - chainlink filename holder
        //   JButton - export code
        JPanel chainControlPanel = new JPanel();
        JButton updateB = new JButton("Update Gui's");
        updateB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updateGuis()) {
                    updateB.setBackground(null);
                } else {
                    System.out.println("No image selected in Image Table to update Guis.");
                    updateB.setBackground(Color.RED);
                }
            }
        });        
        JButton saveSettingsB = new JButton("Save Settings >");
        saveSettingsB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
            }
        });
        chainLinkTF = new JTextField();
        chainLinkTF.setPreferredSize(new Dimension(120, 25));
        if (myChainMap.containsKey("CHAIN_FILE")) {
            chainLinkTF.setText(myChainMap.get("CHAIN_FILE"));
        }            
        JButton exportCodeB = new JButton("Export Code");
        exportCodeB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportCode();
            }
        });
        chainControlPanel.add(updateB);
        chainControlPanel.add(saveSettingsB);
        chainControlPanel.add(chainLinkTF);
        chainControlPanel.add(exportCodeB);
        myPanel.add(chainControlPanel);
        
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                Dimension sizeSP = imgSP.getSize();
                imgSP.setPreferredSize(new Dimension(sizeSP.width, frame.getSize().height - frameHeightMinusImage));
            }
        });        
        
        // Get Image based on imgTable selection
        getImage();
        // Set the output file names in the guiTable according to
        // image selected in imageTable
        setOutputFileNames();
        frame.add(myPanel, BorderLayout.PAGE_START);
        frame.add(imgSP, BorderLayout.PAGE_END);
        frame.pack();
        frameHeightMinusImage = frame.getSize().height - imgSP.getSize().height;
//        resizeFrame();        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);       
        frame.setLocation(0,0);         
        frame.setVisible(true);
        
        buildGuis();
    }
    
        
    /**
     * buildImageScrollPane - create an JScrollPane to hold image content for the gui.
     * @return - constructed JScrollPane
     */
    public JScrollPane buildImageScrollPane() {
        
        //------------------------------------- image JTable and JScrollPane -------------------------
        //        
        // JScrollpane constructed with JTable
        
        // Create TableModel with non-editable cells
        imageTM = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        imgTable = new JTable(imageTM);
        imgTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        imgTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!suppressListeners) {
                    // If the initial image is changed, set up the chain window to 
                    // propogate the effect of a new image to the Link Gui's                    
                    getImage();
                    setOutputFileNames();
                    //resizeFrame();               
                }
            }
        });
        imageTM.addColumn("Image");
        for (int i = 0; i < sourceImages.size(); i++) {
            imageTM.addRow(new Object[]{sourceImages.get(i)});                                              
        }
        JScrollPane sourceSP = new JScrollPane(imgTable);
        sourceSP.setPreferredSize(new Dimension(NOWIDTH,100));
        if (myChainMap.containsKey("IMAGE_SELECTED")) {
            suppressListeners = true;
            int idx = sourceImages.indexOf(myChainMap.get("IMAGE_SELECTED"));
            if (idx != -1) {
                imgTable.setRowSelectionInterval(idx, idx);
            } else {
                System.out.println("IMAGE_SELECTED:"+myChainMap.get("IMAGE_SELECTED")+" is not in list of IMAGES");
            }
            suppressListeners = false;
        }
        return sourceSP;
        
    }
        
    /**
     * buildImageControlPanel - create the control panel for add/removing images.
     * @return - constructed JPanel
     */
    public JPanel buildImageControlPanel() {
        
        //------------------------------------- imageControlPanel -------------------------
        //        
        // JPanel with Flow Layout
        //   JButton - to add Images
        //   JTextField - name of image to add to JTable
        //   JButton - to remove image selected in JTable
        JPanel imageControlPanel = new JPanel();
        //
        // JButton and JTextfield to add entries to Image JTable
        //
        JButton imageAddB = new JButton("Add Image >");
        JTextField imageAddTF = new JTextField();
        imageAddTF.setPreferredSize(new Dimension(120, 25));
        imageAddB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // check if image entry in imageAddTF already exists in list of images
                // and if there is a non-null entry in the imageAddTF Textfield.                
                String imgStr = imageAddTF.getText().trim();
                if (imgStr.equals("")) {
                    System.out.println("No entry in Add Image Text Field.");
                    imageAddB.setBackground(Color.RED);                    
                } else {
                    if (sourceImages.contains(imgStr)) {
                        System.out.println("Entry "+imgStr+" already exists in Image JTable.");
                        imageAddB.setBackground(Color.RED);                    
                    } else {
                        imageTM.addRow(new Object[]{imgStr});
                        sourceImages.add(imgStr);
                        imageAddB.setBackground(null);
                    }
                }
            }
        });
        //
        // JButton to remove entries from Image JTable
        //
        JButton imageRemoveB = new JButton("Kill Image");
        imageRemoveB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Delete the selected image from the list of images and the jtable
                int row = imgTable.getSelectedRow();
                if (row > -1) {
                    imageTM.removeRow(imgTable.getSelectedRow());
                    sourceImages.remove(row);
                    imageRemoveB.setBackground(null);
                }
                else {
                    System.out.println("No row selected for removal in Image JTable.");
                    imageRemoveB.setBackground(Color.RED);
                }
            }
        });
        imageControlPanel.add(imageAddB);
        imageControlPanel.add(imageAddTF);
        imageControlPanel.add(imageRemoveB);
        
        return imageControlPanel;
    }
    
    /**
     * verifyGuiScrollPane - not needed for ChainGui; overridden by JoinGui.
     */
    public void verifyGuiScrollPane() {
    }
    
    /**
     * buildGuiKeys - not needed for ChainGui; overridden by JoinGui.
     */
    public void buildGuiKeys(String guiName) {
    }
    
    /**
     * getFrameName - returns name of frame (overridable)
     * @return String  - name of frame
     */
     public String getFrameName() {
        return ("ChainGui");
     }

    /**
     * resizeFrame - utility method to resize this window
     */
    private void resizeFrame() {
        frame.setPreferredSize( new Dimension(
                                    Math.max(matImgSrc.cols(), NOWIDTH), 
                                    (int)myPanel.getPreferredSize().getHeight() + matImgSrc.rows()
                                ));
        frame.pack();
        frame.repaint();
    }
    
    /**
     * buildLinkGuiName - utility to construct String representing Link Gui name.
     * @param LinkGuiString - name of Link Gui, including in>out notation.
     * @return String       - name of Link Gui name
     */
    private String buildLinkGuiName(String LinkGuiString) {
        String[] chunks = LinkGuiString.split("\\s+");
        return chunks[0]; 
    }
    
    
    /**
     * buildLinkFileName - utility to construct String representing Link Gui setting filename.
     * @param LinkGuiName - name of Link Gui
     * @return String     - name of Link Gui Setting Filename
     */
    private String buildLinkFileName(String LinkGuiName) {
        // construct String of format:
        //    chain_<chain_reference>_<LinkGui_name>.txt
        String chainRef = refChainTF.getText().trim();
        if (chainRef.equals("")) {
            chainRef = "default";
        }
        return "chain_" + chainRef + "_" + LinkGuiName + ".txt"; 
    }
   
    /**
     * buildOutputFileName - utility to construct String representing Link Gui output filename.
     * @param LinkGuiString - name of Link Gui, including in>out notation
     * @return String       - name of Output Filename
     */
    private String buildOutputFileName(String LinkGuiString) {
        // construct String of format:
        //    chain_<chain_reference>_<LinkGui_name>_<Output_name>.<Output filename extension>
        
        // We get the Output_name from the selected image in the Image JTable, if a row is selected
        String outputbasename;
        List<String> chunks;
        int idx = imgTable.getSelectedRow();
        if (idx > -1) {
            // remove the input filename extension
            String inputfilename = imgTable.getValueAt(idx,0).toString();        
            chunks = new ArrayList<>(Arrays.asList(inputfilename.split("\\.")));
            if (chunks.size() > 1) {
                chunks.remove(chunks.size()-1);
            }
            String[] chunkStrs = new String[chunks.size()];
            outputbasename = String.join(".", chunks.toArray(chunkStrs));
        } else {
            outputbasename = "null";
        }
        // We get the output file extension from the LinkGuiBase
        String[] stubs = LinkGuiString.split("\\s+");
        String[] exts = stubs[stubs.length-1].split(">");
        
        
        // The chain reference is taken from the Chan Reference JTextField.
        String chainRef = refChainTF.getText().trim();
        if (chainRef.equals("")) {
            chainRef = "default";
        }
        // The output filename extension is pulled from extensions map.
        return "chain_" + chainRef + "_" + stubs[0] + "_" + 
                outputbasename + "." + extensions.get(exts[1]);
    }                                            
    
    
    /**
     * getImage - method to load image into ChainGui frame.
     */
    public void getImage() {    
        // Get Image based on imgTable selection
        int rowIndex = imgTable.getSelectedRow();
        String sourceFilename = "";
        if (rowIndex > -1) {
            sourceFilename = imgTable.getValueAt(rowIndex, 0).toString();
        } 
        // If there is no readable image then set the image Label to 
        // given static size with all elements set to 0.
        // With no readable image, the Mat itself will not be null because
        // it will have a header; there will be no data rows or columns.
        matImgSrc = Imgcodecs.imread(sourceFilename);
        if ((matImgSrc.rows() == 0) || (matImgSrc.cols() == 0)) {
            matImgSrc = Mat.zeros(new Size(NOWIDTH,NOHEIGHT), CvType.CV_8UC3);
        }
        img = HighGui.toBufferedImage(matImgSrc);
        imgLabel.setIcon(new ImageIcon(img));
    }
    
    public void setOutputFileNames() {
        for (int i = 0; i < guiTable.getRowCount(); i++) {
            String linkGuiStr = guiTable.getValueAt(i,0).toString();
            String outputImageFile = buildOutputFileName(linkGuiStr);
            guiTable.setValueAt(outputImageFile, i, 2);
            if (guiLinksAreSyncedToGuiTable) {
                guiLinks.get(i).outputFile = guiTable.getValueAt(i,2).toString();
            }
        }
    }
    

    /**
     * parseArgs - Parse the command line arguments. 
     *             Call the method to build myChainMap.
     *  @param className - Name of Class from which main method called
     *  @param args      - Array of command line arguments 
     */
    public void parseArgs(String className, String[] args) {
    
        boolean setSettingFile = false;
        boolean setImageIn     = false;
        boolean setImageSelect = false;
        boolean setImageOut    = false;    
        String chainfilename = "";
        boolean setKey          = false;
        boolean setKeyInput     = false;
        boolean setJoinSelected = false;
        boolean setChainRef     = false;
        boolean isFirstArgJ     = true;
        String  thisKey = null;        
        
        cmdMap = new HashMap<>();
        cmdImage = new ArrayList<>();
        
        // If '--help' is any of the arguments then 
        // show proper command line usage and exit
        for (String arg : args) {
            //
            // Always good to have an option to show what the valid command arguments are.
            //
            if (arg.equals("--help") || arg.equals("-help")) {
                System.out.println("Usage:");
                System.out.println(className);
                System.out.println("    [<ChainFileName>]   // use full filename including file extension");
                System.out.println("    [[-]-help]          // use either one or two dashes in front of 'help'");
                System.out.println("    [-r <reference>]");
                System.out.println("    [-f <chainSettingFileName]");
                System.out.println("    [-i <ImageInputFileName>]");
                System.out.println("    [-s <ImageSelected>]");
                System.out.println("    [-o <ImageOutputFileName>]");                
                System.exit(0);
            }

            //
            // Build command map (cmdMap) out of command line arguments
            // These will override the settings from the chain settings file
            //
        
            //
            // Look for -r command. 
            // The next arg after -r is the chain reference.
            //
            if (setChainRef) {
                cmdMap.put("CHAIN_REFERENCE", arg);
                setChainRef = false;
            }
            if (arg.equals("-r")) {
                setChainRef = true;
            }
            //
            // Look for -f command. 
            // The next arg after -f is the chain settings filename.
            //
            if (setSettingFile) {
                cmdMap.put("CHAIN_FILE", arg);
                setSettingFile = false;
            }
            if (arg.equals("-f")) {
                setSettingFile = true;
            }
            //
            // Look for -i command. 
            // The next arg after -i is the image input filename.
            //
            if (setImageIn) {
                cmdImage.add(arg);
                setImageIn = false;
            }
            if (arg.equals("-i")) {
                setImageIn = true;
            }
            //
            // Look for -s command. 
            // The next arg after -i is the image input filename.
            //
            if (setImageSelect) {
                cmdMap.put("IMAGE_SELECTED", arg);
                setImageSelect = false;
            }
            if (arg.equals("-s")) {
                setImageSelect = true;
            }        
            //
            // Look for -o command. 
            // The next arg after -o is the image output filename.            
            //
            if (setImageOut) {
                cmdMap.put("IMAGE_OUT", arg);
                setImageOut = false;
            }
            if (arg.equals("-o")) {
                setImageOut = true;                            
            }
        
            //
            // Look for -k command.
            // The next arg after -k is the key
            // The second arg after -k is the input file for the key
            //
            if (setKeyInput) {
                sourceImages.add(arg);
                sourceKeys.add(thisKey);
                setKeyInput = false;
                thisKey = null;
            }
            if (setKey) {
                thisKey = arg;
                setKeyInput = true;
                setKey = false;
            }
            if (arg.equals("-k")) {
                setKey = true;
            }                                
            //
            // Look for -j command. 
            // The next arg after -j is the image input filename.
            //
            if (setJoinSelected) {
                sourcesSelected.add(arg);
                setJoinSelected = false;
            }
            if (arg.equals("-j")) {
                setJoinSelected = true;
                if (isFirstArgJ) {
                    isFirstArgJ = false;
                    sourcesSelected.clear();
                    sourcesSelectedFlattenedByCmdMap = true;
                }
            }        
        
            // Chain setting file name can only be first argument.
            // All commands have a '-' prefix.
            if ((args.length > 0) && (args[0].charAt(0) != '-')) {
                chainfilename = args[0];
            }
        }        
        
        // Keep for debugging
        //System.out.println(cmdMap);
        //System.out.println(cmdImage);
        
        getChain(chainfilename);
    }    
    
    /**
     * getChain - parse chain file
     * @param chainfilename - chain file name
     */
    public void getChain(String chainfilename) {
        myChainMap = new HashMap<>();
        BufferedReader reader;
        boolean failToParse = false;
        int lineNum = 1;
        LinkGui gui = null;
        JoinInput joinInput = null;
        // If there is a chain file name provided in the command line arguments,
        // then try to parse it. 
        if (!chainfilename.equals("")) {
            try {
                myChainMap.put("CHAIN_FILE", chainfilename);
                reader = new BufferedReader(new FileReader(chainfilename));
                String line = reader.readLine();
                while (line != null) {
                    line = line.trim();
                    String[] chunks = line.split("\\s+");
                    
                    // xml-like format to parse:
                    //                    
                    // <IMAGE> image.png </IMAGE>
                    // <JOIN_INPUT> 
                    //   <JOIN_FILE> file.txt </JOIN_FILE>
                    //   <JOIN_KEY>  key </JOIN_KEY>
                    // </JOIN_INPUT>
                    // <IMAGE_SELECTED> image.png <IMAGE_SELECTED>
                    // <CHAIN_REFERENCE> reference <CHAIN_REFERENCE>
                    // <CHAIN_FILE> file.txt <CHAIN_FILE>
                    // <CHAIN_OUTPUT> file.txt <CHAIN_OUTPUT>     (only used with MeshGui)
                    // <LINK_GUI>
                    //   <NAME> LinkGui </NAME>
                    //   <LINK> file.txt </LINK>
                    //   <OUT> file.txt </OUT>
                    // </LINK_GUI>
                    
                    if (chunks.length != 0) {
                        //
                        // LINK_GUI has a hierarchy
                        //
                        if (chunks[0].equals("<LINK_GUI>")) {
                            guiLinksAreSyncedToGuiTable = false;
                            guisAreLinked = false;
                            if (gui != null) { 
                                System.out.println("gui points to non-null reference. Fail.");
                                failToParse = true; 
                            }
                            else {
                                gui = new LinkGui();
                            }
                        }
                        else if (chunks[0].equals("</LINK_GUI>")) {                        
                            if (guiLinks == null) { failToParse = true; }
                            else {
                                // Error checking -
                                //   - if there was no NAME specified for the LINK_GUI then null this LINK_GUI parse
                                if (!gui.name.equals("")) {
                                    guiLinks.add(gui);
                                } else {
                                    System.out.println("Missing NAME in LINK_GUI.");
                                    failToParse = true;
                                }
                                gui = null;
                            }
                        }
                        //
                        // NAME, LINK and OUT are all subsets of LINK_GUI
                        //
                        else if (chunks[0].equals("<NAME>")) {
                            // <NAME> is allowed to have whitespace between its delimiters
                            if (!chunks[chunks.length-1].equals("</NAME>")) {
                                failToParse = true;
                            } else if (gui != null) {
                                // trim off <NAME>, </NAME>
                                gui.name = line.substring(6,line.length()-7).trim();
                            } else {
                                failToParse = true;
                            }
                        }
                        else if (chunks[0].equals("<LINK>")) {
                            if (chunks.length != 3) {
                                failToParse = true;
                            } else if (!chunks[2].equals("</LINK>")) {
                                failToParse = true;
                            } else if (gui == null) {
                                failToParse = true;
                            } else {                        
                                gui.linkFile = chunks[1].trim();                                
                            }
                        }
                        else if (chunks[0].equals("<OUT>")) {
                            if (chunks.length != 3) {
                                failToParse = true;
                            } else if (!chunks[2].equals("</OUT>")) {
                                failToParse = true;
                            } else if (gui == null) {
                                failToParse = true;
                            } else {                        
                                gui.outputFile = chunks[1].trim();                                
                            }
                        }
                        //
                        // JOIN_INPUT has a hierarchy
                        //
                        else if (chunks[0].equals("<JOIN_INPUT>")) {
                            if (joinInput != null) { failToParse = true; }
                            else {
                                joinInput = new JoinInput();
                            }
                        }
                        else if (chunks[0].equals("</JOIN_INPUT>")) {                        
                            // Error checking -
                            //   - if there was no NAME specified for the LINK_GUI then null this LINK_GUI parse
                            if (joinInput.file.equals("")) {
                                System.out.println("Missing JOIN_FILE in JOIN_INPUT.");
                                failToParse = true;
                            }
                            else if (joinInput.key.equals("")) {
                                System.out.println("Missing JOIN_KEY in JOIN_INPUT.");
                                failToParse = true;
                            }
                            if (!sourceImages.contains(joinInput.file)) {
                                sourceImages.add(joinInput.file);
                                sourceKeys.add(joinInput.key);
                            }
                            joinInput = null;                            
                        }                        
                        //
                        // JOIN_FILE, JOIN_KEY are subsets of JOIN_INPUT
                        //
                        else if (chunks[0].equals("<JOIN_FILE>")) {
                            if (chunks.length != 3) {
                                System.out.println("Only 3 contiguous non-whitespace sequences allowed. Fail.");
                                failToParse = true;
                            } else if (!chunks[2].equals("</JOIN_FILE>")) {
                                System.out.println("1st char sequence is <JOIN_FILE> therefore 3rd sequence must be </JOIN_FILE>. Fail.");
                                failToParse = true;
                            } else if (joinInput == null) {
                                System.out.println("No joinInput object declared. Fail.");
                                failToParse = true;
                            } else {                        
                                joinInput.file = chunks[1].trim();                                
                            }
                        }
                        else if (chunks[0].equals("<JOIN_KEY>")) {
                            if (chunks.length != 3) {
                                System.out.println("Only 3 contiguous non-whitespace sequences allowed. Fail.");
                                failToParse = true;
                            } else if (!chunks[2].equals("</JOIN_KEY>")) {
                                System.out.println("1st char sequence is <JOIN_KEY> therefore 3rd sequence must be </JOIN_KEY>. Fail.");
                                failToParse = true;
                            } else if (joinInput == null) {
                                System.out.println("No joinInput object declared. Fail.");
                                failToParse = true;
                            } else {                        
                                joinInput.key = chunks[1].trim();                                
                            }
                        }
                        
                        //
                        // Note: IMAGE is a separate attribute from a JOIN_IMAGE
                        //
                        else if (chunks[0].equals("<IMAGE>")) {
                            if (chunks.length != 3) {
                                failToParse = true;
                            } else if (!chunks[2].equals("</IMAGE>")) {
                                failToParse = true;
                            } else {
                                sourceImages.add(chunks[1].trim());
                            }                        
                        }    
                        //
                        // IMAGE_SELECTED is a separate attribute from a JOIN_SELECTED
                        //
                        else if (chunks[0].equals("<IMAGE_SELECTED>")) {
                            if (chunks.length != 3) {
                                failToParse = true;
                            } else if (!chunks[2].equals("</IMAGE_SELECTED>")) {
                                failToParse = true;
                            } else {
                                myChainMap.put("IMAGE_SELECTED", chunks[1].trim());
                            }
                        }
                        else if (chunks[0].equals("<JOIN_SELECTED>")) {
                            if (chunks.length != 3) {
                                failToParse = true;
                            } else if (!chunks[2].equals("</JOIN_SELECTED>")) {
                                failToParse = true;
                            } else {
                                if (!sourcesSelectedFlattenedByCmdMap) {
                                    sourcesSelected.add(chunks[1].trim());
                                }
                            }
                        }
                        else if (chunks[0].equals("<CHAIN_REFERENCE>")) {
                            if (chunks.length != 3) {
                                failToParse = true;
                            } else if (!chunks[2].equals("</CHAIN_REFERENCE>")) {
                                failToParse = true;
                            } else {
                                myChainMap.put("CHAIN_REFERENCE", chunks[1].trim());
                            }
                        }
                        else if (chunks[0].equals("<CHAIN_FILE>")) {
                            if (chunks.length != 3) {
                                failToParse = true;
                            } else if (!chunks[2].equals("</CHAIN_FILE>")) {
                                failToParse = true;
                            } else {
                                myChainMap.put("CHAIN_FILE", chunks[1].trim());
                            }
                        }
                        else {
                            System.out.println("Cannot resolve xml attribute:"+chunks[0]);
                            failToParse = true;
                        }
                    }
                    
                    if (failToParse) {
                        throw new IOException("Invalid format in link file: " + chainfilename + " line: " + lineNum + "\n"+line);
                    }
                    
                    line = reader.readLine();
                    lineNum += 1;
                }                
                reader.close();
            }
            // For all IOExceptions, clear myChainkMap from any 
            // accumulated key, values before the exception was thrown.
            catch (IOException e) { 
                System.out.println("ChainGui.getChain() error:");        
                System.out.println(e);
                myChainMap.clear();
            }
        }
        // If there is no explicit CHAIN_REFERENCE given then
        // set it to "default" so that there is at least some
        // sort of chain reference.
        if (!myChainMap.containsKey("CHAIN_REFERENCE")) {
            myChainMap.put("CHAIN_REFERENCE", "default");
        }
        // If there is no explicit CHAIN_FILE given then
        // set it to "default" so that there is at least some
        // sort of chain reference.
        if (!myChainMap.containsKey("CHAIN_FILE")) {
            myChainMap.put("CHAIN_FILE", "chain_default.txt");
        }
        
        
        // Override myChainMap built from file settings with
        // command line arguments
        for (Map.Entry<String, String> entry : cmdMap.entrySet()) {
            if (entry.getKey().equals("CHAIN_REFERENCE")) {
                myChainMap.replace("CHAIN_REFERENCE", entry.getValue());
            }
            if (entry.getKey().equals("CHAIN_FILE")) {
                myChainMap.replace("CHAIN_FILE", entry.getValue());
            }
            if (entry.getKey().equals("IMAGE_SELECTED")) {
                if (myChainMap.containsKey("IMAGE_SELECTED")) {
                    myChainMap.replace("IMAGE_SELECTED", entry.getValue());
                } else {
                    myChainMap.put("IMAGE_SELECTED", entry.getValue());
                }
            }
            if (entry.getKey().equals("CHAIN_OUTPUT")) {
                if (myChainMap.containsKey("CHAIN_OUTPUT")) {
                    myChainMap.replace("CHAIN_OUTPUT", entry.getValue());
                } else {
                    myChainMap.put("CHAIN_OUTPUT", entry.getValue());
                }
            }
        }
        for (int iidx = 0; iidx < cmdImage.size(); iidx++) {
            if (!(sourceImages.contains(cmdImage.get(iidx)))) {
                sourceImages.add(cmdImage.get(iidx));
            }
        }
    }
    
    
    public void buildGuis() {
    
        int i; // iterator
        
        // keep for debugging
        //System.out.println("guiLinksAreSyncedToGuiTable:"+guiLinksAreSyncedToGuiTable);
        //System.out.println("guisAreLinked:"+guisAreLinked);
        
        //
        // If the guiTable has been updated since last creation of list of Link Guis then
        // rebuild the list of Link Gui objects from the guiTable.
        //
        if (!guiLinksAreSyncedToGuiTable) {
            // Kill off frames from screen
            for (i = 0; i < guiLinks.size(); i++) {
                if (guiLinks.get(i).gui != null) {
                    guiLinks.get(i).gui.frame.dispose();
                }
            }
            // clear out list of guiLinks
            guiLinks.clear();
            // build new list of guiLinks from guiTable
            for (i = 0; i < guiTable.getRowCount(); i++) {
                // for each entry in the guiTable, create a LinkGui object
                // and add it to the List of guiLinks
                LinkGui gui    = new LinkGui();
                gui.name       = guiTable.getValueAt(i,0).toString();
                gui.linkFile   = guiTable.getValueAt(i,1).toString();
                gui.outputFile = guiTable.getValueAt(i,2).toString();
                guiLinks.add(gui);
            }
            guiLinksAreSyncedToGuiTable = true;
            guisAreLinked = false;
        }
    
        //
        // If the Link Gui windows need to be refreshed then do so.
        //
        if (!guisAreLinked) {
        
            List<String> argList = new ArrayList<String>();
            List<String> argAppendImage = new ArrayList<String>();
            
            argAppendImage = buildLinkInputs();
            
            for (i = 0; i < guiLinks.size(); i++) {
            
                LinkGui thisLinkGui = guiLinks.get(i);
            
                argList.clear();
                // set link filename into 1st position of argument list
                argList.add(thisLinkGui.linkFile);
                // add chain reference
                argList.add("-r");
                argList.add(chainRefStr);

                if (i == 0) {
                    // add input file name to argument list
                    argList.addAll(argAppendImage);
                } else {
                    // connect output file of prior Link Gui as input file to this Link Gui
                    argList.add("-i");
                    argList.add(guiLinks.get(i-1).outputFile);
                }
                // add output file name to argument list
                argList.add("-o");
                argList.add(thisLinkGui.outputFile);
                // ALL additional arguments are low-priority command line default arguments...
                argList.add("-default");
                // add link file name to argument list
                argList.add("-f");
                argList.add(thisLinkGui.linkFile);
                                                
                // Convert List of args to array of String of args
                String[] args = new String[argList.size()];
                args = argList.toArray(args);

                // Create new Link Gui windows according to 'type' of link gui,                
                thisLinkGui.gui = new LinkClass();
                String name = thisLinkGui.name.trim();
                String[] chunks = name.split("\\s+");
            
                thisLinkGui.gui = newGui(chunks[0], args);
                
                //keep for debugging
                //System.out.println("buildGuis arglist:" +chunks[0]);
                //System.out.println(argList.toString());
                
                // Set position of Link Gui on computer screen
                int x = (i+1)*100+300;
                int y = (i+1)*50;
                thisLinkGui.gui.setFrameLocation(x,y);
                                
                // Save this LinkGui's settings                                
                thisLinkGui.gui.saveSettings();
                // Save this LinkGui's output for the next LinkGui
                // in the chain to have available as input
                thisLinkGui.gui.saveImage();
            }
            guisAreLinked = true;
        }
        
    }

    /**
     * buildLinkInputs - construct command line arguments to instantiate a LinkGui and
     *                   provide the input file to the LinkGui
     * @return - list of String of arguments
     */
    public List<String> buildLinkInputs() {
        
        List<String> argList = new ArrayList<>();
    
        //
        // Get image input file name
        //
        int idx = imgTable.getSelectedRow();
        if (idx > -1) {                
            argList.add("-i");
            argList.add(imgTable.getValueAt(idx, 0).toString());
        }
        return argList;
    }
    
    
    /**
     *
     */
    public LinkClass newGui(String guiName, String[] args) {
        LinkClass gui = new LinkClass();
                if (guiName.equals("LinkInRangeHSV")) {
                    gui = new LinkInRangeHSV(args);
                } 
                else if (guiName.equals("LinkInRangeHHSV")) {
                    gui = new LinkInRangeHHSV(args);
                }
                else if (guiName.equals("LinkCropImage")) {
                    gui = new LinkCropImage(args);
                }
                else if (guiName.equals("LinkInRangeYCrCb")) {
                    gui = new LinkInRangeYCrCb(args);
                }
                else if (guiName.equals("LinkErodilate")) {
                    gui = new LinkErodilate(args);
                }                
                else if (guiName.equals("LinkExtractChannel")) {
                    gui = new LinkExtractChannel(args);
                }                
                else if (guiName.equals("LinkFindContours")) {
                    gui = new LinkFindContours(args);
                }
                else if (guiName.equals("LinkFilterContours")) {
                    gui = new LinkFilterContours(args);
                }
                else if (guiName.equals("LinkGeometryContours")) {
                    gui = new LinkGeometryContours(args);
                }
                else if (guiName.equals("LinkPolygons")) {
                    gui = new LinkPolygons(args);
                }                
                else if (guiName.equals("LinkContourStats")) {
                    gui = new LinkContourStats(args);
                }
                else if (guiName.equals("LinkRelicPictograph")) {
                    gui = new LinkRelicPictograph(args);
                }
                else if (guiName.equals("JoinRelicJewel")) {
                    gui = new JoinRelicJewel(args);
                }                
                else if (guiName.equals("LinkRoverMineral")) {
                    gui = new LinkRoverMineral(args);
                }
                else if (guiName.equals("LinkSkyStoneDetect")) {
                    gui = new LinkSkyStoneDetect(args);
                }
                else if (guiName.equals("LinkCenterStageProp")) {
                    gui = new LinkCenterStageProp(args);
                }
                else {
                    System.out.println("BuidGui: Failed to match to a Link Gui name:"+guiName);
                }
        return gui;
    }
    
    
    /**
     * updateRef - update link settings file names based on an update of the chain reference.
     */ 
    public void updateRef() {        
        String newfilename;
        //
        // For each link file name, substitute the new chain reference in
        // place of the last known chain reference.
        //
        for (int i = 0; i < guiTable.getRowCount(); i++) {
            // apply update to link file names
            String linkfilename = guiTable.getValueAt(i,1).toString();
            newfilename = linkfilename.replace(chainRefStr, refChainTF.getText());
            newfilename = newfilename.replace("default", refChainTF.getText());
            if (!linkfilename.equals(newfilename)) {
                guiLinksAreSyncedToGuiTable = false;
                guiTable.setValueAt(newfilename,i,1);
            }
            // apply update to output file names
            String outputfilename = guiTable.getValueAt(i,2).toString();
            newfilename = outputfilename.replace(chainRefStr, refChainTF.getText());
            newfilename = newfilename.replace("default", refChainTF.getText());
            if (!outputfilename.equals(newfilename)) {
                guiLinksAreSyncedToGuiTable = false;
                guiTable.setValueAt(newfilename,i,2);
            }
            
        }    
        //
        // Substitute the new chain reference in
        // place of the chain settings file name.
        //
        String chainfilename = chainLinkTF.getText();
        newfilename = chainfilename.replace(chainRefStr, refChainTF.getText());
        newfilename = newfilename.replace("default", refChainTF.getText());
        chainLinkTF.setText(newfilename);
    }
    
    /**
     * updateGuis - propogate input file to first link gui and update it
     * @return boolean - true if no errors, else false
     */
    public boolean updateGuis() {
    
        LinkClass gui;
        boolean status = true;
        
        buildGuis();
                
        // Propogate chain settings through Link Guis
        for (int i = 0; i < guiLinks.size(); i++) {
            gui = guiLinks.get(i).gui;
            // set output Text Field in Link Gui to updated output file in list of guiLinks
            gui.textFieldImageOut.setText(guiLinks.get(i).outputFile);
                
            // If JoinClass...
            if (isJoinGui) {
                gui.setImageInTF(buildLinkInputs());
                gui.loadImage("");
            }
            // If LinkClass...
            else {
                // set input Text Field in Link Gui to output Text Field of prior Gui
                // except for first Link Gui, set its input to the selected row in the Image Table
                if (i == 0) {
                    // Get Image based on imgTable selection
                    int rowIndex = imgTable.getSelectedRow();
                    if (rowIndex < 0) {
                        status = false;
                    }
                    else {
                        gui.textFieldImageIn.setText(imgTable.getValueAt(rowIndex, 0).toString());
                    }
                } else {
                    gui.textFieldImageIn.setText(guiLinks.get(i-1).outputFile);
                }
                // upload input file
                gui.loadImage(gui.textFieldImageIn.getText());
            }
            
            if (status) {
                //gui.resizeFrame();
                // update Image and outputs in Link Gui
                gui.refreshSettings();
                gui.refreshImage();
                // update output file from Link Gui
                gui.saveImage();
            
                // keep for debugging
                //System.out.println(i+":"+gui.textFieldImageOut.getText());
            }
            // set this Chain's output file same as output file in last Link Gui
            //chainOutputFile = guiLinks.get(guiLinks.size()-1).textFieldImageOut.getText();
            gui = guiLinks.get(guiLinks.size()-1).gui;
            chainOutputFile = gui.textFieldImageOut.getText();
        }
        return status;
    }

    /**
     * saveSettings - write chain gui settings to file
     */
    public void saveSettings() {
        String chainfilename = chainLinkTF.getText();
        
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(chainfilename));
            writer.write("<CHAIN_REFERENCE>\t" + refChainTF.getText() +"\t</CHAIN_REFERENCE>\n");
            for (int i = 0; i < imgTable.getRowCount(); i++) {
                writer.write("<IMAGE>\t"+imgTable.getValueAt(i, 0).toString()+"\t</IMAGE>\n");
            }
            int srcSelectionIndex = imgTable.getSelectedRow();
            String sourceFilename = "";
            if (srcSelectionIndex > -1) {
                sourceFilename = imgTable.getValueAt(srcSelectionIndex, 0).toString();
                writer.write("<IMAGE_SELECTED>\t" + sourceFilename +"\t</IMAGE_SELECTED>\n");
            }             
            for (int i = 0; i < guiTable.getRowCount(); i++) {
                writer.write("<LINK_GUI>\n");
                writer.write("\t<NAME>\t"+guiTable.getValueAt(i, 0).toString()+"\t</NAME>\n");
                writer.write("\t<LINK>\t"+guiTable.getValueAt(i, 1).toString()+"\t</LINK>\n");
                writer.write("\t<OUT>\t"+guiTable.getValueAt(i, 2).toString()+"\t</OUT>\n");
                writer.write("</LINK_GUI>\n");
            }
            writer.write("<CHAIN_FILE>\t" + chainLinkTF.getText() +"\t</CHAIN_FILE>\n");
            writer.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    
    /**
     * exportCode - write prototype code from entire chain to file
     */
    public void exportCode() {
        String linkfilename  = "code_Chain_"+refChainTF.getText()+".java";
        List<String> importList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        BufferedWriter writer;
        LinkClass gui;
        String rtnStr = "";
        String objStr;
        String mthStr;
        String prevObjStr;
        
        try {
            writer = new BufferedWriter(new FileWriter(linkfilename));

            // generate text for import statements from Links
            List<String> importChainList = genImportList();
            if (importChainList.size() > 0) {
                writer.write("//requires:\n");
                Collections.sort(importChainList);
            }
            for (int i = 0; i < importChainList.size(); i++) {
                writer.write("//import "+importChainList.get(i)+";\n");
            }

            // call the Links to retrieve a String of code from each Link
            // to create a method written out in text
            writer.write(genCodeString());

            objStr = "matImgSrc";
            sb.append(" doChain_"+refChainTF.getText()+"(Mat matImgSrc) {\n");
            for (int i = 0; i < guiLinks.size(); i++) {
                LinkGui thisLinkGui = guiLinks.get(i);
                gui = thisLinkGui.gui;
                prevObjStr = objStr;
                rtnStr = gui.getReturnStr();
                objStr = gui.getObjectStr();
                mthStr = gui.getMethodStr();
                sb.append("        "+rtnStr+" "+objStr+" = "+mthStr+"("+prevObjStr+");\n");
            }
            sb.append("        return "+objStr+";\n");
            sb.append("    }\n");
            writer.write("    public ");
            writer.write(rtnStr);
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {}    
    }
    /**
     * genImportList - generate a list of import statements required to run the prototype code
     * @return - List of Strings of packages to import for all the prototype code of the entire chain of links
     */
    public List<String> genImportList() {
        List<String> importChainList = new ArrayList<>();
        List<String> importGuiList   = new ArrayList<>();
        LinkClass gui;
        for (int i = 0; i < guiLinks.size(); i++) {
            LinkGui thisLinkGui = guiLinks.get(i);
            gui = thisLinkGui.gui;
            
            // append new import statements to this list of import statements
            importGuiList = gui.genImportList();
            if (importGuiList != null) {
                for (String importStr : importGuiList) {   
                    if (!importChainList.contains(importStr)) {
                        importChainList.add(importStr);
                    }
                }
            }
            else {
                System.out.println("WHOA, null import list returned from link:"+thisLinkGui.name);
            }
        }
        return importChainList;    
    }
    /**
     * genCodeString - generate a set of methods that represents this Chain's Link's prototype code
     */
    public String genCodeString() {
        LinkClass gui;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < guiLinks.size(); i++) {          
            LinkGui thisLinkGui = guiLinks.get(i);
            gui = thisLinkGui.gui;
            sb.append(gui.genCodeString(chainRefStr));
        }
        return sb.toString();
    }

    /**
     * main - method to allow command line application launch
     */
    public static void main(String[] args) {
        // Load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChainGui(args);
            }
        });
    }
}

/**
 * LinkGui - Class to hold information for a Link Gui
 */
class LinkGui {
    String    name;       // Name of Link Gui
    String    linkFile;   // File to read/write settings
    String    outputFile; // File to export Gui output to
    LinkClass gui;        // The Link Gui
    
    /**
     * Constructor - minimal non-null assingments
     */
    public LinkGui () {
        this.name = "";
        this.linkFile = "";
        this.outputFile = "";
    }
}

/**
 * JoinInput - Class to hold information for a join input
 */
class JoinInput {
    String    file;       // Name of input file
    String    key;        // Name of input key
    
    /**
     * Constructor - minimal non-null assingments
     */
    public JoinInput () {
        this.file = "";
        this.key = "";
    }
}
