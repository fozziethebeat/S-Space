package edu.ucla.sspace.mains;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.wordsi.ContextExtractor;
import edu.ucla.sspace.wordsi.PreComputedContextExtractor;


/**
 * An executiable class for running {@link Wordsi} with a {@link
 * PreComputedContextExtractor}.  The core command line arguments are provided
 * by {@link GenericWordsiMain}.  
 *
 * When using the {@code --Save} option, this class will save a {@link
 * BasisMapping} from strings to feature indices.  When using the {@code --Load}
 * option, this class will load a mapping from strings to feature indices from
 * disk.  This mapping must be a {@link BasisMapping}.  If {@code --Save} is not
 * used, a new {@link BasisMapping} will be used.
 *
 * @see GenericWordsiMain
 * @see PreComputedContextExtractor 
 *
 * @author Keith Stevens
 */
public class PreComputedWordsiMain extends GenericWordsiMain {

    /**
     * The {@link BasisMapping} responsible for creating feature indices for
     * features keyed by strings, with each feature being described by a string.
     */
    private BasisMapping<String, String> basis;

    /**
     * {@inheritDoc}
     */
    protected void handleExtraOptions() {
        // If the -L option is given, load the basis mapping from disk.
        if (argOptions.hasOption('L'))
            basis = loadObject(openLoadFile());
        else 
            basis = new StringBasisMapping();
    }

    /**
     * Saves the {@code basis} to disk.
     */
    protected void postProcessing() {
        if (argOptions.hasOption('S'))
            saveObject(openSaveFile(), basis);
    }

    /**
     * {@inheritDoc}
     */
    protected ContextExtractor getExtractor() {
        return new PreComputedContextExtractor(basis);
    }

    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    public static void main(String[] args) throws Exception {
        PreComputedWordsiMain main = new PreComputedWordsiMain();
        main.run(args);
    }
}
