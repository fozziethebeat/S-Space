/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.experiment

import cc.mallet.classify._
import cc.mallet.types._

import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.common.ArgOptions
import edu.ucla.sspace.vector.DoubleVector
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.VectorIO
import edu.ucla.sspace.util.SerializableUtil

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable

import scala.io.Source
import scala.util.Random

import java.io.File
import java.io.FileReader


/**
 * A simple wrapper around <a href="http://mallet.cs.umass.edu/">Mallet's</a>
 * classifier code that simplifies the training and evaluation of a classifier
 * with arbitrary feature spaces.  Mallet's command line tool assumes that the
 * input will be text documents.  This code assumes that the data points are
 * represented as rows in a matrix, where the features can be any arbitrary
 * type.
 * 
 * </p>
 * 
 * This code includes a simple Bagged Decision Tree trainer.
 * 
 * </p>
 *
 * This code requires the following arguments, in the following order:
 * <ol>
 *   <li> trainer: an abreviation of a classifier trainer in mallet.  Can be:
 *        nb,c45,dt,me, or bag. </li>
 *   <li> dataMatrix: a SVDLIBC_SPARSE_TEXT or DENSE_TEXT matrix file where each
 *        row is a data point and each column is a feature.</li>
 *   <li> idFile: a unique idenfier for each row in dataMatrix, in the same
 *        order as the rows, with one identifier per line.</li>
 *   <li> labelFile: the class label associated with each row in dataMatrix,
 *        with one label per row in the same order as in dataMatrix.</li>
 *   <li> testLabels: a file listing the test identifiers used for evaluation.
 *        This should be a subset of the ids in idFile.  Each line should have 
 *        two parts, "anytoken identifier" per line.  The ordering does not 
 *        matter.</li>
 * </ol>
 *
 * </p>
 *
 * This code also supports the following options:
 * <ul>
 *  <li> -d, dense: Set to true if the data matrix is in a dense format, 
 *                  otherwise the data is assumed to be in the 
 *                  SVDLIBC_SPARSE_TEXT format.</li>
 * </ul>
 *
 * </p>
 *
 * Classification results for the test set will be printed in the form:
 * </br>
 * identifier_base identifier label
 * </br>
 */
object SchiselClassify {

    /**
     * Create a Mallet {@link Instance} from a row vector in a matrix.  The row
     * should have sense label {@code sense} and unique identifier {@code id}.
     * {@code alphabet} is required in order to create the feature vector and
     * {@code classes} maintains the set of possible classes labels and will be
     * updated with the {@code sense} label.
     */
    def makeInstance(rowVector:DoubleVector, 
                      sense: String, 
                     id: String,
                     alphabet: Alphabet, 
                     classes: LabelAlphabet) = {
        // Check to see whether or not row vector is sparse.  If it is, only
        // extract the non zero values and indices , otherwise evaluate all
        // indices.
        val (nonZeros, values) = rowVector match {
            case sv:SparseDoubleVector => {
                val nz = sv.getNonZeroIndices
                (nz, nz.map( i => sv.get(i)))
            }
            case v:DoubleVector =>
                (0 until v.length toArray, v.toArray)
        }

        // Create a new feature vector using the given alphabet and classes
        // mapping for sense.
        new Instance(new FeatureVector(alphabet, nonZeros, values),
                     classes.lookupLabel(sense), id, null)
    }
        
