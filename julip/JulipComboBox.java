package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * JulipComboBox - essentially a JComboBox
 */
public class JulipComboBox {

    public JComboBox comboBox;
    public int       index = 0;

    /**
     * setValue - set selected entry of JComboBox
     * @param item entry to set JComboBox selection
     */
    public void setValue (String item) {
        comboBox.setSelectedItem(item);
        index = comboBox.getSelectedIndex();
    }
    
    /**
     * JulipComboBox - constructor
     * @param items  Array of selectable entries of the Combobox
     * @param item   pre-selected entry
     * @param self  reference to link gui calling this constructor
     */
    public JulipComboBox(String[] items, String item, LinkClass self)  {
        comboBox = new JComboBox<>(items);
        setValue(item);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> cb = (JComboBox<String>)e.getSource();
                index = cb.getSelectedIndex();
                self.refreshSettings();
                self.refreshImage();
            }
        });
    }
    
}
