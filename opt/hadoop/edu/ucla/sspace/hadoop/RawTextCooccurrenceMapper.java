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

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.HadoopResourceFinder;
import edu.ucla.sspace.util.ResourceFinder;

import java.io.IOException;
import java.io.IOError;

import java.util.Properties;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;

import static edu.ucla.sspace.text.IteratorFactory.ITERATOR_FACTORY_PROPERTIES;


/**
 * A {@link Mapper} implementation that maps a the text values of a document to
 * the word co-occurrences.  This class is intended to be used with the {@link
 * TextInputFormat} where the incoming text files are mapped to byte offsets and
 * the text contained there-in.  The input key values are not interpreted by
 * this mapper, only the text values.
 *
 * <p>This class defines the following configurable properties that may be set
 * using {@link Properties} constructor to {@link HadoopJob}.  Note that
 * setting these properties with the {@link System} properties will have no
 * effect on this class.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value edu.ucla.sspace.hadoop.CooccurrenceExtractor#WINDOW_SIZE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value edu.ucla.sspace.hadoop.CooccurrenceExtractor#DEFAULT_WINDOW_SIZE}
 *
 * <dd style="padding-top: .5em">This property sets the number of words before
 *      and after that are counted as co-occurring.  With the default value,
 *      {@value
 *      edu.ucla.sspace.hadoop.CooccurrenceExtractor#DEFAULT_WINDOW_SIZE} words
 *      are counted before and {@value
 *      edu.ucla.sspace.hadoop.CooccurrenceExtractor#DEFAULT_WINDOW_SIZE} words
 *      are counter after.  This class always uses a symmetric window. <p>
 *
 * </dl>
 *
 * @see HadoopJob#HadoopJob(Class,Class,Class,Class,Class,Class,Properties)
 */
public class RawTextCooccurrenceMapper 
        extends Mapper<LongWritable,Text,Text,TextIntWritable> {

    /**
     * The object responsible for performing all the tokenization and
     * co-occurrence extraction from a {@link Text} object.
     */
    private CooccurrenceExtractor extractor;
    
    public RawTextCooccurrenceMapper() { }

    /**
     * Initializes all the properties for this particular mapper.  This process
     * includes setting up the window size and configuring how the input
     * documents will be tokenized.
     */
    protected void setup(Mapper.Context context) {
        Configuration conf = context.getConfiguration();
        extractor = new CooccurrenceExtractor(conf);

        // Set up the IteratorFactory properties           
        Properties props = new Properties();
        for (String property : ITERATOR_FACTORY_PROPERTIES) {
            String propVal = conf.get(property);
            if (propVal != null)
                props.setProperty(property, propVal);
        }
        
        // Create the ResourceFinder that the IteratorFactory will use to find
        // the various files on HDFS
        ResourceFinder hadoopRf = null;
        try {
            hadoopRf = new HadoopResourceFinder(conf);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        // Set the IteratorFactory to locate the resources and then have it
        // reconfigure itself based on the user specified properties
        IteratorFactory.setResourceFinder(hadoopRf);        
        IteratorFactory.setProperties(props);
    }


    /**
     * Processes the tokens in the {@code value} and writes a set of tuples
     * mapping a word to the other words it co-occurs with and the relative
     * position of those co-occurrences.  The key to this method is ignored.
     *
     * @param key the byte offset of the document in the input corpus
     * @param value the document that will be segmented into tokens and
     *        mapped to cooccurrences
     * @param context the context in which this mapper is executing
     */
    public void map(LongWritable key, Text value, Context context) 
            throws IOException, InterruptedException {
        extractor.processDocument(value, context);
    }

}