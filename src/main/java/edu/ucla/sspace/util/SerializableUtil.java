/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.util;

import edu.ucla.sspace.vector.IntegerVector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


/**
 * A utility class for loading and saving typed {@link Serializable} objects
 * from files, streams and readers.  This class encapsulates all the common
 * serializing code and type casting to allow a clean interface for classes that
 * need to interact with serialized objects.  All checked {@link IOException}
 * cases are rethrown as {@link IOError}.
 */
public class SerializableUtil {

    /**
     * Uninstantiable
     */
    private SerializableUtil() { }

    /**
     * Serializes the object to the provided file.
     *
     * @param o the object to be stored in the file
     * @param file the file in which the object should be stored
     */
    public static void save(Object o, String file) {
        save(o, new File(file));
    }

    /**
     * Serializes the object to the provided file.
     *
     * @param o the object to be stored in the file
     * @param file the file in which the object should be stored
     */
    public static void save(Object o, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream outStream = 
                new ObjectOutputStream(new BufferedOutputStream(fos));
            outStream.writeObject(o);
            outStream.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }


    /**
     * Serializes the object to the provided stream.  This method does not close
     * the stream after writing.
     *
     * @param o the object to be stored in the file
     * @param stream the stream in which the object should be stored
     */
    public static void save(Object o, OutputStream stream) {
        try {
            ObjectOutputStream outStream = 
                (stream instanceof ObjectOutputStream)
                    ? (ObjectOutputStream)stream
                    : new ObjectOutputStream(stream);
            outStream.writeObject(o);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Serializes the object to a {@code byte} array.
     *
     * @param o the object to be stored in the file
     */
    public static byte[] save(Object o) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        save(o, baos);
        return baos.toByteArray();
    }

    /**
     * Loads a serialized object of the specifed type from the file.
     *
     * @param file the file from which a mapping should be loaded
     * @param type the type of the object being deserialized
     *
     * @return the object that was serialized in the file
     */
    public static <T> T load(File file, Class<T> type) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream inStream = 
                new ObjectInputStream(new BufferedInputStream(fis));
            T object = type.cast(inStream.readObject());
            inStream.close();
            return object;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IOError(cnfe);
        }
    }

    /**
     * Loads a serialized object of the specifed type from the file.
     *
     * @param fileName the file name from which an object should be loaded
     *
     * @return the object that was serialized in the file
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(String fileName) {
        return (T) load(new File(fileName));
    }

    /**
     * Loads a serialized object of the specifed type from the file.
     *
     * @param file the file from which a mapping should be loaded
     *
     * @return the object that was serialized in the file
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream inStream = 
                new ObjectInputStream(new BufferedInputStream(fis));
            T object = (T) inStream.readObject();
            inStream.close();
            return object;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IOError(cnfe);
        }
    }

    /**
     * Loads a serialized object of the specifed type from the stream.  This
     * method does not close the stream after reading
     *
     * @param file the file from which a mapping should be loaded
     * @param type the type of the object being deserialized
     *
     * @return the object that was serialized in the file
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(InputStream stream) {
        try {            
            ObjectInputStream inStream = (stream instanceof ObjectInputStream)
                ? (ObjectInputStream)stream
                : new ObjectInputStream(stream);
            T object = (T) inStream.readObject();
            return object;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IOError(cnfe);
        }
    }
}
