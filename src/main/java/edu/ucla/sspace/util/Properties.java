/*
 * Copyright 2010 Keith Stevens 
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


/**
 * A simple wrapper around {@link java.util.Properties} that provides methods
 * that interpret properties based on the type of the default value.
 *
 * @author Keith Stevens
 */
public class Properties {

  /**
   * The underlying properties intsance.
   */
  private final java.util.Properties props;

  /**
   * Creates a new {@link Properties} object from the system provided
   * properties.
   */
  public Properties() {
    this(System.getProperties());
  }

  /**
   * Creates a new {@link Properties} object from the provided properties.
   */
  public Properties(java.util.Properties props) {
    this.props = props;
  }

  /**
   * Returns the string property associated with {@code propName}
   */
  public String getProperty(String propName) {
    return props.getProperty(propName);
  }

  /**
   * Returns the string property associated with {@code propName}, or {@code
   * defaultValue} if there is no property.
   */
  public String getProperty(String propName, String defaultValue) {
    return props.getProperty(propName, defaultValue);
  }

  /**
   * Returns the integer value of the property associated with {@code propName},
   * or {@code defaultValue} if there is no property.
   */
  public int getProperty(String propName, int defaultValue) {
    String propValue = props.getProperty(propName);
    return (propValue == null) ? defaultValue : Integer.parseInt(propValue);
  }

  /**
   * Returns the double value of the property associated with {@code propName},
   * or {@code defaultValue} if there is no property.
   */
  public double getProperty(String propName, double defaultValue) {
    String propValue = props.getProperty(propName);
    return (propValue == null) ? defaultValue : Double.parseDouble(propValue);
  }

  /**
   * Returns a class instance of the property associated with {@code propName},
   * or {@code defaultValue} if there is no property.  This method assumes that
   * the class has a no argument constructor.
   */
  @SuppressWarnings("unchecked")
  public <T> T getProperty(String propName, T defaultValue) {
    String propValue = props.getProperty(propName);
    if (propValue == null)
      return defaultValue;
    return (T) ReflectionUtil.getObjectInstance(propValue);
  }
}
