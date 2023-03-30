package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
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
 * MeshGui - Class to handle a mesh of chain link gui's.
 */
public class MeshGui {
    
    private Map<String, String> myMeshMap;
    private List<String>   sourceImages = new ArrayList<>();    
    private NodeMesh myMesh;
    
    private List<ChainGui> chainGuis;
    private List<Integer>  chainNode;
    private boolean buildBeforeUpdate = true;
    
    private JTable imgTable;
    private JTable nodeTable;

    private DefaultTableModel nodeTM;
    // These correspond to the nodeTM
    private final int INPUT_COLUMN     = 0;
    private final int PROCESS_COLUMN   = 1;
    private final int REFERENCE_COLUMN = 2;
    private final int OUTPUT_COLUMN    = 3;
    
    private JTextField refMeshTF;   // JTextField to hold mesh reference. Ex: "jewel"
    private JTextField fileMeshTF;  // JTextField to hold mesh settings file name. Ex: "mesh_jewel.txt"
    private String     meshRefStr;  // Last known chain reference
    private JTextField refChainTF;  // JTextField to hold chain reference
    private JTextField refJoinTF;   // JTextField to hold join reference
    
    private JButton refMeshB;
    private JButton forkB;
    private JButton chainB;
    private JButton joinB;
    private JButton terminusB;
    private JButton killB;
    private JButton verifyB;
    private JButton exportCodeB;
    
    private JFrame frame;
    private JPanel myPanel;
    private JLabel imgLabel;
    private JScrollPane imgSP;              // JScrollPane to hold image
    private int frameHeightMinusImage = 0;
    
    private Mat matImgSrc;
    private Image img;
    
    private boolean suppressListeners = false;
    
    private final int NOWIDTH  = 420;
    private final int NOHEIGHT = 420;
    
    // Methods within MeshGui
    //
    //  MeshGui() - MeshGui constructor
    //      
    //  validateFork()
    //  validateJoin()
    //  validateChain()
    //  validateTerminus()
    //  validateKill()
    //  validateVerify()
    //
    //  createFork()
    //  createJoin()
    //  createJoinRef()
    //  createChain()
    //  createTerminus()
    //  createKill()
    //  createVerify()
    //
    //  resizeFrame()       - utility method to resize this window
    //  populateNodeTable() - update node JTable from MyMesh
    //  getImage()          - method to load image from ChainGui frame
    //  parseArgs()         - Parse the command line arguments
    //  getMesh()           - parse mesh file
    //  updateRef()         - update link settings file names based on an update of the chain reference
    //  updateGuis()        - instantiate guis from chain nodes in myMesh
    //  saveSettings()      - write chain gui settings to file
    //  exportCode()        - write code to file from all chains and links within this mesh
    //  main()              - method to allow command line application lauch
     
    /**
     *  MeshGui constructor 
     *  @param args - List of command line arguments
     */
    public MeshGui(String[] args) {
    
        // MeshGui architecture:
        // 
        // Does stuff...
        //    
        chainGuis = new ArrayList<>();
        chainNode = new ArrayList<>();        
        parseArgs(this.getClass().getSimpleName(), args);
        
        // Create and set up the window.
        frame = new JFrame("MeshGui");
        imgLabel = new JLabel();        
        imgSP = new JScrollPane(imgLabel);
        imgSP.setPreferredSize(new Dimension(400,400));                
        myPanel = new JPanel();
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.PAGE_AXIS));

        //------------------------------------- refControlPanel -------------------------
        //        
        // JPanel with Flow Layout
        //   JButton - update chain reference
        //   JTextField - text of chain reference
        JPanel refControlPanel = new JPanel();
        refMeshB = new JButton("Update Mesh Reference");
        refMeshTF = new JTextField();
        refMeshTF.setPreferredSize(new Dimension(120, 25));
        refMeshTF.setText(myMeshMap.get("MESH_REFERENCE"));
        refMeshTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refMeshB.setBackground(null);
            }
        });
        refMeshB.setBackground(Color.GREEN);
        meshRefStr = refMeshTF.getText();                    // save mesh reference, in case TextField changes
        refMeshB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateRef();
                refMeshB.setBackground(Color.GREEN);
            }
        });
        refControlPanel.add(refMeshB);
        refControlPanel.add(refMeshTF);
        myPanel.add(refControlPanel);
        
        //------------------------------------- image JTable and JScrollPane -------------------------
        //        
        // Create TableModel with non-editable cells
        DefaultTableModel imageTM = new DefaultTableModel() {
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
//                    resizeFrame();               
                }
            }
        });
        imageTM.addColumn("Image");
        for (int i = 0; i < sourceImages.size(); i++) {
            imageTM.addRow(new Object[]{sourceImages.get(i)});                                              
        }
        JScrollPane sourceSP = new JScrollPane(imgTable);
        sourceSP.setPreferredSize(new Dimension(NOWIDTH,100));
        if (myMeshMap.containsKey("IMAGE_SELECTED")) {
            suppressListeners = true;
            int idx = sourceImages.indexOf(myMeshMap.get("IMAGE_SELECTED"));
            if (idx != -1) {
                imgTable.setRowSelectionInterval(idx, idx);
            } else {
                System.out.println("IMAGE_SELECTED:"+myMeshMap.get("IMAGE_SELECTED")+" is not in list of IMAGES");
            }
            suppressListeners = false;
        }
        myPanel.add(sourceSP);

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
        myPanel.add(imageControlPanel);
        
        //------------------------------------- node JTable and JScrollPane -------------------------
        //                
        // Create node JTable with non-editable cells.
        //
        nodeTM = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        nodeTable = new JTable(nodeTM);
        // The JTable has 4 columns, in order:
        nodeTM.addColumn("Input Node");
        nodeTM.addColumn("Node Process");
        nodeTM.addColumn("Reference");
        nodeTM.addColumn("Output Node");
        //
        // Add node JTable into JScrollPane
        //
        JScrollPane nodeSP = new JScrollPane(nodeTable);
        nodeSP.setPreferredSize(new Dimension(NOWIDTH,100));
        myPanel.add(nodeSP);
        
    
        //--------------------------------------------------------------------------------------
        //
        // JPanel of type GridLayout (3-row,4-col) to hold:
        //      Fork Button     to create new nodes with inputs tied to a common node output
        //      Join Button     to create a new node with input tied to multiple node outputs
        //      Chain Button    to create a chain with an end node tied to a start node
        //      Terminus Button to select which node is the terminal node
        //      Kill Button     to delete a node
        //      Verify Button   to check if the mesh is complete
        //    
        JPanel nodeControlPanel = new JPanel(new GridLayout(3, 4));
        
        //
        // Grid element 1,1
        //
        nodeControlPanel.add(new JLabel());
                
        //
        // Grid element 1,2
        // JButton fork - to create new nodes with inputs tied to a common node output
        //
        forkB = new JButton("Fork");
        forkB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonBackgroundsNull();
                if (validateFork()) {
                    populateNodeTable();
                } else {
                    forkB.setBackground(Color.RED);
                }
            }
        });
        nodeControlPanel.add(forkB);
    
        //
        // Grid element 1,3
        // JButton join - to create a new node with input tied to multiple node outputs
        //
        joinB = new JButton("Join");
        joinB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonBackgroundsNull();
                if (validateJoin()) {
                    populateNodeTable();
                } else {
                    joinB.setBackground(Color.RED);
                }
            }
        });
        nodeControlPanel.add(joinB);
        
        //
        // Grid element 1,4
        // JTextField to hold name of link file
        //
        refJoinTF = new JTextField();
        nodeControlPanel.add(refJoinTF);
        
        //
        // Grid element 2,1
        // JTextField to hold name of link file
        //
        refChainTF = new JTextField();
        nodeControlPanel.add(refChainTF);

        //
        // Grid element 2,2
        // JButton chain - to create a chain with an end node tied to a start node
        //
        chainB = new JButton("Chain");
        chainB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonBackgroundsNull();            
                if (validateChain()) {
                    populateNodeTable();
                } else {
                    chainB.setBackground(Color.RED);
                }
            }
        });
        nodeControlPanel.add(chainB);        
        
        //
        // Grid element 2,3
        // JButton terminus - to create the terminal node for this mesh
        //
        terminusB = new JButton("Terminus");
        terminusB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonBackgroundsNull();
                if (validateTerminus()) {
                    populateNodeTable();
                } else {
                    terminusB.setBackground(Color.RED);
                }
            }
        });
        nodeControlPanel.add(terminusB);
    
        //
        // Grid element 2,4
        //
        nodeControlPanel.add(new JLabel());
    
        //
        // Grid element 3,1
        //
        nodeControlPanel.add(new JLabel());
    
        //
        // Grid element 3,2
        // JButton kill - to remove a Node process and its output dependencies
        //
        killB = new JButton("Kill");
        killB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonBackgroundsNull();            
                if (validateKill()) {
                    populateNodeTable();                    
                } else {
                    killB.setBackground(Color.RED);
                }
            }
        });
        nodeControlPanel.add(killB);
    
        //
        // Grid element 3,3: label
        // JButton verify - check if the mesh node is completer
        //
        verifyB = new JButton("Verify");
        verifyB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonBackgroundsNull();            
                if (validateVerify()) {
                    verifyB.setBackground(Color.GREEN);
                } else {
                    verifyB.setBackground(Color.RED);
                }
            }
        });
        nodeControlPanel.add(verifyB);
        
        //
        // Grid element 3,4
        //
        nodeControlPanel.add(new JLabel());
    
        
        myPanel.add(nodeControlPanel);
                
        
        //------------------------------------- chainControlPanel -------------------------
        //
        // JPanel with Flow Layout
        //   JButton - to bring up all the chains and links in the mesh
        //   JButton - to save chainlink file
        //   JTextField - chainlink filename holder
        //   JButton - export code
        JPanel chainControlPanel = new JPanel();
        JButton updateB = new JButton("Update Gui's");
        JButton saveSettingsB = new JButton("Save Settings>");
        fileMeshTF = new JTextField();
        exportCodeB = new JButton("Export Code");

        updateB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updateGuis()) {
                    updateB.setBackground(null);
                    exportCodeB.setBackground(Color.GREEN);
                } else {
                    System.out.println("No image selected in Image Table to update Guis.");
                    updateB.setBackground(Color.RED);
                }
            }
        });        

        saveSettingsB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
            }
        });
        
        fileMeshTF.setPreferredSize(new Dimension(120, 25));
        if (myMeshMap.containsKey("MESH_FILE")) {
            fileMeshTF.setText(myMeshMap.get("MESH_FILE"));
        }

        exportCodeB.setBackground(Color.YELLOW);
        exportCodeB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportCode();
            }
        });
        chainControlPanel.add(updateB);
        chainControlPanel.add(saveSettingsB);
        chainControlPanel.add(fileMeshTF);
        chainControlPanel.add(exportCodeB);
        myPanel.add(chainControlPanel);
        
        
        
        // Get Image based on imgTable selection
        getImage();
        
        frame.add(myPanel, BorderLayout.PAGE_START);
        frame.add(imgSP, BorderLayout.PAGE_END);
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                Dimension sizeSP = imgSP.getSize();
                imgSP.setPreferredSize(new Dimension(sizeSP.width, frame.getSize().height - frameHeightMinusImage));
            }
        });        
        frame.pack();
        frameHeightMinusImage = frame.getSize().height - imgSP.getSize().height;        
