package edu.ucla.sspace.tools;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.util.SerializableUtil;


/**
 * @author Keith Stevens
 */
public class BasisPrinter {
    public static void main(String[] args) {
        BasisMapping<String, String> basis = SerializableUtil.load(
                args[0]);
        for (int i = 0; i < basis.numDimensions(); ++i)
            System.out.println(basis.getDimensionDescription(i));
    }
}

