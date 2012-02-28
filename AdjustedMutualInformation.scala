/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
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

import scala.collection.mutable.HashMap
import scala.io.Source
import scala.math._

import java.io.PrintWriter

/**
  * An implementation of the <a
  * href="http://en.wikipedia.org/wiki/Adjusted_mutual_information">Adjusted
  * Mutual Information</a> evaluation metric for
  * the <a
  * href="http://www.cs.york.ac.uk/semeval2010_WSI/taskdescription.html">SemEval
  * 2010 Task 14</a> Word Sense Induction task.  This evaluation conforms to the
  * format expected by the task where each sense labeling is of the form:
  *
  * <pre>
  *    word.pos word.pos.context_id word.pos.sense_label
  * </pre>
  *
  * </p>
  * This script is expected to be run with the following command:
  * <pre>
  *  scala AdjustedMutualInformation.scala solution.key real.solution.key filter
  * </pre>
  * Where filter is expected to be: all, n, or v (corresponding to all words,
  * nouns, or verbs).
  *  
  * </p>
  * 
  * This implementation is based on the following paper:
  * <ul>
  *  <li> N. Vinh, J. Epps, J.Bailey.  Information Theoretic Measures for
  *  Clustering Comparison: Variants, Properties, Normalization and Correction
  *  for Chance.  In <i>Journal of Machine Learning Reseach 11</i>, 2010.
  *  Available <a
  *  href="http://jmlr.csail.mit.edu/papers/volume11/vinh10a/vinh10a.pdf">here</a>
  *  </li>
  * </ul>
  *
  * Furthermore, this implementation is based on the following matlab script by
  * the paper's original authors: <a
  * href="http://ee.unsw.edu.au/~nguyenv/ami.m">ami.m</a>
  *
  * </p>
  *
  * Note though that we make some minor changes:
  *  <ul>
  *    <li> For solutions and keys both with only one class, we return an AMI of
  *    1.0, the matlab script returns NaN.</li>
  *    <li> If the AMI is below 0.0, we return 0.0.  The matlab script returns
  *    the actual AMI in these cases.</li>
  *  </ul>
  */
object AdjustedMutualInformation {

    /**
     * Returns a mapping from words to a set of context assignments from {@code
     * answerFile}.  Each set of context assignments maps context id's to the
     * applied sense label.  If {@code filter} is a non-empty string, the final
     * mapping will only contain mappings for terms that end with {@code
     * filter}.  If {@code referenceMap} is non-null, only word and context
     * id's contained in {@code referenceMap} will be included in the final
     * mapping.
     *
     * @param answerFile A solution file in the format expected by the SemEval
     *        2010 task 14 evaluation
     * @param filter Any word not ending in {@code filter} will be ignored from
     *        the label mapping
     * @param referenceMap If not null, the resulting label mapping will only
     *        contain word and id mappings found in this map
     * 
     * @returns A mapping from words to mappings from context id's to sense
     *          labels
     */
    def extractLabels(answerFile: String, filter: String,
                      referenceMap:HashMap[String, HashMap[Int, String]]) = {
        def acceptLabel(word: String, id: Int) =
            if (referenceMap == null) true
            else referenceMap.get(word).get.contains(id)

        val classLabels = new HashMap[String, HashMap[Int, String]]()
        for (line <- Source.fromFile(answerFile).getLines) {
            val Array(word, instance, label) = line.split("\\s+", 3)
            val cleanLabel = label.split("\\s+")(0).split("/")(0)
            val id = instance.split("\\.")(2).toInt
            if (word.endsWith(filter) && acceptLabel(word, id))
                classLabels.get(word) match {
                    case Some(wordMap) => wordMap(id) = cleanLabel
                    case None =>classLabels(word) = HashMap((id -> cleanLabel))
                }
        }
        classLabels
    }

    /**
     * Returns the entropy of the values in {@code sums} assuming that {@code
     * sum} is the total sum of all values in {@code sums}.
     */
    def entropy(sums:Array[Int], sum:Int) = {
        val total = (sums.map{ s => s / sum.toDouble * log(s/sum.toDouble) }).sum
        -1.0 * total
    }

    /**
     * Returns the contingency matrix {@code M} where {@code M(i)(j)}
     * corresponds to the number of context id's assigned to class i and
     * cluster j.
     */
    def computeMatchMatrix(clusterMap:HashMap[Int, String],
                           classMap:HashMap[Int, String]) = {
        // Create an indexer so that each cluster label has a unique index.
        val labelIndexer = new HashMap[String, Int]() with Indexer
        val clusterIndexer = new HashMap[String, Int]() with Indexer

        // Compute the number of total assignments made for the golden solution
        // and the submitted solution.
        val numClassAssignments = classMap.values.size
        val numClusterAssignments = clusterMap.values.size

        // Compute the number of classes and the number of clusters.  If some
        // points were not assigned points in submitted solution, create an
        // extra cluster corresponding to missing points.
        val rawSize = clusterMap.values.toSet.size 
        val numClasses = classMap.values.toSet.size
        val numClusters = if (numClassAssignments == numClusterAssignments) rawSize 
                          else 1+rawSize
        val missingLabel = "MISSING_LABEL"

        // Create the match matrix.
        val matches = Array.fill(numClasses, numClusters)(0.0)

        // Compute the number of points assigned to each class and cluster
        // combination.
        for ( (id, classLabel) <- classMap) {
            val j = clusterMap.get(id) match {
                case Some(clusterLabel) => clusterIndexer.getDim(clusterLabel)
                case _ => clusterIndexer.getDim(missingLabel)
            }
            val i = labelIndexer.getDim(classLabel)
            matches(i)(j) += 1.0
        }

        matches
    }

