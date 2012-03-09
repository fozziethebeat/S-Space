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

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.index.IntegerVectorGenerator;
import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.TernaryPermutationFunction;

import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.DenseIntVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.logging.Logger;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;

/**
 * A Hadoop utility class for executing a job in the Hadoop map-reduce engine.
 * Users specify which classes to use for the mapping and reducing tasks and are
 * then upon job completion, are return the files generated as output by those
 * jobs.  {@link Mapper} and {@link Reducer} instances may be configured by
 * passing a {@link Properties} argument to this class's constructor.  Any
 * properties specified in this object are copied into the {@link Configuration}
 * used for each job invocation.
 *
 * @author David Jurgens
 */
public class HadoopJob {
    
    /**
     * The configuration used by the Mapper and Reducer instances for running.
     * The parameters are set at Job creation time and then customized based on
     * the execution's input parameters.
     */
    private final Configuration conf;

    /**
     * The mapper class that will be used to map key-value pairs for the jobs.
     */
    private final Class<? extends Mapper> mapperClass;

    /**
     * The reducer class that will be used to reduce key-value for the jobs.
     */
    private final Class<? extends Reducer> reducerClass;

    private final Class<?> mapperOutputKey;

    private final Class<?> mapperOutputValue;

    private final Class<?> outputKey;

    private final Class<?> outputValue;

    /**
     * Creates a {@code HadoopJob} using the System properties for
     * configuring any of the mapper or reducer parameters.
     *
     * @param mapperClass the mapper to use in Hadoop jobs exectued by this
     *        runner.
     * @param reducerClass the reducer to use in Hadoop jobs exectued by this
     *        runner.
     */
    public HadoopJob(Class<? extends Mapper> mapperClass, 
                     Class<?> mapperOutputKey,
                     Class<?> mapperOutputValue,
                     Class<? extends Reducer> reducerClass,
                     Class<?> outputKey,
                     Class<?> outputValue) {
        this(mapperClass, mapperOutputKey, mapperOutputValue,
             reducerClass, outputKey, outputValue,
             System.getProperties());
    }

    /**
     * Creates a {@code HadoopJob} using the provided properties for
     * configuring any of the mapper or reducer parameters.
     *
     * @param mapperClass the mapper to use in Hadoop jobs exectued by this
     *        runner.
     * @param reducerClass the reducer to use in Hadoop jobs exectued by this
     *        runner.
     * @param props the properties to pass on to the mapper and reducer
     *        instances
     */
    public HadoopJob(Class<? extends Mapper> mapperClass, 
                     Class<?> mapperOutputKey,
                     Class<?> mapperOutputValue,
                     Class<? extends Reducer> reducerClass,
                     Class<?> outputKey,
                     Class<?> outputValue,
                     Properties props) {
        this.mapperClass = mapperClass;
        this.mapperOutputKey = mapperOutputKey;
        this.mapperOutputValue = mapperOutputValue;

        this.reducerClass = reducerClass;
        this.outputKey = outputKey;
        this.outputValue = outputValue;
        conf = new Configuration();

        for (String prop : props.stringPropertyNames()) {
            String value = props.getProperty(prop);
            if (value != null)
                conf.set(prop, value);
        }
    }

    /**
     * Exceutes the word co-occurrence counting job on the corpus files in the
     * input directory using the current Hadoop instance, returning an iterator
     * over all the occurrences frequences found in the corpus.
     *
     * @param inputPaths the directories on the Hadoop distributed file system
     *        containing all the corpus files that will be processed
     *
     * @return an iterator over the unique {@link WordCooccurrence} counts found
     *         in the corpus.  Note that if two words co-occur the same distance
     *         apart multiple times, only one {@code WordCooccurrence} is
     *         returned, where the number of co-occurrences is reflected by the
     *         the {@link WordCooccurrence#getCount() getCount()} method.
     *
     * @throws Exception if Hadoop throws an {@code Exception} during its
     *         execution or if the resulting output files cannot be read.
     */
    public HadoopJobResults run(Collection<String> inputPaths)
            throws Exception {

        // Create a mostly unique file name for the output directory.
        String outputDir = "output-" + System.currentTimeMillis();
        //conf.setBoolean("mapred.task.profile", true);

        Job job = new Job(conf, mapperClass.getName() + "-" 
                          + reducerClass.getName());
	
        job.setJarByClass(HadoopJob.class);
        job.setMapperClass(mapperClass);
        job.setReducerClass(reducerClass);
	
        job.setMapOutputKeyClass(mapperOutputKey);
        job.setMapOutputValueClass(mapperOutputValue);
        job.setOutputKeyClass(outputKey);
        job.setOutputValueClass(outputValue);
        
        // Add all the specified directories as input paths for the job
        for (String inputDir : inputPaths) 
            FileInputFormat.addInputPath(job, new Path(inputDir));
        Path outputDirPath = new Path(outputDir);
        FileOutputFormat.setOutputPath(job, outputDirPath);
	
        job.waitForCompletion(true);

        // From the output directory, collect all the results files 
        FileSystem fs = FileSystem.get(conf);
        FileStatus[] outputFiles = 
            fs.listStatus(outputDirPath, new OutputFilePathFilter());
        Collection<Path> paths = new LinkedList<Path>();
        for (FileStatus status : outputFiles) {
            paths.add(status.getPath());
        }
        
        return new HadoopJobResults(fs, paths);
    }

    /**
     * A private {@link PathFilter} implementation designed to only accept
     * output files from the reducer.
     */
    private static class OutputFilePathFilter implements PathFilter {
        
        /**
         * Returns {@code true} if the path begins the prefix for output files
         * from the reducer.
         */
        public boolean accept(Path p) {
            return p.getName().startsWith("part-r-");
        }
    }
}