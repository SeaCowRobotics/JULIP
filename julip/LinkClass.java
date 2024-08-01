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

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

/**
 *  Superclass for all JULIP Link Gui classes.
 *      parses command line arguments
 *      constructs a JPanel to hold file read/write operations
 *      parses link files
 *      loads image files
 */
public class LinkClass {

    public Map<String, String> myLinkMap;  // HashMap of key,value elements of link file settings
    public Map<String, String> cmdMap;     // HashMap of key,value elements of command line arguments
    public Map<String, String> defMap;     // HashMap of key,value elements of command line default arguments
    public List<String> inputKeys;         // List of input keys
    public String inputJoinStr;            // Command line message reporting necessary input keys
    public String codeFilename;            // File name to export prototype code to
    public Mat matImgSrc;                  // Imported image, in Mat format
    public Mat matImgDst;                  // Shown image, and image to export, from Mat format
    public JFrame frame;                   // Java Container for the Link Gui window
    public JLabel imgLabel;                // Java Container to hold image displayed by the Link Gui 

    public JButton    loadImageB;          // Button to load Image
    public JTextField textFieldImageIn;    // Container with filename of imported image.
    public JTextField textFieldLink;       // Container with filename of link gui settings.
    public JTextField textFieldImageOut;   // Container with filename of exported image.
    public JTextField textFieldMaskOut;    // TO BE eliminated

    public final int NOWIDTH  = 420;      // Default Image width when none is available
    public final int NOHEIGHT = 420;      // Default Image height when none is available
    
//    public boolean suppressRefreshImage;   // Flag to allow/disallow Containers to refreshImage()
    
    // List of methods common to all Link Gui's:
    //
    // These methods handle command line argument and link file parsing
    // and error checking of link file values.
    //    parseArgs()
    //    buildLinkMap()
    //    readLinkFile()
    //    intCheck()
    //    comboCheck()
    //
    // These methods handle Java Containers
    //    buildLinkPanel()
    //    setFrameLocation()
    //    loadSettings()
    //    loadImage()
    //
    // These methods are additional utilities
    //   exportCode()
    //
    // These methods are to be overridden by Link Gui subclass
    //   buildMyLinkMap()
    //   verifySettings()
    //   saveSettings(()
    //   saveImage()
    //   mapToSettings()
    //   refreshSettings()
    //   refreshImage()
    //   genImportList()
    //   genCodeString()
    //

