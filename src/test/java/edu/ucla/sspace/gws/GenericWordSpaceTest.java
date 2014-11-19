/*
 * Copyright 2014 David Jurgens
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

package edu.ucla.sspace.gws;

import edu.ucla.sspace.matrix.*;

import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link GenericWordSpace} 
 */
public class GenericWordSpaceTest {

    static String[] docArr = new String[] {
        "shipment of gold damaged in a fire",
        "delivery of silver arrived in a silver truck",
        "shipment of gold arrived in a truck"
    };


    @Test public void testConstructor() {
        GenericWordSpace gws = new GenericWordSpace();
   }

    @Test public void testProcessDocument() throws Exception {
        GenericWordSpace gws = new GenericWordSpace();
        for (String doc : Arrays.asList(docArr))
            gws.processDocument(new BufferedReader(new StringReader(doc)));
        assertEquals(11, gws.getWords().size());
        assertEquals(11, gws.getVector("fire").length());
   }

    @Test public void testTransform() throws Exception {
        GenericWordSpace gws = new GenericWordSpace();
        for (String doc : Arrays.asList(docArr))
            gws.processDocument(new BufferedReader(new StringReader(doc)));
        gws.processSpace(new TfIdfTransform());
        assertEquals(11, gws.getWords().size());
        assertEquals(11, gws.getVector("fire").length());       
    }

    @Test public void testDoubleTransform() throws Exception {
        GenericWordSpace gws = new GenericWordSpace();
        for (String doc : Arrays.asList(docArr))
            gws.processDocument(new BufferedReader(new StringReader(doc)));
        gws.processSpace(new NoTransform());
        gws.processSpace(new NoTransform());
        assertEquals(11, gws.getWords().size());
        assertEquals(11, gws.getVector("fire").length());       
    }

    @Test(expected=IllegalStateException.class) 
    public void testProcessDocAfterTransform() throws Exception {
        GenericWordSpace gws = new GenericWordSpace();
        for (String doc : Arrays.asList(docArr))
            gws.processDocument(new BufferedReader(new StringReader(doc)));
        gws.processSpace(new NoTransform());
        gws.processDocument(new BufferedReader(new StringReader("in a")));
    }    
}
