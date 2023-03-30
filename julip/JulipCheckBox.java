package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * JulipCheckBox - essentially a JCheckBox
 */
public class JulipCheckBox {

    public JCheckBox checkBox;
    public boolean   selected = false;

    /**
     * setValue - set selected entry of JComboBox
     * @param item entry to set JComboBox selection
     */
    public void setValue (boolean value) {
        checkBox.setSelected(value);
        selected = checkBox.isSelected();
    }
    
    /**
     * JulipComboBox - constructor
     * @param items  Array of selectable entries of the Combobox
     * @param item   pre-selected entry
     * @param self  reference to link gui calling this constructor
     */
    public JulipCheckBox(String label, boolean value, LinkClass self)  {
        checkBox = new JCheckBox(label, value);
        selected = value;
        checkBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                JCheckBox kb = (JCheckBox) e.getSource();
                selected = kb.isSelected();
                self.refreshSettings();
                self.refreshImage();
            }
        });
    }
    
}
