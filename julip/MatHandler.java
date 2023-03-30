package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

import org.opencv.core.CvType;
import org.opencv.core.Mat;



/**
 * Class to handle file i/o with openCV Mat objects.
 */
public class MatHandler {

/*
    julip mat file format: 
        .mat file extension.        
        first line gives Mat dimension of image associated with the contours
            <col>\t<row>
        iterate over each col:
            iterate over each row:
                each line is <ch0>\t<ch1>... for all channels
*/


    /**
     * writeMat - customized Mat output to file without using Imgcodecs methods
     * @param mat      - Mat to write to file
     * @param filename - name of text file to write to
     */
    public static void writeMat(Mat mat, String filename) {

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            
            // write Mat Size
            // format <col>\t<row>
            writer.write(mat.cols()+"\t"+mat.rows()+"\n");

            // iterate over all cols, rows
            for (int c = 0; c < mat.cols(); c++) {
                for (int r = 0; r < mat.rows(); r++) {
                    double[] data = mat.get(r,c);
                    int ch;
                    for (ch = 0; ch < mat.channels()-1; ch++) {
                        writer.write((int) data[ch]+"\t");
                    }
                    writer.write((int) data[ch]+"\n");
                }
            }
            writer.close();
        } catch (IOException e) {}
    }


    /**
     * readMat - import a julip-formatted mat file into a Link Gui.
     * @param filename - name of text file of Mat data
     * @return Mat     - Mat constructed from importing file contents
     */
    public static Mat readMat(String filename) {

        Mat mat;
        String[] chunks;
        
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filename));
            
            // retrieve Mat Size info, formatted: <col>x<row>
            String line = reader.readLine();
            chunks = line.split("\t");
            int rows = Integer.parseInt(chunks[1]);
            int cols = Integer.parseInt(chunks[0]);
            
            // From first line of data, get the number of channels
            line = reader.readLine();
            chunks = line.split("\t");
            if (chunks.length == 3) {
                mat = new Mat(rows, cols, CvType.CV_8UC3);
            } else {
                mat = new Mat(rows, cols, CvType.CV_8U);
            }
                          
            int r = 0;
            int c = 0;
                        
            while (line != null) {
                chunks = line.split("\t");
                //int[] data = new int[chunks.length];
                byte[] data = new byte[chunks.length];
                for (int i = 0; i < chunks.length; i++) {
                    data[i] = (byte) Integer.parseInt(chunks[i]);
                    //data[i] = Byte.parseByte(chunks[i]);
                }
//System.out.println("r="+r);      
//System.out.println("c="+c);                
//for (int j = 0; j < data.length; j++) {
//System.out.println("data["+j+"]="+data[j]);
//}
                mat.put(r, c, data);
                
                r += 1;
                if (r == rows) {
                    r = 0;
                    c += 1;
                }
                line = reader.readLine();
            }
        } catch (IOException e) { 
            mat = null;
            System.out.println(e);
        }               
        
        return mat;
    }
    
    // use row, column offsets set to 0
    public static void writeMatToCSV(Mat mat, String filename) {
        writeMatToCSV(mat, 0, 0, filename);
    }
    
    /**
     * writeMatToCSV - customized Mat output to file without using Imgcodecs methods
     * @param mat      - Mat to write to file
     * @param filename - name of text file to write to
     */
    public static void writeMatToCSV(Mat mat, int colOffset, int rowOffset, String filename) {

        int c = 0;
        int r = 0;
        double[] data;
    
        BufferedWriter writer;
        try {
            for (int channel = 0; channel < mat.channels(); channel++) {
                writer = new BufferedWriter(new FileWriter(filename+"_"+channel));
            
                // write column numbers on first line
                StringBuilder line = new StringBuilder("\t");
                for (c = 0; c < mat.cols()-1; c++) {
                    line.append(c+colOffset).append("\t");
                }
                line.append(c+colOffset).append("\n");
                writer.write(line.toString());
                            
                for (r = 0; r < mat.rows(); r++) {
                    // prepend row number
                    line = new StringBuilder("");
                    line.append(r+rowOffset).append("\t");
                    
                    for (c = 0; c < mat.cols()-1; c++) {
                        data = mat.get(r,c);
                        line.append((int) data[channel]).append("\t");
                    }
                    data = mat.get(r,c);
                    line.append((int) data[channel]).append("\n");
                    writer.write(line.toString());
                }
                writer.close();
            }
        } catch (IOException e) {}
    }
    
    
}
