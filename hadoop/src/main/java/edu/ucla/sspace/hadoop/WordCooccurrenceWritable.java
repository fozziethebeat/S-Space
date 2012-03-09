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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;


/**
 * A {@link Writable} that represents the occurrence of a two words a certain
 * distance apart.
 */
public class WordCooccurrenceWritable 
    implements WritableComparable<WordCooccurrenceWritable> {
    
    /**
     * The first word that occurred.
     */
    private Text w1;

    /**
     * The co-occurring word.
     */
    private Text w2;

    /**
     * The distance between {@code w1} and {@code w2}.  If {@code w2} occurs
     * before, this distance is negative.
     */
    private int distance;

    /**
     * Creates an empty {@code WordCooccurrenceWritable} with no text and a
     * distance of 0.  This constructor is intended to facilitate the
     * serialization methods used by Hadoop.
     */
    public WordCooccurrenceWritable() { 
        w1 = new Text();
        w2 = new Text();
        distance = 0;
    }

    public WordCooccurrenceWritable(String word1, String word2, int distance) {
        w1 = new Text(word1);
        w2 = new Text(word2);
        this.distance = distance;
    }

    public WordCooccurrenceWritable(Text word1, Text word2, int distance) {
        w1 = word1;
        w2 = word2;
        this.distance = distance;
    }        

    public static WordCooccurrenceWritable read(DataInput in) 
            throws IOException {
        WordCooccurrenceWritable wow = new WordCooccurrenceWritable();
        wow.w1.readFields(in);
        wow.w2.readFields(in);
        wow.distance = in.readInt();
        return wow;
    }

    public int compareTo(WordCooccurrenceWritable o) {
        int c = w1.compareTo(o.w1);
        if (c != 0)
            return c;
        c = w2.compareTo(o.w2);
        if (c != 0)
            return c;
        return distance - o.distance;
    } 

    public int hashCode() {
        return w1.hashCode() 
            ^ w2.hashCode()
            ^ distance;
    }

    public boolean equals(Object o) {
        if (o instanceof WordCooccurrenceWritable) {
            WordCooccurrenceWritable wow = (WordCooccurrenceWritable)o;
            return w1.equals(wow.w1) &&
                w2.equals(wow.w2) &&
                distance == wow.distance;
        }
        return false;
    }
        
    public void readFields(DataInput in) throws IOException {
        w1.readFields(in);
        w2.readFields(in);
        distance = in.readInt();
    }

    public void write(DataOutput out) throws IOException {
        w1.write(out);
        w2.write(out);
        out.writeInt(distance);
    }

    public String toString() {
        return w1 + "\t" + w2 + "\t" + distance;
    }
}    