    def simpleIndexer(labelFile:String) = {
        val indexer = new HashMap[String, Int]() with Indexer
        Source.fromFile(labelFile).getLines.foreach(w => indexer.getDim(w))
        indexer
    }

    def lines(labelFile: String) = Source.fromFile(labelFile).getLines

    def main(args:Array[String]) {
        if (args(0) == "-w") {
            // Do special processing for a single word instance.  Here we
            // interpret the label files differently.  They instead just have
            // one item per line which is the class labeling.
            val classIndexer = simpleIndexer(args(2))
            val clustIndexer = simpleIndexer(args(1))
            val matches = Array.fill(classIndexer.size, clustIndexer.size)(0.0)
            for ((g,c) <- lines(args(2)).zip(lines(args(1))))
                matches(classIndexer.getDim(g))(clustIndexer.getDim(c)) += 1.0
            ami(matches)
            return
        }

        val filter = if (args.size == 3 && args(2) != "all") args(2) else ""
        val classLabels = extractLabels(args(1), filter, null)
        val clusterLabels = extractLabels(args(0), filter, classLabels)
        var totalAmi = 0.0

        // Compute the AMI for each individual word.  If a word did not have any
        // cluster labels, give it an AMI of 0.
        for ( (word, classMap) <- classLabels) {
            print(word)
            totalAmi += (clusterLabels.get(word) match {
                case Some(clusterMap) => ami(computeMatchMatrix(clusterMap, classMap))
                case None => 0
            })
        }

        // Print the final AMI
        printf("total: %f\n", totalAmi/classLabels.size)
    }

    /**
     * Returns the Adjusted Mutual Information given the contingency counts
     * stored in {@code matches}.  Please see the original paper for details on
     * how this is computed.
     */
    def ami(matches:Array[Array[Double]]) : Double = {
        // First check that there are more than one class and one cluster.  If
        // not, return 1.0.
        val rows = matches.size
        val columns = matches(0).size
        if (rows == 1 &&
            columns == 1) {
            printf(" 0.0 0.0 0.0 0.0 0.0\n")
            return 0.0
        }

        // Compute the total counts for each class and cluster and the total
        // number of points.
        val a = new Array[Int](rows)
        val b = new Array[Int](columns)
        var sum = 0
        for (r <- 0 until rows; c <- 0 until columns) {
            a(r) += matches(r)(c).toInt
            b(c) += matches(r)(c).toInt
            sum += matches(r)(c).toInt
        }

        var emi = 0.0
        var mi = 0.0
        for (r <- 0 until rows; c <- 0 until columns) {
            // Update the mi for this class and cluster.
            val pij = matches(r)(c)/sum.toDouble
            val pi = a(r)/sum.toDouble
            val pj = b(c)/sum.toDouble
            val i = if (pij == 0.0 || pi*pj == 0.0) 0.0 
                    else pij * log(pij/(pi*pj))
            mi += i

            // Update the expectation of mi.
            val nij = max(1, a(r)+b(c) - sum)
            // Determine wether or not this pariing occured more than we'd
            // expect at random.
            val x1 = min(nij, sum-a(r)-b(c)+nij)
            val x2 = max(nij, sum-a(r)-b(c)+nij)
            // Compute a range of numbers
            var num = ((a(r)-nij+1) to a(r)) ++ ((b(c)-nij+1) to b(c))
            var dom = ((sum - a(r)+1) to sum) ++ (1 to x1)
            if (sum-b(c) > x2)
                num = num ++ ((x2+1) to (sum-b(c)))
            else
                dom = dom ++ ((sum-b(c)+1) to x2)

            // Sort the ranges in both num and dom so that we can avoid
            // overflow.
            num = num.sorted
            dom = dom.sorted

            // Compute the product of num / dom.
            var factorialPowers = (for ((n,d) <- num zip dom) yield 
                n / d.toDouble).product

            // Create a small helper function to adjust the score.
            def adjustment(n:Int) = (a(r)-n) * (b(c)-n) / (n+1.0) / (sum-a(r)-b(c)+n+1.0)
            // Create a small helper function to compute the pointwise mutual
            // information.
            def pmi(n:Int) = n/sum.toDouble * log(sum*n/(a(r)*b(c)).toDouble)

            // Create the first value in the factorial computation.
            var factorialSum = pmi(nij) * factorialPowers
            factorialPowers *= adjustment(nij)

            // Update the factorial value for each value left in the range.
            for (n <- max(1, a(r)+b(c)-sum)+1 to min(a(r), b(c))) {
                factorialSum += pmi(n) * factorialPowers
                factorialPowers *= adjustment(n)
            }
            // Update the Expected mutual information with the total sum.
            emi += factorialSum
        }

        // Compute the entropy of the current class and label.
        val ha = entropy(a, sum)
        val hb = entropy(b, sum)
        // If we would get NaN, return a raw AMI of 0.0
        val rami = if (max(ha,hb) - emi == 0) 0.0 
                  else (mi - emi) / (max(ha, hb) - emi)
        // Range the AMI to be above 0.0.
        val ami = if (rami < 0.0) 0.0 else rami

        // Print out some statistics for the word.
        printf(" %f %f %f %f %f\n", ami, mi, emi, ha, hb)

        // Return the final AMI for this word.
        ami
    }

    /** A simple trait that associates a unique integer for each key where the
     * integers are assigned in ascending order with no gaps.
     */
    trait Indexer extends HashMap[String, Int] {
        def getDim(key: String) =
            get(key) match {
                case Some(value) => value
                case None => val value = size
                             put(key, value)
                             value
            }
    }
}