    def main(vargs:Array[String]) {
        // Parse the command line options.
        val options = new ArgOptions()
        options.addOption('d', "dense", 
                          "Set to true if the data matrix is in a dense " +
                          "format, otherwise the data is assumed to be in the" +
                          "SVDLIBC_SPARSE_TEXT format.",
                          false, null, "Optional")
        val args = options.parseOptions(vargs)
        
        // Load the data matrix to be evaluated.  
        val data = VectorIO.readSparseVectors(new FileReader(args(1)))

        // Read the set of unique identifiers for each data point, in the same
        // order as the rows in the data matrix.
        val headers = Source.fromFile(args(2)).getLines.toList

        // Read the class labels associated with each data point, in the same
        // order as the rows for the matrix.
        val labels = Source.fromFile(args(3)).getLines.toList

        // Extract the set of test identifiers.  Data points corresponding to
        // these labels should be set aside for testing the classifier and not
        // be used for training.  The format should be
        //     <anything> <identifier> 
        // Only the identifier will be used.
        val testLabels = Source.fromFile(args(4)).getLines.map(_.split("\\s+")(1)).toSet

        // Mallet requires each feature to be represented by some descriptive
        // object, so create a generic object for each feature corresponding to
        // the feature index and turn it into an alphabet.  
        val terms:Array[Object] = (0 until data(0).length).map(_.toString).toArray
        val alphabet = new Alphabet(terms)

        // Create an alphabet to record the set of possible class values each
        // object can take.
        val classes = new LabelAlphabet()

        // Extract the training instance list.  Ignore any data points whose
        // id appears in the test label set.
        val instanceList = new InstanceList(alphabet, classes)
        for ( (dataPoint, i) <- data.zipWithIndex; if !testLabels.contains(headers(i)) )
            instanceList.add(makeInstance(
                dataPoint, labels(i), headers(i), alphabet, classes))

        // Create the requested classifier model based on the first argument.
        val trainer = args(0) match {
            case "nb" => new NaiveBayesTrainer()
            case "c45" => new C45Trainer()
            case "dt" => new DecisionTreeTrainer()
            case "me" => new MaxEntTrainer()
            case "bag" => new BaggedEnsembleTrainer(100)
        }

        // Train the classifier with the training instances.
        System.err.println("reporter:status:Training with " + args(0))
        val classifier = trainer.train(instanceList)

        // Extract the raw descriptor for the dataset.
        val term = headers(0).replaceAll(".[0-9]+", "")
        System.err.println("reporter:status:Labeling with " + args(0))
        // Iterate through the rows and classify each test instance.  For each
        // instance, find the best class labeling and report it's assignment.
        for ( (dataPoint, i) <- data.zipWithIndex; if testLabels.contains(headers(i))) {
            val inst = makeInstance(dataPoint, labels(i), headers(i), alphabet, classes)
            printf("%s %s %s\n", term, headers(i),
                   classifier.classify(inst).getLabeling.getBestLabel)
        }
    }

    /**
     * A trainer for bagged ensembles of decision trees.  This trainer creates
     * {@code numTrees} decision trees and trains each tree with a bag of data
     * points having the same size as the full training dataset but selected at
     * random from the full dataset with replacement.  Some points, and therefor
     * some classes, may not be fully represented in any individual bag/decision
     * tree.  The final classifier will be a {@code ClassifierEnsemble} which
     * gives even weight to each decision tree during classification.
     */
    class BaggedEnsembleTrainer(numTrees:Int) 
            extends ClassifierTrainer[ClassifierEnsemble] {

        // Create the desired number of decision trees.
        val decisionTrees = Array.fill(numTrees)(new DecisionTreeTrainer())

        // Give each tree even weight.
        val weights = Array.fill(numTrees)(1.0/numTrees.toDouble)

        /**
         * Returns a {@code ClassifierEnsemble} around each decision tree with
         * even weights for each tree.
         */
        def getClassifier = new ClassifierEnsemble(decisionTrees.map(_.getClassifier), weights)

        /**
         * Train each decision tree over the dataset in parallel.
         */
        def train(trainingSet: InstanceList) = {
            // Extract the data and class label alphabest so we can create new
            // instances with the same alphabets.
            val dataAlphabet = trainingSet.getDataAlphabet
            val targetAlphabet = trainingSet.getTargetAlphabet

            val numPoints = trainingSet.size

            // Train each decision tree by using simple bagging.  Also, do this
            // in parallel.
            decisionTrees.par.foreach( tree => {
                // Create a bag for each decision tree where the bag is the same
                // size as the original training set but is selected from the
                // training points at random with replacement.  Some points may
                // be overrepresented and others may not be represented at all
                // for each invidiual ensemble.
                val bag = new InstanceList(dataAlphabet, targetAlphabet)
                for (i <- 0 until numPoints)
                    bag.add(trainingSet.get(Random.nextInt(numPoints)))

                // Train the decision tree.
                tree.train(bag)
            })

            // Return the final classifier over the bagged decision trees.
            getClassifier
        }
    }
}