    //--------------------------------------------------------------------------------------
    // start of parsing methods
    //
    /**
     * parseArgs - Parse the command line arguments. 
     *             Build the cmdMap from command line arguments.
     *             Call the method to build myLinkMap.
     *  @param className - Name of Class from which main method called
     *  @param args      - Array of command line arguments 
     */
    public void parseArgs(String className, String[] args) {
//        suppressRefreshImage = false;
        cmdMap = new HashMap<>();
        defMap = new HashMap<>();
        boolean setDefaults = false;
        boolean setImageIn  = false;
        boolean setImageOut = false;
        boolean setLinkFile = false;
        boolean setKey      = false;
        boolean setKeyInput = false;
        boolean setChainRef = false;
        String  thisKey = null;        
        String linkfilename = "";
        
        cmdMap.put("CLASS_NAME", className);
        
        // keep this for debugging
        //System.out.println(Arrays.toString(args));
        
        // If '--help' is any of the arguments then 
        // show proper command line usage and exit
        for (String arg : args) {
            //
            // Always good to have an option to show what the valid command arguments are.
            //  - the LinkFileName, if it exists is the first argument
            //  - all commands after -default will only override the native default settings of the Link Gui
            //  - all commands before -default will override all other settings
            //
            if (arg.equals("--help") || arg.equals("-help")) {
                System.out.println("Usage:");
                System.out.println("    [ ] = optional");  
                System.out.println(className + "    [<LinkFileName>] | [[-]-help] | [-default [...]]");
                System.out.println("    [-r <ChainReference>]");
                System.out.println("    [-i <ImageInputFileName>]");
                System.out.println("    [-o <ImageOutputFileName>]");
                System.out.println("    [-k <Key> <InputForKey>]");
                System.out.println("    [-f <LinkFileName>]"); // default-only
                if (inputJoinStr != null) {
                    System.out.println(inputJoinStr);
                }                
                System.exit(0);
            }
            if (arg.equals("-default")) {
                setDefaults = true;
            }
            if (setDefaults) {
                //
                // Look for -i command. 
                // The next arg after -i is the image input filename.
                //
                if (setImageIn) {
                    defMap.put("IMAGE_IN", arg);
                    setImageIn = false;
                }
                if (arg.equals("-i")) {
                    setImageIn = true;                            
                }
                //
                // Look for -o command. 
                // The next arg after -o is the image output filename.            
                //
                if (setImageOut) {
                    defMap.put("IMAGE_OUT", arg);
                    setImageOut = false;
                }
                if (arg.equals("-o")) {
                    setImageOut = true;                            
                }
                // Default  ONLY
                // Look for -f command. 
                // The next arg after -f is the link setting filename.            
                //
                if (setLinkFile) {
                    defMap.put("LINK_FILE", arg);
                    setLinkFile = false;
                }
                if (arg.equals("-f")) {
                    setLinkFile = true;                            
                }
                
            } else {
                //
                // Look for -r command. 
                // The next arg after -r is the chain reference.
                //
                if (setChainRef) {
                    cmdMap.put("CHAIN_REF", arg);
                    setChainRef = false;
                }
                if (arg.equals("-r")) {
                    setChainRef = true;                            
                }
                //
                // Look for -i command. 
                // The next arg after -i is the image input filename.
                //
                if (setImageIn) {
                    cmdMap.put("IMAGE_IN", arg);
                    setImageIn = false;
                }
                if (arg.equals("-i")) {
                    setImageIn = true;                            
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
                    cmdMap.put(thisKey, arg);                    
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
            }
            
        }
        // keep for debugging
        //System.out.println("defMap:"+defMap.toString());
        //System.out.println("cmdMap:"+cmdMap.toString());
        
        // Link file name can only be first argument.
        // All commands have a '-' prefix.
        if ((args.length > 0) && (args[0].charAt(0) != '-')) {
            linkfilename = args[0];
        }
        buildLinkMap(linkfilename);
    }    
    
    
    /**
     *  buildLinkMap - Parses link file into Map of key, value strings.
     *                 Link Gui's will parse the arguments as follows:
     *                   1. Link Gui's have native default values
     *                   2. The native default values are overwritten by command line arguments after the -default 
     *                   3. The linkfile overrides all native and command line defaults
     *                   4. The command line arguments (non-default) override linkfile and defaults
     *  @param  linkfilename - Name of link file to parse.
     *  @return Map of key, value strings
     */
    public void buildLinkMap(String linkfilename) {
    
        // 1. native default Link Gui settings are not here, they are in the Link Gui :)
        
        // initialize myLinkMap
        myLinkMap = new HashMap<>();      
        
        // 2. populate myLinkMap with the lowest priority settings - command line defaults
        for (String key : defMap.keySet()) {
            if (myLinkMap.containsKey(key)) {
                myLinkMap.replace(key, defMap.get(key));
            } else {
                myLinkMap.put(key, defMap.get(key)); 
            }
        }        
        
        // 3. populate/override myLinkMap with next priority settings - those from link file
        readLinkFile(linkfilename);

        //
        // 4. Populate/override any key,values of myLinkMap with
        // key,values from non-default command line arguments
        //
        for (String key : cmdMap.keySet()) {
            if (myLinkMap.containsKey(key)) {
                myLinkMap.replace(key, cmdMap.get(key));
            } else {
                myLinkMap.put(key, cmdMap.get(key)); 
            }
        }
        
        // Keep for debugging
        //System.out.println("myLinkMap:"+myLinkMap.toString());
    }    
    /**
     *  buildLinkMap - variant of buildLinkMap; has no arguments, returns an empty Map
     *  @return empty Map
     */
    public Map<String, String> buildLinkMap() {
        return new HashMap<>();
    }
    
    /**
     * readLinkFile - read and parse the key-values of a JULIP link file
     *   purpose of this method is to build the Map<String, String> myLinkMap which holds
     *   the setting values set by the user for the Link: sliders, comboboxes, textfields, etc.
     * @param linkfilename - name of JULIP link file to read
     * @return boolean     - true if file is readable and correct format, else false
     */
    public boolean readLinkFile(String linkfilename) {
        boolean status = true;
        int lineNum = 1;
    
        BufferedReader reader;
        // If there is a link file name provided in the command line arguments,
        // then try to parse it. 
        if (!linkfilename.equals("")) {
            try {
                reader = new BufferedReader(new FileReader(linkfilename));
                String line = reader.readLine();
                while (line != null) {
                    // Each line in file is expected to have two strings
                    // separated by some whitespace. The first string is
                    // the key, the second string is the value.
                    //
                    // If a user provides a non-link file as an argument, then
                    // if the file doesn't strictly fit the expected format, an
                    // IOException will be thrown.
                    String[] chunks = line.split("\\s+");
                    if (chunks.length != 0) {
                        if (chunks.length == 2) {
                            if (myLinkMap.containsKey(chunks[0])) {
                                myLinkMap.replace(chunks[0], chunks[1]);
                            } else {
                                myLinkMap.put(chunks[0], chunks[1]);
                            }
                            line = reader.readLine();
                            lineNum += 1;
                        }
                        else {
                            status = false;
                            throw new IOException("Invalid format in link file:" + linkfilename+"\n"+
                                                  "Line:" + lineNum + " " + line);
                        }
                    }
                }
                reader.close();
            }
            // For all IOExceptions, clear myLinkMap from any 
            // accumulated key, values before the exception was thrown.
            catch (IOException e) { 
                System.out.println(e);
                status = false;
            }
        }
        return status;
    }
    
    /**
     * intCheck - checks if a value for a given key to myLinkMap is valid type and range.
     * @param   keyStr             key to search for in myLinkMap 
     * @param   lowRange           minimum valid value
     * @param   highRange          maximum valid value
     * @param   replaceIfNotFound  option to replace myLinkMap key,value if key is not in myLinkMap
     * @param   replaceIfNotValid  option to replace myLinkMap key,value if value is invalid
     * @param   replacementValue   replacement value
     */
    public void intCheck(String keyStr, int lowRange, int highRange, boolean replaceIfNotFound, boolean replaceIfNotValid, int replacementValue) {
        // Check if the key is defined in the map.
        // If so,
        if (myLinkMap.containsKey(keyStr)) {
            // If the value of the key is nothing, 
            // then this isn't an invalid entry, it's a null entry and replace if flagged to do so
            if (myLinkMap.get(keyStr).trim().equals("")) {
                if (replaceIfNotValid) {
                    myLinkMap.replace(keyStr, Integer.toString(replacementValue));
                }
            }
            // So, there is a non-null key,value to investigate,
            // Check if it is an Integer and then if it's within the desired range
            else {
                try {
                    // parseInt will throw an exception if the value of the key,value is not an Integer
                    int value = Integer.parseInt(myLinkMap.get(keyStr));                
                    // If the value is out-of-range, print the problem,
                    // and replace the value if flagged to do so
                    if ((value < lowRange) || (value > highRange)) {
                        System.out.println(keyStr + ":" + myLinkMap.get(keyStr) + " is out of range:" + lowRange + "-" + highRange);
                        if (replaceIfNotValid) {
                            myLinkMap.replace(keyStr, Integer.toString(replacementValue));
                        }
                    }
                } catch (NumberFormatException e) { 
                    // We have some bogus assignment to the value, so print the problem,
                    // and replace the value if flagged to do so
                    System.out.println(keyStr + ":"+ myLinkMap.get(keyStr) + " is not integer");
                    if (replaceIfNotValid) {
                        myLinkMap.replace(keyStr, Integer.toString(replacementValue));
                    }
                }
            }
        } 
        // Key wasn't found, so create a key,value if flagged to do so
        else if (replaceIfNotFound) {
            myLinkMap.put(keyStr, Integer.toString(replacementValue));            
        }
    }
    // alternate method signature, with replace options defaulted to true
    public void intCheck(String keyStr, int lowRange, int highRange, int replacementValue) {
        intCheck(keyStr, lowRange, highRange, true, true, replacementValue);
    }
    
    /**
     * numberOrVoidCheck - checks if a value is numberic or null.
     * @param   keyStr             key to search for in myLinkMap 
     */
    public void numberOrVoidCheck(String keyStr) {
        if (myLinkMap.containsKey(keyStr)) {
            if (!myLinkMap.get(keyStr).trim().equals("")) {
                try {
                    double value = Double.parseDouble(myLinkMap.get(keyStr));
                } catch (NumberFormatException e) { 
                    System.out.println(keyStr + ":"+ myLinkMap.get(keyStr) + " is not a number");
                    myLinkMap.replace(keyStr, "");
                }
            }            
        }
    }
     
    /**
     * comboCheck - checks if a value for a given key to myLinkMap is valid type and range.
     * @param   keyStr             key to search for in myLinkMap 
     * @param   valueStrings       Array of Strings of valid values for given key
     * @param   replaceIfNotFound  option to replace myLinkMap key,value if key is not in myLinkMap
     * @param   replaceIfNotValid  option to replace myLinkMap key,value if value is invalid
     * @param   replacementIndex   index of array String of replacement value
     */
    public void comboCheck(String keyStr, String[] valueStrings, boolean replaceIfNotFound, boolean replaceIfNotValid, int replacementIndex) {
        if (myLinkMap.containsKey(keyStr)) {
            String myValue = myLinkMap.get(keyStr);
            boolean match = false;
            for (String value : valueStrings) {
                if (value.equals(myValue)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                System.out.println(keyStr + ":" + myValue + " is not valid ComboBox string");
                if (replaceIfNotValid) {
                    myLinkMap.replace(keyStr, valueStrings[replacementIndex]);
                }
            }
        } else if (replaceIfNotFound) {
            myLinkMap.put(keyStr, valueStrings[replacementIndex]);            
        }
    }
    // alternate method signature, with replace options defaulted to true
    public void comboCheck(String keyStr, String[] valueStrings, int replacementIndex) {
        comboCheck(keyStr, valueStrings, true, true, replacementIndex);
    }    
    //
    // end of parsing and error checking methods
    //-----------------------------------------------------------------------
    
    //--------------------------------------------------------------------------------------
    // start of Java Container methods
    //
    /**
     * buildLinkPanel - without an argument
     */
    public JPanel buildLinkPanel() {
        return buildLinkPanel(false);
    }
    
    /**
     * buildLinkPanel - construct a JPanel of Containers that are common to all link guis
     *                  These Containers handle file reading/writing operations.
     */
    public JPanel buildLinkPanel(boolean isJoin) {    
        //
        // JPanel of type GridLayout (2,4) to hold:
        //      Load Input Button     to reload source image or other input; no effect on settings
        //      Load Settings Button  to reload gui settings from file
        //      Save Settings Button  to save gui settings to file
        //      Save Output As Button to save images or other output files
        //      Export Code Button    to write code snippet to file
        //    
        JPanel linkPanel = new JPanel(new GridLayout(2, 4));
        
        //
        // Grid element 1,1
        // JButton to load input
        //
        // If a JoinClass...
        if (isJoin) {
            loadImageB = new JButton("Load Inputs");
        } 
        // If a LinkClass...
        else {
            loadImageB = new JButton("Load Input >");
        }
        Dimension buttonSize = loadImageB.getPreferredSize();
        loadImageB.setPreferredSize(new Dimension(80, (int)buttonSize.getHeight()));
        loadImageB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (loadImage(textFieldImageIn.getText())) {
                    loadImageB.setBackground(null);
                    refreshImage();                    
                } else {
                    loadImageB.setBackground(Color.RED);
                }
            }
        });
        linkPanel.add(loadImageB);
        
