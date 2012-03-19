package edu.ucla.sspace.tools;

import edu.ucla.sspace.basis.BasisMapping;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.SerializableUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class SelectTopKWords {
    public static void main(String[] args) throws Exception {
        // Load the basis mapping.
        BasisMapping<String, String> basis = 
            SerializableUtil.load(new File(args[0]));

        // Create the top 10 lists for each topic in the word space.
        List<MultiMap<Double, String>> topTerms = new ArrayList<MultiMap<Double, String>>();
        Matrix m = MatrixIO.readMatrix(new File(args[1]), Format.DENSE_TEXT);
        for (int c = 0; c < m.columns(); ++c)
            topTerms.add(new BoundedSortedMultiMap<Double, String>(10));

        for (int r = 0; r < m.rows(); ++r) {
            String term = basis.getDimensionDescription(r);
            for (int c = 0; c < m.columns(); ++c)
                topTerms.get(c).put(m.get(r, c), term);
        }

        for (MultiMap<Double, String> topicTerms : topTerms) {
            for (String term : topicTerms.values())
                System.out.printf("%s ", term);
            System.out.println();
        }
    }
}

