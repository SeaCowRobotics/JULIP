package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * JulipTrackBar - combination JSlider and associated JLabel.
 *                 The JSlider is user-adjustable.
 *                 The JLabel is intended to hold title and numeric value of JSlider.
 *                 This object is to be used anywhere a slider is useful to any of the Link Guis.
 */
public class JulipTrackBar {

    public JLabel    label = new JLabel(".");  // Label to position programmable text above the Slider
    public int       value = 0;                // Current value of slider
    public JSlider   slider;                   // Java Slider to allow user to change setting value
    public boolean   suppressListener = false; // Flag to allow temporary suppression of JSlider listener
    
    /**
     * setValue - set a new value for JSlider.
     * @param val  value to set JSlider to
     */
    public void setValue (int val) {
        this.value = val;
        this.slider.setValue(val);
    }
    
    /**
     * setValueSilently - set a new value for JSlider without triggering changeListener
     * @param val  value to set JSlider to
     */
    public void setValueSilently (int val) {
        this.value = val;
        this.suppressListener = true;
        this.slider.setValue(val);
        this.suppressListener = false;
    }
    
    /**
     * setMaximum - change the maximum index of the JSlider.
     * @param max  new maximum index
     */
    public void setMaximum (int max) {
        // Changing the maximum slider value fires a change event that
        // is picked up by the slider's changeListener. We want to 
        // suppress the listener's response because the JulipTrackBar
        // listener may cause unintended actions/exceptions to be thrown.
        suppressListener = true;
        if (value > max) { setValue(max); }
        slider.setMaximum(max); 
        suppressListener = false;
    }
    
    /**
     * JulipTrackBar - constructor for a combination JLabel and JSlider.
     * @param min   minimum JSlider value
     * @param max   maximum JSlider value
     * @param val   initial JSlider value
     * @param major major JSlider tick mark interval
     * @param minor minor JSlider tick mark interval
     * @param self  reference to link gui calling this constructor
     */
    public JulipTrackBar(int min, int max, int val, int major, int minor, LinkClass self)  {
        slider = new JSlider(min, max, val);
        slider.setMajorTickSpacing(major);
        if (minor > 0) {
            slider.setMinorTickSpacing(minor);
        }
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!suppressListener) {
                    JSlider source = (JSlider) e.getSource();
                    value = source.getValue();
                    self.refreshSettings();
                    self.refreshImage();
                }
            }
        });
        value = val;
    }
    
}