        //
        // Grid element 1,2
        // JTextField to hold name of input image file
        //
        textFieldImageIn = new JTextField(myLinkMap.get("IMAGE_IN"));
        // If a JoinClass...
        if (isJoin) {
            linkPanel.add(new JLabel());
        } 
        // If a LinkClas...
        else {
            linkPanel.add(textFieldImageIn);
        }

    
        //
        // Grid element 1,3
        // JButton to save image or output to file
        //
        JButton saveImageB = new JButton("Save Output As >");
        saveImageB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveImage();
            }
        });
        linkPanel.add(saveImageB);        
        
        //
        // Grid element 1,4
        // JTextField to hold name of output image file
        //
        textFieldImageOut = new JTextField(myLinkMap.get("IMAGE_OUT"));
        linkPanel.add(textFieldImageOut);

        //
        // Grid element 2,1
        // JButton to load gui settings from file
        //
        //linkPanel.add(new JLabel("Link File:"));
        JButton loadSettingsB = new JButton("Load Settings >");
        loadSettingsB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (loadSettings()) {
                    loadSettingsB.setBackground(null);
                } else {
                    loadSettingsB.setBackground(Color.RED);
                }
            }
        });
        linkPanel.add(loadSettingsB);

        //
        // Grid element 2,2
        // JTextField to hold name of link file
        //
        textFieldLink = new JTextField(myLinkMap.get("LINK_FILE"));
        linkPanel.add(textFieldLink);
        
        
        //
        // Grid element 2,3
        // JButton to save gui settings to file
        //
        JButton saveSettingsB = new JButton("< Save Settings");
        saveSettingsB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
            }
        });
        linkPanel.add(saveSettingsB);
    
        //
        // Grid element 2,4
        // JButton to write code snippet to file
        //
        JButton exportCodeB = new JButton("Export Code");
        exportCodeB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportCode(codeFilename, "");
            }
        });
        linkPanel.add(exportCodeB);
        
        return linkPanel;
    }
    
    /**
     * setFrameLocation - utility method to position link gui on computer screen
     */
    public void setFrameLocation(int x, int y) {
        frame.setLocation(x,y);
    }
    
    /**
     * setFrameName - utility method to set the frame name
     * @return - String to use for frame name
     */
    public String setFrameName() {
        String frameName = cmdMap.get("CLASS_NAME");
        if (cmdMap.containsKey("CHAIN_REF")) {
            frameName = frameName.concat(" ("+cmdMap.get("CHAIN_REF")+")");
        }
        return frameName;
    }
    
    /**
     * loadSettings - loads settings of link gui from file and refreshes gui.
     */
    public boolean loadSettings() {
        boolean status = readLinkFile(textFieldLink.getText());
        verifySettings();
        mapToSettings();
        refreshSettings();
        refreshImage();
        return status;
    }
    
    /**
     * loadImage - Import image from file; writes to matImgSrc.
     * @param  filename name of image file to import
     * @return   - true if file successfully read, else false
     */
    public boolean loadImage(String filename) {
        // split filename into base and extension
        String[] tokens = filename.split("\\.(?=[^\\.]+$)");
        if ((tokens.length > 1) && (tokens[1].equals("mat"))) {
            matImgSrc = MatHandler.readMat(filename);
        } else {
            matImgSrc = Imgcodecs.imread(filename);
        }
        //
        // The absolute vale of the channels param indicates the 
        // number of channels for the Mat.
        //            
        if (matImgSrc.empty()) {
            System.out.println("No image file:" + filename);
            matImgSrc = Mat.zeros(new Size(512, 512), CvType.CV_8U);
            return false;
        }        
        return true;
    }
        
    //
    // end of Java container methods
    //-----------------------------------------------------------------------
    
    //-----------------------------------------------------------------------------------
    //
    // Cluster of additional utilites
    //   exportCode()
    //
    
    /**
     * exportCode - Overrides method in LinkClass; write code to file.
     */
    public void exportCode(String codefilename, String reference) {
    
        if (codefilename == null) {
            System.out.println("WHOA, codefilename not set for exportCode method in "+this.getClass().getSimpleName());
            return;
        }
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(codefilename));
            List<String> importList = genImportList();
            if (importList.size() > 0) {
                writer.write("//requires:\n");
            }
            for (int i = 0; i < importList.size(); i++) {
                writer.write("//import "+importList.get(i)+";\n");
            }
            writer.write(genCodeString(reference));
            writer.close();
        } catch (IOException e) {}
    }
    //
    // end of additional utility methods
    //-----------------------------------------------------------------------    

    //-----------------------------------------------------------------------------------
    //
    // Cluster of LinkClass methods to be overridden  
    //   buildMyLinkMap()
    //   verifySettings()
    //   saveSettings()
    //   saveImage()
    //   mapToSettings()
    //   refreshSettings()
    //   refreshImage()
    //   genImportList()
    //   genCodeString()
    //
    
    /**
     * buildMyLinkMap - native default values for Link Gui.
     *           creates Link Gui's defaultMap and copies to myLinkMap
     */
    public void buildMyLinkMap() {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
    }
    
    /**
     * verifySettings - error check myLinkMap values.
     *           error corrects myLinkMap values that may be wrong format or out-of-range
     */
    public void verifySettings() {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
    }
    
    /**
     * saveSettings - To be overridden by subclass; saves settings of link gui to file.
     */
    public void saveSettings() {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
    }
     
    public void setImageInTF(List<String> args) {
    }
     
    /**
     * saveImage - To be overridden by subclass; saves output image to file.
     */
    public void saveImage() {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
    }
     
    /**
     * mapToSettings - To be overridden by subclass; update gui settings from myLinkMap.
     */
    public void mapToSettings() {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
    }
     
    /**     
     * refreshSettings - To be overridden by subclass; and update gui settings.
     */
    public boolean refreshSettings() {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
        return false;
    }
    
    /**
     * refreshImage - To be overridden by subclass; recalculate algorithm and refresh shown image.
     */
    public void refreshImage() {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
    }
    
    /**
     * genImportList - To be overridden by subclass; generate list of import statements.
     */
    public List<String> genImportList() {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
        return null;
    }

    String returnStr;
    String objectStr;
    String methodStr;
    public String getReturnStr() { return returnStr; } // return type that this Link outputs for code export
    public String getObjectStr() { return objectStr; } // return object String for ChainGui code export
    public String getMethodStr() { return methodStr; } // return method name this Link outputs for code export

    /**
     * genCodeString - To be overridden by subclass; generate method of code.
     */
    public String genCodeString(String reference) {
        System.out.println("Whoa, "+this.getClass().getSimpleName()+" did not overwrite "+ new Object(){}.getClass().getEnclosingMethod().getName()+"().");
        return "";
    }
    
    //
    //  end of LinkClass methods to be overridden
    //-----------------------------------------------------------------------------------
      
}

/**
 * IntegerValid - Object to hold an integer along with a booleans for valid status.
*/
class IntegerValid {
    int value = 0;          // holds integer value
    boolean found = false;  // status indicator if value was found in HashMap
    boolean valid = false;  // status indicator if value was within acceptable range limits
}
