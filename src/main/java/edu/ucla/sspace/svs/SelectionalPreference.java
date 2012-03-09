package edu.ucla.sspace.svs;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.ScaledSparseDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class SelectionalPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    public SparseDoubleVector lemmaVector;
    public Map<String, SparseDoubleVector> selPreferences;
    public Map<String, SparseDoubleVector> inverseSelPreferences;

    private final VectorCombinor combinor;

    public SelectionalPreference(VectorCombinor combinor) {
        this.combinor = combinor;

        lemmaVector = new CompactSparseVector();
        selPreferences = new HashMap<String, SparseDoubleVector>();
        inverseSelPreferences = new HashMap<String, SparseDoubleVector>();
    }

    public void addPreference(String relation,
                              SparseDoubleVector vector,
                              double frequency) {

        add(relation, new ScaledSparseDoubleVector(vector, frequency),
            selPreferences);
    }

    public void addInversePreference(String relation, 
                                     SparseDoubleVector vector,
                                     double frequency) {
        add(relation, new ScaledSparseDoubleVector(vector, frequency),
            inverseSelPreferences);
    }

    private void add(String relation, 
                     SparseDoubleVector vector,
                     Map<String, SparseDoubleVector> map) {
        SparseDoubleVector preference = map.get(relation);
        if (preference == null) {
            map.put(relation,
                    (SparseDoubleVector) Vectors.copyOf(vector));
            return;
        }

        map.put(relation, combinor.combine(preference, vector));
    }

    public SparseDoubleVector preference(String relation) {
        return selPreferences.get(relation);
    }

    public SparseDoubleVector inversePreference(String relation) {
        return inverseSelPreferences.get(relation);
    }
}