//        resizeFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);       
        frame.setLocation(0,0);
        frame.setVisible(true);
                
        populateNodeTable();
        // Unless this is a new mesh, do a verification check
        if (myMesh.nodes.size() > 1) {
            verifyB.doClick();
        }
    }

    private void buttonBackgroundsNull() {
        forkB.setBackground(null);
        joinB.setBackground(null);
        chainB.setBackground(null);
        terminusB.setBackground(null);
        killB.setBackground(null);
        verifyB.setBackground(null);
    }
    
    //-------------------------------------------
    // begin validate<Stuff> methods
    //
    
    /**
     * validateFork    - create a Fork node, with error-checking at node JTable. 
     * @return boolean - true if error-checking passes
     */
    private boolean validateFork() {
        int[] rowsSelected = nodeTable.getSelectedRows();
        // Only one node can be selected to create a new fork
        if (rowsSelected.length != 1) {
            System.out.println("Select only one row for Fork process. Fail.");
            return false;
        }
        int idx = rowsSelected[0];
        // Only empty and Fork nodes can be used to fork new nodes
        String process = nodeTable.getValueAt(idx,PROCESS_COLUMN).toString();
        if (!process.equals("") & !process.equals("FORK")) {
            System.out.println("Select only empty('') or 'FORK' nodes for Fork process. Fail.");
            return false;
        }
        // create a new fork from the selected input source
        int node = Integer.parseInt(nodeTable.getValueAt(idx,INPUT_COLUMN).toString());
        return createFork(node);
    }
    
    /**
     * validateJoin    - create a Join node, with error-checking at node JTable. 
     * @return boolean - true if error-checking passes
     */
    private boolean validateJoin() {
        int[] rowsSelected = nodeTable.getSelectedRows();
        // At least two nodes must be selected to create a join
        if (rowsSelected.length == 0) {
            System.out.println("Select one row to add a reference to a Join.");
            System.out.println("Select two or more rows for Join process. Fail.");
            return false;
        }
        // When multiple rows are selected, an attempt will be made to create a Join node
        if (rowsSelected.length > 1) {
            int joinOutput = -1;
            List<Integer> inputs = new ArrayList<>();
            // Only empty and Join nodes can be used to create/append a join
            for (int i = 0; i < rowsSelected.length; i++) {
                String process = nodeTable.getValueAt(rowsSelected[i],PROCESS_COLUMN).toString();
                if (!process.equals("") & !process.equals("JOIN")) {
                    System.out.println("Select only empty('') or 'JOIN' nodes for Join process. Fail.");
                    return false;
                } else {
                    // Get the Output Node for the selected row
                    // We need to make sure there's only zero or one outputs selected
                    String outStr = nodeTable.getValueAt(rowsSelected[i],OUTPUT_COLUMN).toString();
                    if (!outStr.equals("")) {
                        if ((joinOutput > -1) && (joinOutput != Integer.parseInt(outStr))) {
                            System.out.println("Select none or only one Join output. Fail.");
                            return false;
                        }
                        joinOutput = Integer.parseInt(outStr);
                    }
                    // Add the Input for the selected node to list of inputs to the Join node
                    inputs.add(Integer.parseInt(nodeTable.getValueAt(rowsSelected[i],INPUT_COLUMN).toString()));
                }
            }
            // create/append a join from the selected rows
            return createJoin(inputs, joinOutput);
        }
        // When a single row is selected, the reference will be updated to this Join input
        else {
            String process = nodeTable.getValueAt(rowsSelected[0],PROCESS_COLUMN).toString();
            if (!process.equals("JOIN")) {
                System.out.println("Select 'JOIN' node to update reference. Fail.");                
                return false;
            } else {
                int input = Integer.parseInt(nodeTable.getValueAt(rowsSelected[0],INPUT_COLUMN).toString());
                return createJoinRef(input, refJoinTF.getText());
            }
        }
    }
    
    /**
     * validateChain   - create a Chain node, with error-checking at node JTable. 
     * @return boolean - true if error-checking passes
     */
    private boolean validateChain() {
        int[] rowsSelected = nodeTable.getSelectedRows();
        // Only one node can be selected to create a new fork
        if (rowsSelected.length != 1) {
            System.out.println("Select only one row for Chain process. Fail.");
            return false;
        }
        int idx = rowsSelected[0];
        // Only empty nodes can be used to chain
        String process = nodeTable.getValueAt(idx,PROCESS_COLUMN).toString();
        if (!process.equals("")) {
            System.out.println("Select only empty nodes('') for Chain process. Fail.");
            return false;
        }
        String name = refChainTF.getText().trim();
        if (name.equals("")) {
            System.out.println("No entry made in chain text field for Chain process. Fail.");
            return false;
        }
        int input = Integer.parseInt(nodeTable.getValueAt(idx,INPUT_COLUMN).toString());
        return createChain(input, name);
    }

    /**
     * validateTerminus - create a Terminus, with error-checking at node JTable. 
     * @return boolean - true if error-checking passes
     */
    private boolean validateTerminus() {
        int[] rowsSelected = nodeTable.getSelectedRows();
        // Only one node can be selected to create a new fork
        if (rowsSelected.length != 1) {
            System.out.println("Select only one row for Terminus process. Fail.");
            return false;
        }
        int idx = rowsSelected[0];
        // Only empty nodes can be used to chain
        String process = nodeTable.getValueAt(idx,PROCESS_COLUMN).toString();
        if (!process.equals("")) {
            System.out.println("Select only empty('') nodes for Terminus process. Fail.");
            return false;
        }
        // create a new fork from the selected input source
        int node = Integer.parseInt(nodeTable.getValueAt(idx,INPUT_COLUMN).toString());
        return createTerminus(node);
    }
    
    /**
     * validateKill - create a Terminus, with error-checking at node JTable. 
     * @return boolean - true if error-checking passes
     */
    private boolean validateKill() {
        int[] rowsSelected = nodeTable.getSelectedRows();
        // Only one node can be selected to create a new fork
        if (rowsSelected.length != 1) {
            System.out.println("Select only one row for Kill process. Fail.");
            return false;
        }
        int idx = rowsSelected[0];
        // Only empty nodes can be used to chain
        String process = nodeTable.getValueAt(idx,PROCESS_COLUMN).toString();
        if (process.equals("")) {
            System.out.println("Do not select empty nodes for Kill process. Fail.");
            return false;
        }
        // It's possible to kill a terminus module, which would have Output Node
        // value of 'N' in the table. In that special case, set output to -1.
        String outputStr = nodeTable.getValueAt(idx,OUTPUT_COLUMN).toString();
        int output = -1;
        if (!outputStr.equals("N")) {
            output = Integer.parseInt(outputStr);
        }
        int input = Integer.parseInt(nodeTable.getValueAt(idx,INPUT_COLUMN).toString());
        // kill a node path
        return createKill(input, output);
    }
    
    /**
     * validateVerify - 
     * @return boolean - true if error-checking passes
     */
    private boolean validateVerify() {
        return createVerify();
    }
    //
    // end validate<Stuff> methods
    //-------------------------------------------
     

    //-------------------------------------------
    // begin create<Stuff> methods
    //
    
    /**
     * createFork - create a Fork, with error-checking at myMesh nodes.
     * @param input    - node input identifier to create Fork
     * @return boolean - true if error-checking passes
     */
    private boolean createFork(int input) {
        if (myMesh == null) {
            System.out.println("Whoa, attempting to createFork but myMesh is null. Fail.");
            return false;
        }
        if (input >= myMesh.nodes.size()) {
            System.out.println("Whoa, attempting to createFork but input:"+input+" is not less than mesh size:"+myMesh.nodes.size()+". Fail.");
            return false;
        }
        boolean nodeFound = false;
        // Find the node with the identified input
        for (int i = 0; i < myMesh.nodes.size(); i++) {
            Node node = myMesh.nodes.get(i);
            if (node.inputs.contains(input)) {
                // node type must either be "FORK" or ""
                // in case of "FORK"...
                if (node.type.equals("FORK")) {
                    // create new node with input pointed to forked node
                    // add new node to list of outputs of forked node
                    myMesh.maxInput += 1;
                    myMesh.nodes.add(new Node(myMesh.maxInput));
                    node.outputs.add(myMesh.maxInput);
                } 
                // node type must either be "FORK" or ""
                // in case of ""...
                else if (node.type.equals("")) {
                    // If this node is an output of a forked node then the fork should
                    // be done on that forked node instead of this one.
                    boolean makeFork = true;
                    for (int j = 0; j < myMesh.nodes.size(); j++) {
                        Node onode = myMesh.nodes.get(j);
                        if (onode.outputs.contains(input) && onode.type.equals("FORK")) {
                            if (onode.inputs.size() != 1) {
                                System.out.println("Whoa, attempting to createFork on:"+input+" but found a forking node without 1 input. Fail.");
                                return false;
                            }
                            createFork(onode.inputs.get(0));
                        }
                    }               
                    if (makeFork) {
                        // create two new nodes with inputs pointed to forked node
                        // add new nodes to list of outputs of forked node
                        myMesh.maxInput += 1;
                        myMesh.nodes.add(new Node(myMesh.maxInput));
                        node.outputs.add(myMesh.maxInput);
                        myMesh.maxInput += 1;
                        myMesh.nodes.add(new Node(myMesh.maxInput));                        
                        node.outputs.add(myMesh.maxInput);
                        // set type of (now) forked node to FORK
                        node.type = "FORK";                    
                    }
                } 
                // wrong node type
                else {
                    System.out.println("Whoa, attempting to createFork but input:"+input+" goes to a non-null, non-FORK node:"+i+". Fail.");
                    return false;
                }
                nodeFound = true;
                break;
            }
        }
        return nodeFound;        
    }
    
    /**
     * createJoin - create a Join node, with error-checking at myMesh nodes.
     * @param inputs     - List of node input identifiers to create Join
     * @param joinOutput - node output identifier for Join node output
     * @return boolean   - true if error-checking passes
     */
    private boolean createJoin(List<Integer> inputs, int joinOutput) {
        // Check to make sure that if there is a joinOutput found (>-1) then
        // at most only one node has it as its output.
        myMesh.sortInputs();
        Node joinNode = null;
        if (joinOutput >= 0) {            
            for (int i = 0; i < myMesh.nodes.size(); i++) {
                Node node = myMesh.nodes.get(i);
                if (node.outputs.contains(joinOutput)) {
                    if (joinNode != null) {
                        System.out.println("Whoa, attempting to createJoin but output:"+joinOutput+" found in two nodes:"+joinNode+","+i+". Fail.");
                        return false;
                    } else {
                        joinNode = node;
                    }
                }
            }
            if (joinNode == null) {
                System.out.println("Whoa, attempting to createJoin but no node has selected output:"+joinOutput+". Fail.");
                return false;
            }
        }
        // Sort the inputs by increasing index
        Collections.sort(inputs);
        // If no join node was found then convert the
        // first empty node into Join node 
        if (joinNode == null) {
            int firstInput = inputs.get(0);
            for (int j = 0; j < myMesh.nodes.size(); j++) {
                Node inode = myMesh.nodes.get(j);
                if (inode.inputRefs.containsKey(firstInput)) {
                    joinNode = inode;
                    break;
                }
            }
            joinNode.type = "JOIN";
            myMesh.maxInput += 1;
            myMesh.nodes.add(new Node(myMesh.maxInput));
            joinNode.outputs.add(myMesh.maxInput);
            for (int i = 0; i < inputs.size(); i++) {
                if (!joinNode.inputRefs.containsKey(inputs.get(i))) {
                    joinNode.inputRefs.put(inputs.get(i), "");
                 }
            }
        } 
        // join node was found, don't need to create a new node
        else {
            for (int i = 0; i < inputs.size(); i++) {
                if (!joinNode.inputRefs.containsKey(inputs.get(i))) {
                    joinNode.inputRefs.put(inputs.get(i), "");
                 }
            }
        }
        // delete all empty nodes with input from the inputs list
        myMesh.sortInputs();
        for (int j = 0; j < myMesh.nodes.size(); j++) {
            Node xnode = myMesh.nodes.get(j);
            if ((xnode.inputs.size() == 1) && 
                (inputs.contains(xnode.inputs.get(0))) &&
                (xnode.type.equals(""))
               ) {
                myMesh.nodes.remove(j);
                j -= 1;
            }
        }
        return true;
    }
    
    /**
     * createJoinRef - update a reference to a Join input, with error-checking at myMesh nodes.
     * @param input      - node input identifier to create Chain
     * @param ref        - reference for created Chain node
     * @return boolean   - true if error-checking passes
     */
    private boolean createJoinRef(int input, String ref) {
        // Find the join node with the given input
        for (int i = 0; i < myMesh.nodes.size(); i++) {
            Node node = myMesh.nodes.get(i);
            if (node.inputRefs.containsKey(input)) {
                node.inputRefs.replace(input, refJoinTF.getText());
                return true;                
            }
        }
        System.out.println("Whoa, could not find node with input:"+input+". Fail.");
        return false;    
    }
    
    
    /**
     * createChain - create a Chain node, with error-checking at myMesh nodes.
     * @param input      - node input identifier to create Chain
     * @param name       - reference for created Chain node
     * @return boolean   - true if error-checking passes
     */
    private boolean createChain(int input, String name) {
        // Find the node with the given input
        int idx;
        for (idx = 0; idx < myMesh.nodes.size(); idx++) {
            Node node = myMesh.nodes.get(idx);
            if (node.inputRefs.containsKey(input)) {
                if (!node.type.equals("")) {
                    System.out.println("Whoa, node with input:"+input+" is type:"+node.type+" not type:''. Fail.");
                    return false;
                }
                // create a new node to take output of chain node
                myMesh.maxInput += 1;
                myMesh.nodes.add(new Node(myMesh.maxInput));
                // update the chain node's parameters
                node.type = "CHAIN";
                node.outputs.add(myMesh.maxInput);
                node.inputRefs.replace(input, name);
                break;
            }
        }
        if (idx == myMesh.nodes.size()) {
            System.out.println("Whoa, attempting to createChain but no node found with input:"+input+". Fail.");
            return false;
        }
        return true;
    }
    
    /**
     * createTerminus - create a Terminus node, with error-checking at myMesh nodes.
     * @param input      - node input identifier to create Terminus
     * @return boolean   - true if error-checking passes
     */
    private boolean createTerminus(int input) {
        // Make sure there is only one empty node in myMesh
        // and it is the only node with the given input identifier. 
        // This empty node will become the terminus node.
        int inputCount = 0;
        int emptyCount = 0;
        Node terminus = null;
        for (int i = 0; i < myMesh.nodes.size(); i++) {
            Node node = myMesh.nodes.get(i);
            if (node.inputRefs.containsKey(input)) {
                inputCount += 1;
            }
            if (node.type.equals("")) {
                emptyCount += 1;
            }
            if (node.inputRefs.containsKey(input) &&
                node.type.equals("")) {
                terminus = node;
            }
        }
        if (terminus == null) {
            System.out.println("Whoa, atteming to createTerminus but no empty node found with input:"+input+". Fail.");
            return false;
        }
        if (inputCount > 1) {
            System.out.println("Whoa, atteming to createTerminus but multiple nodes have input:"+input+". Fail.");
            return false;
        }
        if (emptyCount > 1) {
            System.out.println("Whoa, atteming to createTerminus but multiple empty nodes found. Fail.");
            return false;
        }
        terminus.type = "TERMINUS";        
        terminus.outputs.clear();
        terminus.outputs.add(-1); // special case for terminus nodes
        return true;
    }
    
    /**
     * createKill - kill a node path, with error-checking at myMesh nodes.
     * @param input      - node input identifier of node path to kill
     * @param output     - node output identifier of node path to kill
     * @return boolean   - true if error-checking passes
     */
    private boolean createKill(int input, int output) {
    
        // Start a list of outputs to kill
        List<Integer> killInputs = new ArrayList<>();
        
        // The targetted node that owns the node path will likely be made into
        // and empty node. The exception for this is if targetted node is a fork node.
        // Descendants of the targetted node will be killed off.
        boolean makeEmpty = true;
        
        // Find the node with the node path to kill
        for (int i = 0; i < myMesh.nodes.size(); i++) {
            Node node = myMesh.nodes.get(i);
            if (node.inputRefs.containsKey(input) && node.outputs.contains(output)) {
                if (node.type.equals("FORK")) {
                    // kill the node path of the fork node
                    node.outputs.remove(Integer.valueOf(output));
                    // kill descendants of the fork node
                    killInputs.add(output);
                    // if the fork node has no paths left then
                    // convert the fork node into an empty node
                    if (node.outputs.size() == 0) {
                        node.type = "";
                    }
                    makeEmpty = false;
                } 
                else if (node.type.equals("JOIN")) {
                    // set the input to be killed off
                    killInputs.add(input);                    
                    // don't automatically kill off the output node
                } 
                else {
                    killInputs.add(input);
                    if (output > 0) { killInputs.add(output); }
                }
                break;
            }
        }
        if (killInputs.size() == 0) {
            System.out.println("Whoa, attempting to createKill but found no node with input:"+input+" and output:"+output+". Fail.");
            return false;                
        }

        while (killInputs.size() > 0) {
            int killme = killInputs.get(0);
            // iterate through kill list, find node with given input
            for (int j = 0; j < myMesh.nodes.size(); j++) {
                Node xnode = myMesh.nodes.get(j);
                if (xnode.inputRefs.containsKey(killme)) {
                    // In the case of a join node,  and 
                    if (xnode.type.equals("JOIN")) {
                        // If there is only one input to the join node,
                        // convert the join node to an empty node and kill
                        // off the descendants
                        if (xnode.inputs.size() == 1) {
                            xnode.type = "";           
                            killInputs.add(xnode.outputs.get(0));
                            xnode.outputs.clear();
                            
                        } else {
                            xnode.inputRefs.remove(killme);                        
                            myMesh.nodes.add(new Node(killme));
                        }
                    } 
                    else {
                        // all descendants of this path are to be wiped out
                        killInputs.addAll(xnode.outputs);
                                        
                        // special case if this is the node that owns the node path
                        // and it wasn't a fork node, then convert it to an empty node
                        if (makeEmpty) {
                            xnode.type = "";
                            xnode.outputs.clear();
                        }
                        else {
                            myMesh.nodes.remove(xnode);
                        }
                    }

                    makeEmpty = false;
                    break;
                }
            }
            killInputs.remove(0);                
        }
        // Recalculate the max input to myMesh after node path deletions
        myMesh.calcMaxInput();                
        return true;
    }
    
    /**
     * createVerify - verify the mesh is complete. 
     * @return boolean - true if error-checking passes
     */
    private boolean createVerify() {
        int terminusCount = 0;
        int emptyCount = 0;
        Node terminus = null;
        for (int i = 0; i < myMesh.nodes.size(); i++) {
            Node node = myMesh.nodes.get(i);
            if (node.inputRefs.size() == 0) {
                System.out.println("Whoa, attempting to verify mesh; a "+node.type+" has no input. Fail.");
                return false;
            }
            if (node.type.equals("TERMINUS")) {
                terminusCount += 1;
                if (node.inputRefs.size() > 1) {
                    System.out.println("Whoa, attempting to verify mesh; a terminus node has multiple ("+node.inputs.size()+") inputs. Fail");
                    return false;
                }
                if (node.outputs.size() > 1) {
                    System.out.println("Whoa, attempting to verify mesh; a terminus node has multiple ("+node.outputs.size()+") outputs. Fail");
                    return false;
                }
            }
            else if (node.type.equals("")) {
                emptyCount += 1;
                if (node.inputRefs.size() > 1) {
                    System.out.println("Whoa, attempting to verify mesh; an empty node has multiple ("+node.inputs.size()+") inputs. Fail");
                    return false;
                }
                if (node.outputs.size() > 0) {
                    System.out.println("Whoa, attempting to verify mesh; an empty node has ("+node.outputs.size()+") outputs. Fail");
                    return false;
                }
            }
            else if (node.type.equals("FORK")) {
                if (node.inputRefs.size() > 1) {
                    System.out.println("Whoa, attempting to verify mesh; a fork node has multiple ("+node.inputs.size()+") inputs. Fail");
                    return false;
                }
                if (node.outputs.size() == 0) {
                    System.out.println("Whoa, attempting to verify mesh; a fork node has no output. Fail");
                    return false;
                }                
            }
            else if (node.type.equals("CHAIN")) {
                if (node.inputRefs.size() > 1) {
                    System.out.println("Whoa, attempting to verify mesh; a chain node has multiple ("+node.inputs.size()+") inputs. Fail");
                    return false;
                }
                if (node.outputs.size() == 0) {
                    System.out.println("Whoa, attempting to verify mesh; a chain node has no output. Fail");
                    return false;
                }
                if (node.outputs.size() > 1) {
                    System.out.println("Whoa, attempting to verify mesh; a chain node has multiple ("+node.outputs.size()+") outputs. Fail");
                    return false;
                }
            }
            else if (node.type.equals("JOIN")) {
                if (node.outputs.size() == 0) {
                    System.out.println("Whoa, attempting to verify mesh; a join node has no output. Fail");
                    return false;
                }
                if (node.outputs.size() > 1) {
                    System.out.println("Whoa, attempting to verify mesh; a join node has multiple ("+node.outputs.size()+") outputs. Fail");
                    return false;
                }
            }
            else {
                System.out.println("Whoa attempting to verify mesh; unknown node process:"+node.type+". Fail.");
                return false;
            }
        }
        if (emptyCount > 0) {
            System.out.println("Whoa, attempting to verify mesh; there are "+emptyCount+" empty nodes. Fail.");
            return false;
        }
        if (terminusCount == 0) {
            System.out.println("Whoa, attempting to verify mesh; there are no terminus nodes. Fail.");
            return false;
        }
        if (terminusCount > 1) {
            System.out.println("Whoa, attempting to verify mesh; there are multiple ("+terminusCount+") terminus nodes. Fail.");
            return false;
        }
        return true;
    }
    //
    // end create<Stuff> methods
    //-------------------------------------------
    
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
     * populateNodeTable - update node JTable from myMesh.
     */
    public void populateNodeTable() {
    
        // blow away current entries in node JTable
        nodeTM.setRowCount(0);
        buildBeforeUpdate = true;
    
        // keep for debugging
        //System.out.println("size:"+myMesh.nodes.size());        
    
        // build List of inputs from all inputs in myMesh
        myMesh.sortInputs();
        List<Integer> inputList = new ArrayList<>();
        for (int i = 0; i < myMesh.nodes.size(); i++) {
            Node node = myMesh.nodes.get(i);
            for (int j = 0; j < node.inputs.size(); j++) {
                if (!inputList.contains(node.inputs.get(j))) {
                    inputList.add(node.inputs.get(j));
                }
            }
        }
        Collections.sort(inputList);
        
        // keep for debugging
        //System.out.println(inputList.toString());        
        
        // iterate through List of inputs 
        while (inputList.size() > 0) {
            int input = inputList.get(0);
            // Find which node takes this input 
            int nodeID;
            for (nodeID = 0; nodeID < myMesh.nodes.size(); nodeID++) {
                Node node = myMesh.nodes.get(nodeID);
                if (node.inputs.contains(input)) {
                    if (node.type.equals("FORK")) {
                        for (int j = 0; j < node.outputs.size(); j++) {
                            nodeTM.addRow(new Object[]{input, node.type, "", node.outputs.get(j)});
                        }
                        inputList.remove(Integer.valueOf(input));
                    } else if (node.type.equals("CHAIN")) {
                        nodeTM.addRow(new Object[]{input, node.type, node.inputRefs.get(input), node.outputs.get(0)});
                        inputList.remove(Integer.valueOf(input));
                    } else if (node.type.equals("TERMINUS")) {
                        nodeTM.addRow(new Object[]{input, node.type, "", "N"});
                        inputList.remove(Integer.valueOf(input));
                    } else if (node.type.equals("JOIN")) {
                        nodeTM.addRow(new Object[]{input, node.type, node.inputRefs.get(input), node.outputs.get(0)});
                        inputList.remove(Integer.valueOf(input));
                    } else if (node.type.equals("")) {                        
                        nodeTM.addRow(new Object[]{input, node.type, "", ""});
                        inputList.remove(Integer.valueOf(input));
                    } else {
                        System.out.println("Whoa, badness in refreshNodeTablk(), invalid node type:"+node.type+". Fail.");
                        return;
                    }
                    break;
                }
            }
            if (nodeID == myMesh.nodes.size()) {
                System.out.println("Whoa, badness in refreshNodeTablk(), no node has input:"+input+". Fail.");
                return;
            }
        }        
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
    

    /**
     * parseArgs - Parse the command line arguments. 
     *             Call the method to build myMeshMap.
     *  @param className - Name of Class from which main method called
     *  @param args      - Array of command line arguments 
     */
    public void parseArgs(String className, String[] args) {
        String meshfilename = "";
        
        // If '--help' is any of the arguments then 
        // show proper command line usage and exit
        for (String arg : args) {
            //
            // Always good to have an option to show what the valid command arguments are.
            //
            if (arg.equals("--help") || arg.equals("-help")) {
                System.out.println("Usage:");
                System.out.println(className + "    [<MeshFileName>] | [[-]-help] |");
                System.exit(0);
            }
        }
        // keep for debugging
        //System.out.println(cmdMap.toString());
        
        // Link file name can only be first argument.
        // All commands have a '-' prefix.
        if ((args.length > 0) && (args[0].charAt(0) != '-')) {
            meshfilename = args[0];
        }
        getMesh(meshfilename);
    }    
    
    /**
     * getMesh - parse mesh file
     * @param meshfilename - mesh file name
     */
    public void getMesh(String meshfilename) {
        myMesh = new NodeMesh();
        myMeshMap = new HashMap<>();
        Node node = null;
        int key = -1;
        String ref = "";
        
        BufferedReader reader;
        boolean failToParse = false;
        int lineNum = 1;
        // If there is a chain file name provided in the command line arguments,
        // then try to parse it. 
        if (!meshfilename.equals("")) {
            try {
                myMeshMap.put("MESH_FILE", meshfilename);
                reader = new BufferedReader(new FileReader(meshfilename));
                String line = reader.readLine();
                while (line != null) {
                    line = line.trim();
                    String[] chunks = line.split("\\s+");
                    
                    // xml-like format to parse:
                    //
                    // <IMAGE> image.png </IMAGE>
                    // <IMAGE_SELECTED> image.png <IMAGE_SELECTED>
                    // <MESH_REFERENCE> reference <MESH_REFERENCE>
                    // <MESH_FILE> file.txt <MESH_FILE>
                    // <NODE>                                      <-- header for a single node; there may be multiple nodes in the file
                    //   <INS>                                     <-- header for a key,value of input index and input reference; node may have multiple INS 
                    //      <INT> integer index of input </INT>    <-- mandatory
                    //      <REF> reference </REF>                 <-- optional
                    //   </INS>
                    //   <TYPE> "FORK", "JOIN", "CHAIN", "TERMINUS", "" </TYPE>
                    //   <OUTS> list of output indices </OUT>
                    // </NODE>
                    
                    if (chunks.length != 0) {
                        if (chunks[0].equals("<NODE>")) {
                            if (node != null) { failToParse = true; }
                            else {
                                node = new Node();
                            }
                        }
                        else if (chunks[0].equals("</NODE>")) {                        
                            if (myMesh == null) { 
                                System.out.println("Whoa, null mesh to add a node to. Fail.");
                                failToParse = true; 
                            }
                            else if (node == null) {
                                System.out.println("Whoa, trying to add a null node to mesh. Fail.");
                                failToParse = true;
                            }
                            else if (node.inputRefs.size() == 0) {
                                System.out.println("Whoa, no inputs on node being added to mesh. Fail.");
                                failToParse = true;
                            }
                            else {
                                myMesh.nodes.add(node);
                                node = null;
                            }
                        }
                        else if (chunks[0].equals("<INS>")) {
                            if (node == null) { failToParse = true; }
                            else {
                                key = -1;
                                ref = "";                        
                            }
                        }
                        else if (chunks[0].equals("<REF>")) {
                            if (chunks.length == 2) { } // blank REF, don't need to do anything
                            else if (chunks.length != 3) { failToParse = true; }
                            else if (!chunks[2].equals("</REF>")) { failToParse = true; }                                                                 
                            else {                        
                                ref = chunks[1].trim();                                
                            }
                        }                        
                        else if (chunks[0].equals("<INT>")) {
                            if (chunks.length == 2) { } // blank REF, don't need to do anything
                            else if (chunks.length != 3) { failToParse = true; }
                            else if (!chunks[2].equals("</INT>")) { failToParse = true; }                                                                 
                            else {                        
                                key = Integer.parseInt(chunks[1].trim());
                            }
                        }                        
                        else if (chunks[0].equals("</INS>")) {
                            if (node == null) {failToParse = true; }
                            else if (key == -1) { failToParse = true; }
                            else {
                                node.inputRefs.put(key, ref);
                            }
                        }
                        else if (chunks[0].equals("<TYPE>")) {
                            if (chunks.length == 2) { } // blank REF, don't need to do anything
                            else if (chunks.length != 3) { failToParse = true; }
                            else if (!chunks[2].equals("</TYPE>")) { failToParse = true; }                                                                 
                            else {                        
                                node.type = chunks[1].trim();                                
                            }
                        }
                        else if (chunks[0].equals("<OUT>")) {
                            if (!chunks[chunks.length-1].equals("</OUT>")) { failToParse = true; }                                
                            else if (node == null) { failToParse = true; }
                            else {
                                // substring inside the OUT brackets and split by whitespace
                                String[] ins = line.substring(5,line.length()-6).trim().split("\\s+");
                                for (int j = 0; j < ins.length; j++) {
                                    if (!ins[j].equals("")) {
                                        node.outputs.add(Integer.parseInt(ins[j]));
                                    }
                                }
                            }
                        }
                        else if (chunks[0].equals("<IMAGE>")) {
                            if (chunks.length != 3) { failToParse = true; }
                            else if (!chunks[2].equals("</IMAGE>")) { failToParse = true; }
                            else {
                                sourceImages.add(chunks[1].trim());
                            }
                        }                        
                        else if (chunks[0].equals("<IMAGE_SELECTED>")) {
                            if (chunks.length != 3) { failToParse = true; }
                            else if (!chunks[2].equals("</IMAGE_SELECTED>")) { failToParse = true; }
                            else {
                                myMeshMap.put("IMAGE_SELECTED", chunks[1].trim());
                            }
                        }
                        else if (chunks[0].equals("<MESH_REFERENCE>")) {
                            if (chunks.length != 3) { failToParse = true; }
                            else if (!chunks[2].equals("</MESH_REFERENCE>")) { failToParse = true; }
                            else {
                                myMeshMap.put("MESH_REFERENCE", chunks[1].trim());
                            }
                        }
                        else if (chunks[0].equals("<MESH_FILE>")) {
                            if (chunks.length != 3) { failToParse = true; }
                            else if (!chunks[2].equals("</MESH_FILE>")) { failToParse = true; }
                            else {
                                myMeshMap.put("MESH_FILE", chunks[1].trim());
                            }
                        }
                        else {
                            failToParse = true;
                        }
                    }
                    
                    if (failToParse) {
                        throw new IOException("Invalid format in link file: " + meshfilename + " line: " + lineNum);
                    }
                    
                    line = reader.readLine();
                    lineNum += 1;
                }                
                reader.close();
            }
            // For all IOExceptions, clear myMeshMap from any 
            // accumulated key, values before the exception was thrown.
            catch (IOException e) { 
                System.out.println("MeshGui.getMesh() error:");
                System.out.println(e);
                myMeshMap.clear();
            }
        }
        // If there is no explicit MESH_REFERENCE given then
        // set it to "default" so that there is at least some
        // sort of mesh reference.
        if (!myMeshMap.containsKey("MESH_REFERENCE")) {
            myMeshMap.put("MESH_REFERENCE", "default");
        }
        // If there is no explicit MESH_FILE given then
        // set it to "default" so that there is at least some
        // sort of mesh reference.
        if (!myMeshMap.containsKey("MESH_FILE")) {
            myMeshMap.put("MESH_FILE", "mesh_default.txt");
        }
        // start myMesh with at least one node
        if (myMesh.nodes.size() == 0) {
            myMesh.nodes.add(new Node(0));
        }
        // calculate mesh's maxInput
        myMesh.calcMaxInput();
    }
    
    /**
     * updateRef - update link settings file names based on an update of the chain reference.
     */ 
    public void updateRef() {        
    
        String newfilename;
        //
        // Substitute the new mesh reference in
        // place of the mesh settings file name.
        //
        String meshfilename = fileMeshTF.getText();
        newfilename = meshfilename.replace(meshRefStr, refMeshTF.getText());
        newfilename = newfilename.replace("default", refMeshTF.getText());
        fileMeshTF.setText(newfilename);
        
    }
    
    
    /**
     * updateGuis - instantiate guis from chain nodes in myMesh
     * @return boolean - true if no errors, else false
     */
    public boolean updateGuis() {
    
//        myMesh.calcInputZero();
        myMesh.calcJoinNode();
        myMesh.calcStreams();
        boolean status = true;
        ChainGui gui;
        
        if (buildBeforeUpdate) {
            // kill off frames if there are any
            if (chainGuis != null) {
                for (int x = chainGuis.size()-1; x >= 0; x--) {
                    chainGuis.get(x).frame.dispose();
                    chainGuis.remove(x);
                    chainNode.remove(x);
                }
            }
            myMesh.sortInputs();
            for (int i = 0; i < myMesh.nodes.size(); i++) {
                Node node = myMesh.nodes.get(i);
                if (node.type.equals("CHAIN")) {
            
                    List<String> argList = new ArrayList<String>();    
                    String ref = node.inputRefs.get(node.inputs.get(0));
                    String filename = "chain_"+ref+".txt";
                    argList.add(filename);
                    
                    //
                    // add chain settings filename
                    //
                    argList.add("-f");
                    argList.add(filename);
                    
                    //
                    // add chain reference
                    //
                    argList.add("-r");
                    argList.add(ref);
                    
                    //
                    // If this chain node is directly connected to the root input...
                    //
                    if (node.streams.size() == 0) {
                        //
                        // add all input images
                        //
                        for (int j = 0; j < imgTable.getRowCount(); j++) {
                            argList.add("-i");
                            argList.add(imgTable.getValueAt(j, 0).toString());
                        }
                        //
                        // add input image selected
                        //
                        int srcSelectionIndex = imgTable.getSelectedRow();
                        if (srcSelectionIndex > -1) {
                            argList.add("-s");
                            argList.add(imgTable.getValueAt(srcSelectionIndex, 0).toString());
                        }                                     
                    }                    
                    else {
                        //
                        // add output of predecessor to this gui's input images
                        // and select it 
                        //
                        // keep for debugging
                        //System.out.println("streams:"+node.streams.toString());
                        for (Map.Entry<Integer, String> entry : node.streams.entrySet()) {
                            Node inode = myMesh.nodes.get(entry.getKey());
                            gui = chainGuis.get(inode.chainIndex);
                            // If the Join key hasn't been entered in the Mesh or the chainOutputFile not
                            // been created, then we cannot set up the images to the join ChainGui.
                            if (!entry.getValue().trim().equals("") && gui.chainOutputFile != null) {
                                argList.add("-k");
                                argList.add(entry.getValue());
                                argList.add(gui.chainOutputFile);
                                argList.add("-j");
                                argList.add(gui.chainOutputFile);
                            }
                        }
                    }
                
                    // Convert List of args to array of String of args
                    String[] args = new String[argList.size()];
                    args = argList.toArray(args);
                    //keep for debugging
                    //System.out.println("buildChainGuis arglist:");
                    //System.out.println(argList.toString());
                    
                    if (node.isJoinChain) {                    
                        gui = new JoinGui(args);
                    }
                    else {
                        gui = new ChainGui(args);                    
                    }
                    //keep for debugging
                    //System.out.println("gui class:" +gui.getClass().getSimpleName());
                    
                    gui.updateGuis();
                    // Add gui to list of chain guis and cross-reference
                    // The index of the chainGui to the index of the mesh node.
                    chainGuis.add(gui);
                    chainNode.add(i);
                    node.chainIndex = chainGuis.size()-1;
                    gui.frame.setLocation(chainGuis.size()*50, chainGuis.size()*50);
                }
            }
            
            buildBeforeUpdate = false;
        }
        else {
            // Check if the Mesh Gui has an image selected
            // If so, propogate the image through the chainGuis
            int srcSelectionIndex = imgTable.getSelectedRow();
            if (srcSelectionIndex > -1) {
                String sourceFilename = imgTable.getValueAt(srcSelectionIndex, 0).toString();                
                for (int c = 0; c < chainGuis.size(); c++) {
                    gui = chainGuis.get(c);
                    Node node = myMesh.nodes.get(chainNode.get(c));
                    // make sure the chains generated from the hasInputZero 
                    // node has this image and has it selected in their image Tables
                    if (node.streams.size() == 0) {                        
                        int idx = gui.sourceImages.indexOf(sourceFilename);
                        if (idx == -1) {
                            gui.sourceImages.add(sourceFilename);
                            gui.imageTM.addRow(new Object[]{sourceFilename});                            
                        }
                        idx = gui.sourceImages.indexOf(sourceFilename);
                        gui.imgTable.setRowSelectionInterval(idx, idx);
                    }
                    else {
                        gui.imgTable.getSelectionModel().clearSelection();
                        for (Map.Entry<Integer, String> entry : node.streams.entrySet()) {
                            Node inode = myMesh.nodes.get(entry.getKey());
                            sourceFilename = chainGuis.get(inode.chainIndex).chainOutputFile;
                            int idx = gui.sourceImages.indexOf(sourceFilename);
                            if (idx == -1) {
                                gui.sourceImages.add(sourceFilename);
                                gui.sourceKeys.add(entry.getValue());
                                gui.imageTM.addRow(new Object[]{sourceFilename, entry.getValue()});                            
                            }
                            idx = gui.sourceImages.indexOf(sourceFilename);
                            gui.imgTable.addRowSelectionInterval(idx, idx);
                        }                        
                    }
                    gui.updateGuis();
                }
            }            
        }
        return status;
    }

    /**
     * saveSettings - write chain gui settings to file
     */
    public void saveSettings() {
        String meshfilename = fileMeshTF.getText();
        
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(meshfilename));
            //
            // Write mesh refrence to file
            //
            writer.write("<MESH_REFERENCE>\t" + refMeshTF.getText() +"\t</MESH_REFERENCE>\n");
            //
            // Write images and selected image to file
            //
            for (int i = 0; i < imgTable.getRowCount(); i++) {
                writer.write("<IMAGE>\t"+imgTable.getValueAt(i, 0).toString()+"\t</IMAGE>\n");
            }
            int srcSelectionIndex = imgTable.getSelectedRow();
            if (srcSelectionIndex > -1) {
                String sourceFilename = imgTable.getValueAt(srcSelectionIndex, 0).toString();
                writer.write("<IMAGE_SELECTED>\t" + sourceFilename +"\t</IMAGE_SELECTED>\n");
            }             
            //
            // Write mesh nodes to file
            //            
            for (int i = 0; i < myMesh.nodes.size(); i++) {
                Node node = myMesh.nodes.get(i);
                writer.write("<NODE>\n");
                for (Map.Entry<Integer, String> entry : node.inputRefs.entrySet()) {
                    writer.write("\t<INS>\n");                
                    writer.write("\t\t<INT>\t"+entry.getKey()+"\t</INT>\n");
                    writer.write("\t\t<REF>\t"+entry.getValue()+"\t</REF>\n");
                    writer.write("\t</INS>\n");
                }
                writer.write("\t<TYPE>\t"+node.type+"\t</TYPE>\n");
                writer.write("\t<OUT>\t");
                for (int j = 0; j < node.outputs.size(); j++) {
                    writer.write(node.outputs.get(j)+" ");
                }
                writer.write("</OUT>\n");
                writer.write("</NODE>\n");
            }
            //
            // Write mesh filename to file
            //
            writer.write("<MESH_FILE>\t" + fileMeshTF.getText() +"\t</MESH_FILE>\n");
            writer.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    
    /**
     * exportCode - write code to file from all chains and links within this mesh
     */
    public void exportCode() {
        String linkfilename  = "code_Mesh_"+refMeshTF.getText()+".java";
        List<String> importMeshList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        BufferedWriter writer;
        
        // Bail out if there are no chainGuis
        if (chainGuis.size() == 0) {
            exportCodeB.setBackground(Color.RED);
            System.out.println("No chains: press 'Update Gui's' button");
            return;
        }

        try {
            writer = new BufferedWriter(new FileWriter(linkfilename));
            
            // iterate through all the chains within the mesh...
            for (int i = 0; i < chainGuis.size(); i++) {          
                ChainGui chainGui = chainGuis.get(i);
                Node node = myMesh.nodes.get(chainNode.get(i));
                
                //
                // append each new import statement from a given chain to this mesh's list of import statements
                //
                List<String> importChainList = chainGui.genImportList();
                if (importChainList != null) {
                    for (String importStr : importChainList) {   
                        if (!importMeshList.contains(importStr)) {
                            importMeshList.add(importStr);
                        }
                    }
                }
                else {
                    System.out.println("WHOA, null import list returned from chain:"+node.inputRefs.get(node.inputs.get(0)));
                }
                
                //
                // get and append all the Link's generated code from given chain to this mesh's code
                //
                sb.append(chainGui.genCodeString());
            }
             
            // print the list of import statements to file 
            if (importMeshList.size() > 0) {
                writer.write("//requires:\n");
                Collections.sort(importMeshList);
            }
            for (int i = 0; i < importMeshList.size(); i++) {
                writer.write("//import "+importMeshList.get(i)+";\n");
            }            
            
            // print the accumulated generated code to file
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {}    
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
                new MeshGui(args);
            }
        });
    }
}

