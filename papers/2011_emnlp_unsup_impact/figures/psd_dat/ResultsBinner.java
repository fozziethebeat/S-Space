import java.io.*;
import java.util.*;

import edu.ucla.sspace.common.Statistics;

public class ResultsBinner {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("usage: RB input.dat output.dat");
            return;
        }

        List[] scores = new List[100];
        for (int i = 0; i < scores.length; ++i)
            scores[i] = new ArrayList<Double>();

        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        br.readLine();
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] arr = line.split("\\s+");
            if (arr.length != 6 || line.startsWith("#"))
                continue;
            double similarity = Double.parseDouble(arr[3]);
            double psdAcc = Double.parseDouble(arr[4]);
            int bucket = (int)(similarity * 100);
            scores[bucket].add(psdAcc);
        }
        br.close();
        
        for (int i = 0; i < scores.length; ++i) {
            List<Double> s = (List<Double>)(scores[i]);
            if (s.size() > 0) {
                double stderr = (s.size() > 1 ? Statistics.stderr(s) : 0);
                System.out.println(i + " " + Statistics.mean(s) + " " +
                                   stderr + " " + s.size());
            }
        }
    }

}