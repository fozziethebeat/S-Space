import java.io.*;
import java.util.*;

import edu.ucla.sspace.common.Statistics;
import edu.ucla.sspace.util.LineReader;

public class Stats {

    public static void main(String[] args) {
//         List[] points = new List[60];
//         for (int i = 0; i < points.length; ++i)
//             points[i] = new ArrayList<Integer>();
        List<Integer> numPoints = new ArrayList<Integer>();
        for (String file : args) {
            for (String line : new LineReader(new File(file))) {
                String[] arr = line.split("\\s+");
                int i = Integer.parseInt(arr[0]);
                if (i <= 60)
                    numPoints.add(Integer.parseInt(arr[3]));
            }
        }
        
        System.out.printf("Mean number of points %f, median %d (stddev %f)%n",
                          Statistics.mean(numPoints),
                          Statistics.median(numPoints),
                          Statistics.stddev(numPoints));
    }

}