/**
 * Node - Class to hold information for a Node
 */
class Node {
    Map<Integer, String> inputRefs; // Map of input(Integer key) and references(String value)
    List<Integer> outputs;          // List of output connections from this node
    String        type;             // one of: "CHAIN", "FORK", "JOIN", "TERMINUS", ""
    
    int           chainIndex = -1;  // cross-reference into List of chainGuis that this node corresponds to
    
    // These fields are for holding calculated values -- not for direct writing
    List<Integer> inputs;           // List of input connections to this node; especially for sorting
    boolean       hasInputZero;     // true if this node has a direct (as a CHAIN or via FORK) to input 0
    boolean       isJoinChain;      // true if this node's input is a JOIN node's output
    Map<Integer, String> streams;   // Map of upstream node indices(Integer key) and reference(String value)
    
    /**
     * Constructor - minimal non-null assignments
     */
    public Node (int input, String ref) {
        this.inputRefs = new HashMap<>();
        this.inputRefs.put(input, ref);
        this.outputs = new ArrayList<>();
        this.type = "";
    }    
    
    /**
     * Constructor - minimal non-null assignments
     */
    public Node (int input) {
        this.inputRefs = new HashMap<>();
        this.inputRefs.put(input, "");
        this.outputs = new ArrayList<>();
        this.type = "";
    }    
    
