package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;


public class DeeseEvaluator {
    public static void main(String[] args) throws Exception {
        DeeseAntonymEvaluation  evaluator = new DeeseAntonymEvaluation();
        for (String file : args) {
            SemanticSpace sspace = SemanticSpaceIO.load(file);
            WordAssociationReport report = evaluator.evaluate(sspace);
            System.out.printf("%s: %.3f\n", file, report.correlation());
        }
    }
}
