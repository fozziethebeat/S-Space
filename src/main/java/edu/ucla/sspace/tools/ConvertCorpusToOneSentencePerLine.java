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

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * A utility tool for converting a corpus into a one-sentence-per-line format.
 * This tool is not guaranteed to be accurate, but empirically performs quite
 * well.  This tool prints the converted corpus directly to {@code stdout}.
 */ 
public class ConvertCorpusToOneSentencePerLine {
    
    public static void main(String[] args) {
	if (args.length == 0) { 
	    System.out.println("usage: java <class name> <input file>");
	    return;
	}

	try {
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    StringBuilder sb = new StringBuilder();
	    boolean inQuotation = false;
	    for (String line = null; (line = br.readLine()) != null; ) {
		String[] tokens = line.split("\\s+");
		for (String token : tokens) {

		    if (token.startsWith("\"")) {
			inQuotation = true;
		    }

		    if (token.endsWith(".\"")) {
			inQuotation = false;
		    }
		    
		    // look for end of setnences
		    if (!inQuotation && 
			token.endsWith("!") || 
			token.endsWith("?") || 
			token.endsWith(".\"") ||
			(token.endsWith(".") 
			 && !isInitial(token) 
			 && !isAbbreviation(token))) {


			sb.append(token);
			System.out.println(sb.toString());
			sb = new StringBuilder();
		    }
		    else {
			sb.append(token).append(" ");
		    }
		}
	    }
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    public static boolean isInitial(String token) {
	return token.length() == 2 &&
	    // if it's already in upper case
	    token.toUpperCase().equals(token);
    }

    public static boolean isAbbreviation(String token) {
	String s = token.toLowerCase();
	return 
	    // titles
	    s.equals("mr.") ||
	    s.equals("ms.") ||
	    s.equals("mrs.") ||
	    s.equals("dr.") ||
	    s.equals("drs.") ||
	    s.equals("fr.") ||

	    // surnames
	    s.equals("jr.") ||
	    s.equals("sr.") ||

	    // streets
	    s.equals("st.") ||
	    s.equals("ave.") ||
	    s.equals("abbr.") ||
	    
	    // latin
	    s.equals("i.e.") ||
	    s.equals("e.g.") ||

	    // months
	    s.equals("jan.") ||
	    s.equals("feb.") ||
	    s.equals("apr.") ||
	    s.equals("jun.") ||
	    s.equals("jul.") ||
	    s.equals("aug.") ||
	    s.equals("sep.") ||
	    s.equals("sept.") ||
	    s.equals("oct.") ||
	    s.equals("nov.") ||
	    s.equals("desc.") ||

	    // degrees
	    s.equals("b.a.") ||
	    s.equals("b.s.") ||
	    s.equals("m.s.") ||
	    s.equals("ph.d.") ||
	    
	    // measures
	    s.equals("in.") ||
	    s.equals("ft.") ||
	    s.equals("lbs.") ||
	    s.equals("gal.") ||
	    s.equals("min.") ||
	    	   
	    // other
	    s.equals("assn.") ||
	    s.equals("c.") || // circa
	    s.equals("corp.") ||
	    s.equals("col.") ||
	    s.equals("cpl.") ||
	    s.equals("d.") || // died
	    s.equals("dist.") ||
	    s.equals("inst.") ||
	    s.equals("lt.") ||
	    s.equals("msgr.") ||
	    s.equals("pl.") ||
	    s.equals("vol.") ||	    
	    s.equals("wt.") ||	    
	    
	    // places
	    s.equals("d.c.") ||
	    s.equals("n.y.c.");
    }

}