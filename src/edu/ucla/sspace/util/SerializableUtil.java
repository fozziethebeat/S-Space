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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.IOError;
import java.io.IOException;


/**
 * A utility class for loading and saving typed {@link Serializable} objects
 * from a file.  This class encapsulates all the common serializing code and
 * type casting to allow a clean interface for classes that need to interact
 * with serialized objects.  All checked {@link IOException} cases are rethrown
 * as {@link IOError}.
 */
public class SerializableUtil {

    /**
     * Uninstantiable
     */
    private SerializableUtil() { }

    /**
     * Serializes the object to a file with the provided file name.
     *
     * @param o the object to be stored in the file
     * @param file the file name in which the object should be stored
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
            save(o, new FileOutputStream(file));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Serializes the object to the provided {@link OutputStream}.
     *
     * @param o the object to be stored in the {@link OutputStream} 
     * @param stream the {@link OutputStream} in which the object should be
     *        stored
     */
    public static void save(Object o, OutputStream stream) {
        try {
            ObjectOutputStream outStream = new ObjectOutputStream(stream);
            outStream.writeObject(o);
            outStream.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Loads a serialized object of the specifed type from the file.
     *
     * @param file the file from which a mapping should be loaded
     * @param type the type of the object being deserialized
     *
     * @return the object that was serialized in the file
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(File file, Class<T> type) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream inStream = new ObjectInputStream(fis);
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
     * Loads a serialized object of the specifed type from the file.
     *
     * @param fileName the file name from which an object should be loaded
     *
     * @return the object that was serialized in the file
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(String fileName) {
        return load(new File(fileName));
    }

    /**
     * Loads a serialized object of the specifed type from the file.
     *
     * @param file the file from which an object should be loaded
     *
     * @return the object that was serialized in the file
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream inStream = new ObjectInputStream(fis);
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
     * Loads a serialized object of the specifed type from the {@link
     * InputStream}.
     *
     * @param file the {@link InputStream} from which an object should be loaded
     *
     * @return the object that was serialized in the {@link InputStream} 
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(InputStream file) {
        try {
            ObjectInputStream inStream = new ObjectInputStream(file);
            T object = (T) inStream.readObject();
            inStream.close();
            return object;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IOError(cnfe);
        }
    }
}
