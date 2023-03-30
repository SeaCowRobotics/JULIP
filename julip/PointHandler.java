package julip;   // Java, Uh, Linked Image Processing (with OpenCV)

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

/**
 * Class to handle methods with dealing with openCV points.
 */
public class PointHandler {

/*
    julip contour file format: 
        .ctr file extension.        
        first line gives Mat dimension of image associated with the contours
            (int)<col>x(int)<row>
        second line number of points in the file
            (int)<N>
        iterate over each pont:
            (Double)<x-coor>\t(Double)<y-coor>        
*/


    /**
     * savePoints - export a julip-formatted point file from a Link Gui.
     * @param filename - name of text file to write to
     * @param img      - Mat image associated with points
     * @param points - List<Point> of points
     */
    public static void savePoints(String filename, Mat img, List<Point> points) {

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            
            // write Mat image Size
            // format (int)<col>x(int)<row>
            writer.write(img.size().toString()+"\n");
            
            // keep for debugging
            //System.out.println(points.size());
            // write size of List of points
            writer.write(points.size()+"\n");
            
            // iterate over all points
            for (int p = 0; p < points.size(); p++) {
                writer.write(points.get(p).x + " " + points.get(p).y + "\n");
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("PointHandler.savePoints exception:");
            System.out.println(e);        
        }
    }


    /**
     * loadPoints - import a julip-formatted point file into a Link Gui.
     * @param filename - name of text file of contour data
     * @return List<Point> - List of points; except first entry is a Mat Size
     *                       for missing or bad file formats, return is null
     */
    public static List<Point> loadPoints(String filename) {

        List<Point> points = new ArrayList<>();
        String line;
        String[] chunks;
        
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filename));
            // Make sure there's a file to read
            if (reader == null) {
                throw new IOException("No readable file :"+filename);
            }
            
            // retrieve Mat Size info, formatted: (int)<col>x(int)<row>
            line = reader.readLine();            
            // Make sure the file isn't empty
            if (line == null) {
                throw new IOException("No lines to read file :"+filename);
            }
            
            chunks = line.split("x");
            // Check for valid julip-Points file format
            if (chunks.length != 2) {
                throw new IOException("First readable line in "+filename+" not <\\d>x<\\d>");
            }
            
            // Consider the col, row of the Mat Size to be a Point.
            // We construct a MatOfPoint with a single Point and
            // make this the first entry for the List of contours.
            points.clear();
            points.add(new Point(
                (double)Integer.parseInt(chunks[0].trim()), 
                (double)Integer.parseInt(chunks[1].trim())
            ));
            
            // retrieve number of ponts in the file
            // format (int)<N>
            line = reader.readLine();
            int numPoints = Integer.parseInt(line);
            
            int numRows = 0;
            
            line = reader.readLine();
            while (line != null) {
                // each point in the file is <x-coor> <y-coor>
                chunks = line.split("\\s+");
                points.add(new Point(
                    Double.parseDouble(chunks[0]), 
                    Double.parseDouble(chunks[1])
                ));
                numRows += 1;
                line = reader.readLine();
            }
            
        } catch (IOException e) { 
            points = null;
            System.out.println(e);
        }               
        
        return points;
    }
    
}
