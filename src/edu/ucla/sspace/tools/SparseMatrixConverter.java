/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.tools;

import java.io.*;
import java.util.*;

public class SparseMatrixConverter {

    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(args[0]));

            Map<Integer,Integer> colToNonZero = 
                new HashMap<Integer,Integer>();

            // read through once to get matrix dimensions
            int rows = 0, cols = 0, nonZero = 0;                
            for (String line = null; (line = br.readLine()) != null; ) {
                String[] rowColVal = line.split("\\s+");
                int row = Integer.parseInt(rowColVal[0]);
                int col = Integer.parseInt(rowColVal[1]);
                if (row > rows)
                    rows = row;
                if (col > cols)
                    cols = col;
                ++nonZero;
                Integer colCount = colToNonZero.get(col);
                colToNonZero.put(col, (colCount == null) ? 1 : colCount + 1);
            }
            br.close();

            // Matlab indices are indexed starting at 1, while SVDLIBC start at
            // 0, so decrement the total number of rows and columns
            --rows;
            --cols;

            br = new BufferedReader(new FileReader(args[0]));

            // loop through a second time and convert each of the rows into its
            // SVDLIBC sparse format
            System.out.println(rows + "\t" + cols + "\t" + nonZero);
            int lastCol = 0;
            for (String line = null; (line = br.readLine()) != null; ) {
                String[] rowColVal = line.split("\\s+");
                int col = Integer.parseInt(rowColVal[1]);
                if (col != lastCol) {
                    // print any missing colums in case not all the columns have
                    // data
                    for (int i = lastCol + 1; i < col; ++i) {
                        System.out.println(0);
                    }
                    // print the new header
                    int colCount = colToNonZero.get(col);
                    lastCol = col;
                    System.out.println(colCount);                    
                }
                System.out.println(rowColVal[0] + "\t" + rowColVal[2]);
            }
            br.close();
            System.out.flush();
            
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
