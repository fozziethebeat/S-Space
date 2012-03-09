/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
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


package edu.ucla.sspace.tools;


import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.UkWacDependencyFileIterator;

import java.util.Iterator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * @author Keith Stevens
 */
public class PUkWacSentenceStripper {

    public static void main(String[] args) throws IOException {
        Iterator<Document> ukWacIter = new UkWacDependencyFileIterator(args[0]);

        PrintWriter writer = new PrintWriter(args[1]);
        StringBuilder builder = new StringBuilder();
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        while (ukWacIter.hasNext()) {
            BufferedReader doc = ukWacIter.next().reader();
            for (DependencyTreeNode[] tree = null;
                 (tree = extractor.readNextTree(doc)) != null; ) {
                for (DependencyTreeNode node : tree)
                    builder.append(node.word()).append(" ");
            }
            writer.println(builder.toString());
            builder = new StringBuilder();
        }
    }
}