    /**
     * Constructor
     */ 
    public Node () {
        this.inputRefs = new HashMap<>();
        this.outputs = new ArrayList<>();
        this.type = "";
    }
}

/**
 * NodeMesh - Class to hold information for a Link of Nodes
 */
class NodeMesh {
    List<Node> nodes; // List of nodes
    int maxInput;     // Maximum input number

    /**
     * sortInputs - get all inputs from inputRefs Map and sort them; by node
     */
    public void sortInputs() {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            node.inputs = new ArrayList<>();
            for (int key : node.inputRefs.keySet())
                node.inputs.add(key);
            Collections.sort(node.inputs);
        }
    }
    
    /**
     * calcStreams - determine input connections and references among CHAIN nodes
     */
    public void calcStreams() {
        List<Integer> inputs = new ArrayList<>();
        List<String>  refs   = new ArrayList<>();
        sortInputs();
        for (Node node : nodes) {
            if (node.type.equals("CHAIN") && (node.inputs.size() > 0)) {
                inputs.clear();
                refs.clear();
                for (Integer input : node.inputs) {
                    inputs.add(input);
                    refs.add("");
                }
                node.streams = new HashMap<>();
                
                while (inputs.size() > 0) {
                    for (int j = 0; j < nodes.size(); j++) {
                        Node jnode = nodes.get(j);
                        if (jnode.outputs.contains(inputs.get(0))) {
                            if (jnode.type.equals("FORK")) {
                                inputs.add(jnode.inputs.get(0));
                                refs.add(refs.get(0));
                            }
                            else if (jnode.type.equals("JOIN")) {
                                for (Map.Entry<Integer, String> entry : jnode.inputRefs.entrySet()) {
                                    inputs.add(entry.getKey());
                                    refs.add(entry.getValue());
                                }
                            }
                            else if (jnode.type.equals("CHAIN")) {
                                node.streams.put(j, refs.get(0));
                            }
                        }
                    }
                    inputs.remove(0);
                    refs.remove(0);
                }
            }
        }
/*        
        // keep for debugging
        //
        if (nodes == null) {
            System.out.println("no nodes to calcStreams .");
        } else {
            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);                
                if (node.type.equals("CHAIN")) {
                    System.out.println("node:"+i+" with streams:"+node.streams.toString());
                }
            }
        }
*/        
    }
    
    
    /**
     * calcJoinChain - determine which CHAIN nodes are outputs of JOIN nodes
     */
    public void calcJoinNode() {
        for (Node node : nodes) {
            node.isJoinChain = false;
            if (node.type.equals("CHAIN") && (node.inputs.size() > 0)) {
                int input = node.inputs.get(0);
                for (Node jnode : nodes) {
                    if ((jnode.outputs.size() > 0) && jnode.type.equals("JOIN")) {
                        if (jnode.outputs.get(0) == input) {
                            node.isJoinChain = true;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * calcInputZero - determine which nodes connect directly or by fork to input 0
     */
    public void calcInputZero() {
        List<Integer> zeroNodes = new ArrayList<>();
        if (!(nodes == null)) {
            sortInputs();
            // set all nodes to false
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).hasInputZero = false;
            }
            // Find node with Input 0
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).inputs.contains(0)) {
                    zeroNodes.add(i);
                    break;
                }
            }
            // construct and iterate through zeroNodes list
            while (zeroNodes.size() > 0) {
                Node node = nodes.get(zeroNodes.get(0));
                // A chain node gets set to true; but doesn't propogate to more zero nodes
                if (node.type.equals("CHAIN")) {
                    node.hasInputZero = true;
                }
                // A fork node gets set to true and propogates
                else if (node.type.equals("FORK")) {
                    node.hasInputZero = true;
                    // Find all nodes with inputs same as this FORK's output
                    for (int j = 0; j < node.outputs.size(); j++) {
                        int connection = node.outputs.get(j);
                        for (int k = 0; k < nodes.size(); k++) {
                            if (nodes.get(k).inputs.contains(connection)) {
                                zeroNodes.add(k);
                            }
                        }
                    }                
                }
                // this node was processed, so eliminate it from list
                zeroNodes.remove(0);
            }
        }
        
        // keep for debugging
        //
        /*if (nodes == null) {
            System.out.println("no nodes to calculate hasInputZero.");
        } else {
            System.out.println("nodes with hasInputZero:");
            for (int d = 0; d < nodes.size(); d++) {
                if (nodes.get(d).hasInputZero) {
                    System.out.println(d+"  "+nodes.get(d).ref);
                }
            }
        }*/
    }
    
    /**
     * calcMaxInput - find the highest indexed input within the mesh of nodes
     */
    public void calcMaxInput() {
        sortInputs();
        maxInput = -1;
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = 0; j < nodes.get(i).inputs.size(); j++) {
                    maxInput = Math.max(maxInput, nodes.get(i).inputs.get(j));
                }
            }
        } 
    }
    
    /**
     * Constructor - minimal non-null assignments
     */
    public NodeMesh () {
        this.nodes = new ArrayList<>();
        this.maxInput = -1;
    }    
}

