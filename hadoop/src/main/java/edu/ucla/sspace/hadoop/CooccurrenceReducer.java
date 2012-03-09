/*
 * Copyright 2010 David Jurgens
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

package edu.ucla.sspace.hadoop;

import edu.ucla.sspace.util.Duple;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;


/**
 * A {@link Reducer} that transforms the co-occurrence of they input key
 * with another word at a certan position to a count of how many times that
 * co-occurrence took place.
 */
public class CooccurrenceReducer
    extends Reducer<Text,TextIntWritable,WordCooccurrenceWritable,IntWritable> {

    public CooccurrenceReducer() { }

    public void reduce(Text occurrence,
                       Iterable<TextIntWritable> values, Context context)
        throws IOException, InterruptedException {
        
        // Record how many times a particular co-occurrence with a word at an
        // offet happpened
        Map<Duple<String,Integer>,Integer> cooccurrenceToCount = 
            new HashMap<Duple<String,Integer>,Integer>();

        // Loop through each of the co-occurrences, updating the counts
        for (TextIntWritable cooccurrence : values) {
            Duple<String,Integer> tuple = new Duple<String,Integer>(
                cooccurrence.t.toString(), cooccurrence.position);
            Integer count = cooccurrenceToCount.get(tuple);
            cooccurrenceToCount.put(tuple, (count == null) ? 1 : count + 1);
        }

        for (Map.Entry<Duple<String,Integer>,Integer> e : 
                 cooccurrenceToCount.entrySet()) {
            Duple<String,Integer> d = e.getKey();
            context.write(new WordCooccurrenceWritable(
                          occurrence, new Text(d.x), d.y), 
                          new IntWritable(e.getValue()));
        }
    }
}
