package edu.ucla.sspace.mains;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.lsa.LatentSemanticAnalysis;

import edu.ucla.sspace.matrix.LogEntropyTransform;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.MatlabSparseMatrixBuilder;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.matrix.factorization.NonNegativeMatrixFactorizationMultiplicative;
import edu.ucla.sspace.matrix.factorization.SingularValueDecomposition;

import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.util.SerializableUtil;

import java.io.File;
import java.io.IOError;
import java.io.IOException;


/**
 * @author Keith Stevens 
 */
public class PLSAMain extends GenericMain {

    private BasisMapping<String, String> basis;

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected void addExtraOptions(ArgOptions options) {
        options.addOption('n', "dimensions", 
                          "the number of dimensions in the semantic space",
                          true, "INT", "Algorithm Options"); 
        options.addOption('p', "preprocess", "a MatrixTransform class to "
                          + "use for preprocessing", true, "CLASSNAME",
                          "Algorithm Options");
    }

    public static void main(String[] args) throws Exception {
        PLSAMain lsa = new PLSAMain();
        lsa.run(args);
    }
    
    protected SemanticSpace getSpace() {
        try {
            int dimensions = argOptions.getIntOption("dimensions", 300);
            Transform transform = new LogEntropyTransform();
            if (argOptions.hasOption("preprocess"))
                transform = ReflectionUtil.getObjectInstance(
                        argOptions.getStringOption("preprocess"));
            String algName = argOptions.getStringOption("svdAlgorithm", "ANY");
            MatrixFactorization factorization =
                new NonNegativeMatrixFactorizationMultiplicative();
            basis = new StringBasisMapping();

            throw new IOException("Not sure what to do");
//             return new LatentSemanticAnalysis(
//                 true, dimensions, transform, factorization, false, basis);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    protected void postProcessing() {
        if (argOptions.hasOption('B'))
            SerializableUtil.save(basis, argOptions.getStringOption('B'));
    }

    /**
     * Returns the {@likn SSpaceFormat.BINARY binary} format as the default
     * format of a {@code LatentSemanticAnalysis} space.
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.BINARY;
    }
}
