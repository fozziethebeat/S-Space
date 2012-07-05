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

// Import several classes from the S-Space package.
import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.bigram.BigramSpace
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.matrix.MatlabSparseMatrixWriter
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.PointWiseMutualInformationTransform
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.text.StringDocument
import edu.ucla.sspace.util.SerializableUtil

// Import some methods to automatically cast scala data structures are java data
// structures.
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet
import scala.io.Source

import java.io.File
import java.io.FileOutputStream
import java.util.HashSet


/**
 * Creates a bigram word space using the Pointwise Mutual Information method for
 * filtering out unimportant bigrams.
 */
object BigramMain {
    def main(args: Array[String]) {
        // Read a set of words to exclude from our bigram statistics.  These include
        // generic stop words such as "the", "and", and a variety of punctuation 
        // characters.  Each line in this file should be a stop word to ignore.
        val excludeSet = new HashSet[String]()
        Source.fromFile(args(1)).getLines.foreach { excludeSet.add(_) }
        val basis = new FilteredStringBasisMapping(excludeSet)

        // Create the bigram space that filteres candidate bigrams using
        // PointwiseMutualInformation and eliminates any cadidates with a PMI less than
        // 5.0.
        val bigramSpace = new BigramSpace(
            basis, 8, new PointWiseMutualInformationTransform(), args(0).toDouble)

        // Create a parser for files in the standard CoNLL format.
        val parser = new CoNLLDependencyExtractor()

        // Iterate over every file containing text and extract each dependnecy parsed
        // sentence from the file.
        for (semevalFile <- args.slice(4, args.length);
             document <- new DependencyFileDocumentIterator(semevalFile)) {
            val reader = document.reader
            // Assume that the first line of every sentence contains meta information
            // and is not part of the sentence, so discard it.
            val header = reader.readLine
            // Extract the tokens from the Dependency Parse Tree and transform it into a
            // simple string of works.
            val tree = parser.readNextTree(reader)
            val wordDoc = new StringDocument(tree.map(_.word).mkString(" "))
            // Pass the string of tokens to the bigram space for processing.
            bigramSpace.processDocument(wordDoc.reader)
        }

        println("Processing space");
        // Process the full set of bigram statistics after every document has been
        // processed.
        bigramSpace.processSpace(System.getProperties)

        println("Printing space");
        // Extract the the bigram vector for each word observed in the corpus.  Each
        // vector corresponds to the first word in a bigram and records the PMI value
        // between that word and the secondary words in the retained bigrams.  After
        // extracting each bigram vector, turn it into a sparse matrix and save the
        // matrix to a file.
        val matrix = Matrices.asSparseMatrix(
            (for (w <- bigramSpace.getWords) yield bigramSpace.getVector(w)).toList)
        val writer = new MatlabSparseMatrixWriter();
        writer.writeMatrix(matrix, new FileOutputStream(args(2)))

        println("Printing basis");
        // Save the mapping from a word to it's row/column index in the serialzed matrix
        // file.
        SerializableUtil.save(basis, args(3))
    }
}
