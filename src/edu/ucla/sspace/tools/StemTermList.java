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

import edu.ucla.sspace.text.EnglishStemmer;
import edu.ucla.sspace.text.Stemmer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * A simple utility for stemming a list of terms.  Given an input file that has
 * one word per line, this utility will stem each term and write the stemmed
 * version to the output file.
 *
 * @author Keith Stevens
 */
public class StemTermList {

    public static void main(String args[]) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: StemTermList <in-term-list> <out-term>");
            System.exit(1);
        }

        Stemmer stemmer = new EnglishStemmer();

        PrintWriter writer = new PrintWriter(args[1]);

        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        for (String line = null; (line=br.readLine()) != null; )
            writer.println(stemmer.stem(line.trim()));
        writer.close();
    }
}
