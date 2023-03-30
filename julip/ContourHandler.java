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
 * Class to handle methods with dealing with openCV contours.
 */
public class ContourHandler {

/*
    julip contour file format: 
        .ctr file extension.        
        first line gives Mat dimension of image associated with the contours
            <col>x<row>
        second line number of contours in the file
            <decimal>
        iterate over each contour:
            Mat size of contour; will be 1xN, where N is the number of points in the contour
                <col>x<row>
            iterate over each point:
                <x-coor>\t<y-coor>        
*/


    /**
     * saveContours - export a julip-formatted contour file from a Link Gui.
     * @param filename - name of text file to write to
     * @param img      - Mat image associated with contours
     * @param contours - List<MatOfPoint> of contours
     */
    public static void saveContours(String filename, Mat img, List<MatOfPoint> contours) {

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            
            // write Mat image Size
            // format <col>x<row>
            writer.write(img.size().toString()+"\n");
            
            // keep for debugging
            //System.out.println(contours.size());
            // write size of List of contours
            writer.write(contours.size()+"\n");
            
            // iterate over all contours
            for (int c = 0; c < contours.size(); c++) {
                Mat contour = contours.get(c);
                // write size (number of points) of a given contour
                writer.write(contour.size()+"\n");
                for (int row = 0; row < contour.rows(); row++) {
                    double[] coor = contour.get(row, 0);
                    // write each point in the contour
                    writer.write((int)coor[0] + "\t" + (int)coor[1] + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {}
    }


    /**
     * loadContours - import a julip-formatted contour file into a Link Gui.
     * @param filename - name of text file of contour data
     * @return List<MatOfPoint> - List of contours; except first entry is a Mat Size
     */
    public static List<MatOfPoint> loadContours(String filename) {

        List<MatOfPoint> contours;
        MatOfPoint contour;
        List<Point> points = new ArrayList<>();
        String[] chunks;
        
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filename));
            contours = new ArrayList<>();
            
            // retrieve Mat Size info, formatted: <col>x<row>
            String line = reader.readLine();
            chunks = line.split("x");
            
            // Consider the col, row of the Mat Size to be a Point.
            // We construct a MatOfPoint with a single Point and
            // make this the first entry for the List of contours.
            points.clear();
            points.add(new Point(
                (double)Integer.parseInt(chunks[0]), 
                (double)Integer.parseInt(chunks[1])
            ));
            contour = new MatOfPoint();
            contour.fromList(points);
            contours.add(contour);
            
            // retrieve number of contours in the file
            points.clear();
            line = reader.readLine();
            int numContours = Integer.parseInt(line);
            
            boolean getSize = true;     // flag to indicate next line read is size of next contour in file
            int numRows = 0;
            int totalRows = 0;    
            
            line = reader.readLine();
            while (line != null) {
                if (getSize) {
                    // line is formatted: <col>x<row>
                    chunks = line.split("x");
                    totalRows = Integer.parseInt(chunks[1]);
                    getSize = false;
                    numRows = 0;    
                    // keep for debugging
                    //System.out.println(totalRows);
                }
                else {
                    // each point in the file is <x-coor>\t<y-coor>
                    chunks = line.split("\\s+");
                    points.add(new Point(
                        (double)Integer.parseInt(chunks[0]), 
                        (double)Integer.parseInt(chunks[1])
                    ));
                    numRows += 1;
                    if (numRows == totalRows) {
                        contour = new MatOfPoint();
                        contour.fromList(points);
                        contours.add(contour);
                        
                        points.clear();
                        getSize = true;
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) { 
            contours = null;
            System.out.println(e);
        }               
        
        return contours;
    }
    
}
