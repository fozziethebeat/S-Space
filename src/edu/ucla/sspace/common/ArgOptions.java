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

package edu.ucla.sspace.common;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * A utility class for parsing command line arguments.
 *
 * @author David Jurgens
 */
public class ArgOptions {

    //
    // Positional args and optionsToValue only contain data after processArgs
    // has been called.
    //

    /**
     * The list of positional args
     */
    private final List<String> positionalArgs;

    /**
     * A mapping from each option specified on the command line to the value
     * that it was paired with or {@code null} if that option did not take a
     * value
     */
    private final Map<Option,String> optionToValue;

    //
    // Data for three Map instances is added from the addOption() methods
    //

    /**
     * A mapping from a {@code String} name for the option to the the {@code
     * Option}.
     */
    private final Map<String,Option> longNameToOption;

    /**
     * A mapping from a {@code char} abbreviation for the option to the the
     * {@code Option}.
     */
    private final Map<Character,Option> shortNameToOption;

    /**
     * A mapping to the group name for a set of options.  Note that if no group
     * name is provided the options are mapped from the group {@code null}.
     */
    private final Map<String,Set<Option>> groupToOptions;

    /**
     * Constructs an empty {@code ArgOptions} with no options available.
     */
    public ArgOptions() {
        positionalArgs = new ArrayList<String>();;
        optionToValue = new HashMap<Option,String>();
        longNameToOption = new HashMap<String,Option>();
        shortNameToOption = new HashMap<Character,Option>();
        // use a linked for stable iteration ordering
        groupToOptions = new LinkedHashMap<String,Set<Option>>();
    }

    /**
     * Creates an option that is in the default group and takes no values and
     * adds to the set of possible options.
     *
     * @param shortName a one character short name for specify this option.
     * @param longName a string name for specifying this option.
     * @param description an optional descritpion for this option, or {@code
     *        null} if no description is to be provided.
     *
     * @throws IllegalArgumentException if <ul> <li> If another option with
     *         either the same {@code shortName} or the same {@code longName}
     *         has been already been added. <li>longName has length 1. </ul>
     */
    public void addOption(char shortName,
                          String longName,
                          String description) {
        addOption(shortName, longName, description, false, null, null);
    }

    /**
     * Creates an option in the default group using the provided parameters and
     * adds it to the set of possible options.
     *
     * @param shortName a one character short name for specify this option.
     * @param longName a string name for specifying this option.
     * @param description an optional descritpion for this option, or {@code
     *        null} if no description is to be provided.
     * @param hasValue whether this option takes a value.
     * @param valueName if this option takes a value, the name of that value
     *
     * @throws IllegalArgumentException if <ul> <li> If another option with
     *         either the same {@code shortName} or the same {@code longName}
     *         has been already been added. <li>longName has length 1 <li>
     *         {@code hasArg} is {@code true} but {@code valueName} is {@code
     *         null}.  </ul>
     */
    public void addOption(char shortName,
                          String longName,
                          String description,
                          boolean hasValue,
                          String valueName) {
        addOption(shortName, longName, description, hasValue, valueName, null);
    }


    /**
     * Creates an option based on the provided parameters and adds the option to
     * the set of possible options.
     *
     * @param shortName a one character short name for specify this option.
     * @param longName a string name for specifying this option.
     * @param description an optional descritpion for this option, or {@code
     *        null} if no description is to be provided.
     * @param hasValue whether this option takes a value.
     * @param valueName if this option takes a value, the name of that value
     * @param optionGroupName the name of a group if this option is part of a
     *        specific subset of the options, or {@code null} if this is a
     *        generic option
     *
     * @throws IllegalArgumentException if <ul> <li> If another option with
     *         either the same {@code shortName} or the same {@code longName}
     *         has been already been added. <li>longName has length 1 <li>
     *         {@code hasArg} is {@code true} but {@code valueName} is {@code
     *         null}.  </ul>
     */
    public void addOption(char shortName,
                          String longName,
                          String description,
                          boolean hasValue,
                          String valueName,
                          String optionGroupName) {
 
        if (longName != null && longName.length() == 1) {
            throw new IllegalArgumentException(
                "long name must be at least two characters");
        }
        
        if (hasValue && valueName == null) {
            throw new IllegalArgumentException(
                "value name must be supposed");
        }

        Option o = new Option(shortName, longName, description, valueName);
        
        if (shortNameToOption.containsKey(shortName) || 
            (longName != null && longNameToOption.containsKey(longName))) {
            throw new IllegalArgumentException(
                "Already specified value with same name");
        }
        else {
            shortNameToOption.put(shortName, o);
            if (longName != null) {
                longNameToOption.put(longName, o);
            }
            Set<Option> groupMembers = groupToOptions.get(optionGroupName);
            if (groupMembers == null) {
                groupMembers = new TreeSet<Option>();
                groupToOptions.put(optionGroupName, groupMembers);
            }
            groupMembers.add(o);
        }
    }


    /**
     *
     *
     * @param commandLine the set of string arguments provided on the command
     *        line
     */
    public void parseOptions(String[] commandLine) {

        for (int i = 0; i < commandLine.length; ++i) {
            String s = commandLine[i];

            if (s.startsWith("--")) {
                // see if the option is combined with a value
                int index = s.indexOf("=");
                if (index > 0) {
                    String optionName = s.substring(2, index);
                    String value = s.substring(index + 1);
                    if (value.length() == 0) {
                        throw new Error("no value specified for " + optionName);
                    }
                    Option o = longNameToOption.get(optionName);
                    if (o == null) {
                        throw new IllegalArgumentException(
                            "unrecognizedOption: " + s);
                    }
                    if (!o.hasValue()) {
                        throw new Error("Option " + optionName + " does not " +
                                        "take a value");
                    }
                    optionToValue.put(o, value);
                }

                // the option was not combined with a value, but may still
                // require one, so load the Option and then decide
                else {
                    String optionName = s.substring(2);
                    Option o = longNameToOption.get(optionName);
                    if (o == null) {
                        throw new IllegalArgumentException(
                            "unrecognizedOption: " + s);
                    }
                    if (o.hasValue()) {
                        // because the value combined case was handled above,
                        // the value must be in the next command line argument
                        // position
                        if (i + 1 >= commandLine.length) {
                            throw new Error("no value specified for " +
                                            optionName);
                        }
                        else {
                            String value = commandLine[++i];
                            optionToValue.put(o, value);
                        }
                    }
                    // no value was required for this option.
                    else {
                        optionToValue.put(o, null);
                    }
                }
            }
            
            // Single character options are more complex as they require
            // handling multiple single character options per string as well as
            // having the value specified as a part of the option string with no
            // intervening '=' character.
            else if (s.startsWith("-")) {
                
                for (int j = 1; j < s.length(); ++j) {
                    
                    char optionName = s.charAt(j);
                    Option o = shortNameToOption.get(optionName);
                    if (o == null) {
                        throw new IllegalArgumentException(
                            "unrecognizedOption: " + s);
                    }
                    if (o.hasValue()) {
                        // see if there are remaining characters as a part of
                        // this string and if so, use them as the value,
                        // e.g. -xtrue
                        if (j + 1 < s.length()) {
                            String value = s.substring(j + 1);
                            optionToValue.put(o, value);
                        }
                        
                        // otherwise, the next argument in command line is the
                        // value for this option
                        else {
                            // check to ensure that there is at least one more
                            // value
                            if (i + 1 == commandLine.length) {
                                throw new Error("no value specified for " + o);
                            }
                            String value = commandLine[++i];
                            optionToValue.put(o, value);
                        }
                        
                        break;
                    }
                    else {
                        optionToValue.put(o, null);
                    }
                }
            }
            
            // otherwise the argument isn't prefixed with any option-indicating
            // '-', so add it as a positional argument
            else {
                positionalArgs.add(s);
            }
        }
    }

    /**
     * Returns the number of positional arguments specified on the command line.
     */
    public int numPositionalArgs() {
        return positionalArgs.size();
    }

    /**
     * Returns the number of options that were specified on the command line.
     */
    public int numProvidedOptions() {
        return optionToValue.size();
    }

    /**
     * Returns the argument that was provided at the position based on the
     * original ordering.
     */
    public String getPositionalArg(int argNum) {
        return positionalArgs.get(argNum);
    }

    /**
     * Returns the positional arguments in the order they were provided.
     */
    public List<String> getPositionalArgs() {
        return positionalArgs;
    }


    private Option getOption(char shortName) {
        Option o = shortNameToOption.get(shortName);
        if (o == null) {
            throw new IllegalArgumentException(
                "no such option name: " + shortName);
        }
        return o;
    }

    private Option getOption(String longName) {
        Option o = longNameToOption.get(longName);
        if (o == null) {
            throw new IllegalArgumentException(
                "no such option name: " + longName);
        }
        return o;
    }

    public double getDoubleOption(char shortName) {
        Option o = getOption(shortName);
        if (optionToValue.containsKey(o)) {
            return Double.parseDouble(optionToValue.get(o));
        }
        else {
            throw new IllegalArgumentException(
                "Option -" + shortName + " does not have a value");
        }
    }

    public double getDoubleOption(char shortName, double defaultValue) {
        Option o = getOption(shortName);
        return optionToValue.containsKey(o)
            ? Double.parseDouble(optionToValue.get(o))
            : defaultValue;
    }

    public double getDoubleOption(String optionName) {
        Option o = getOption(optionName);
        if (optionToValue.containsKey(o)) {
            return Double.parseDouble(optionToValue.get(o));
        }
        else {
            throw new IllegalArgumentException(
                "Option " + optionName + " does not have a value");
        }
    }

    public double getDoubleOption(String optionName, double defaultValue) {
        Option o = getOption(optionName);
        return optionToValue.containsKey(o)
            ? Double.parseDouble(optionToValue.get(o))
            : defaultValue;
    }

    public long getLongOption(char shortName) {
        Option o = getOption(shortName);
        if (optionToValue.containsKey(o)) {
            return Long.parseLong(optionToValue.get(o));
        }
        else {
            throw new IllegalArgumentException(
                "Option -" + shortName + " does not have a value");
        }
    }

    public long getLongOption(char shortName, long defaultValue) {
        Option o = getOption(shortName);
        return optionToValue.containsKey(o)
            ? Long.parseLong(optionToValue.get(o))
            : defaultValue;
    }

    public long getLongOption(String optionName) {
        Option o = getOption(optionName);
        if (optionToValue.containsKey(o)) {
            return Long.parseLong(optionToValue.get(o));
        }
        else {
            throw new IllegalArgumentException(
                "Option " + optionName + " does not have a value");
        }
    }

    public long getLongOption(String optionName, long defaultValue) {
        Option o = getOption(optionName);
        return optionToValue.containsKey(o)
            ? Long.parseLong(optionToValue.get(o))
            : defaultValue;
    }

    public int getIntOption(char shortName) {
        Option o = getOption(shortName);
        if (optionToValue.containsKey(o)) {
            return Integer.parseInt(optionToValue.get(o));
        }
        else {
            throw new IllegalArgumentException(
                "Option -" + shortName + " does not have a value");
        }
    }

    public int getIntOption(char shortName, int defaultValue) {
        Option o = getOption(shortName);
        return optionToValue.containsKey(o)
            ? Integer.parseInt(optionToValue.get(o))
            : defaultValue;
    }

    public int getIntOption(String optionName) {
        Option o = getOption(optionName);
        if (optionToValue.containsKey(o)) {
            return Integer.parseInt(optionToValue.get(o));
        }
        else {
            throw new IllegalArgumentException(
                "Option " + optionName + " does not have a value");
        }
    }

    public int getIntOption(String optionName, int defaultValue) {
        Option o = getOption(optionName);
        return optionToValue.containsKey(o)
            ? Integer.parseInt(optionToValue.get(o))
            : defaultValue;
    }

    public boolean getBooleanOption(char shortName) {
        Option o = getOption(shortName);
        if (optionToValue.containsKey(o)) {
            return Boolean.parseBoolean(optionToValue.get(o));
        }
        else {
            throw new IllegalArgumentException(
                "Option -" + shortName + " does not have a value");
        }
    }

    public boolean getBooleanOption(char shortName, boolean defaultValue) {
        Option o = getOption(shortName);
        return optionToValue.containsKey(o)
            ? Boolean.parseBoolean(optionToValue.get(o))
            : defaultValue;
    }

    public boolean getBooleanOption(String optionName) {
        Option o = getOption(optionName);
        if (optionToValue.containsKey(o)) {
            return Boolean.parseBoolean(optionToValue.get(o));
        }
        else {
            throw new IllegalArgumentException(
                "Option " + optionName + " does not have a value");
        }
    }

    public boolean getBooleanOption(String optionName, boolean defaultValue) {
        Option o = getOption(optionName);
        return optionToValue.containsKey(o)
            ? Boolean.parseBoolean(optionToValue.get(o))
            : defaultValue;
    }

    public String getStringOption(char shortName) {
        Option o = getOption(shortName);
        if (optionToValue.containsKey(o)) {
            return optionToValue.get(o);
        }
        else {
            throw new IllegalArgumentException(
                "Option -" + shortName + " does not have a value");
        }
    }

    public String getStringOption(char shortName, String defaultValue) {
        Option o = getOption(shortName);
        return optionToValue.containsKey(o)
            ? optionToValue.get(o)
            : defaultValue;
    }

    public String getStringOption(String optionName) {
        Option o = getOption(optionName);
        if (optionToValue.containsKey(o)) {
            return optionToValue.get(o);
        }
        else {
            throw new IllegalArgumentException(
                "Option " + optionName + " does not have a value");
        }
    }

    public String getStringOption(String optionName, String defaultValue) {
        Option o = getOption(optionName);
        return optionToValue.containsKey(o)
            ? optionToValue.get(o)
            : defaultValue;
    }

    public boolean hasOption(String optionName) {
        Option o = longNameToOption.get(optionName);
        return (o == null) ? false : optionToValue.containsKey(o);
    }

    public boolean hasOption(char shortName) {
        Option o = shortNameToOption.get(shortName);
        return (o == null) ? false : optionToValue.containsKey(o);
    }

    /**
     * Returns a pretty-print formated string describing all of the options and
     * their arguments.
     */
    public String prettyPrint() {
        // make a pass through to find out how wide the printing needs to be
        int maxNameLength = -1;
        int maxDescLength = -1;
        int descriptionStart = -1;
        for (Option o : shortNameToOption.values()) {
            String longName = o.longName;
            String valDesc = o.valueName;
            if (longName != null) {
                int length = (valDesc == null) 
                    ? longName.length()
                    : longName.length() + valDesc.length();
                if (length > maxNameLength) {
                    maxNameLength = length;
                }
            }
            String desc = o.description;
            if (desc != null && maxDescLength < desc.length()) {
                maxDescLength = desc.length();
            }
        }
        // Where do these 9 extra characters come from?
        descriptionStart = maxNameLength + 4 + 9;
        
        StringBuilder sb = new StringBuilder(100);

        // start with regular options
        Set<Option> regular = groupToOptions.get(null);

        // check to ensure that the user defined at least one regular option
        if (regular != null) {
            printOption(regular, "Options",
                        maxNameLength, descriptionStart, sb);
        }

        // now print the remaining options
        for (Map.Entry<String,Set<Option>> e : groupToOptions.entrySet()) {
            if (e.getKey() == null)
                continue;
            printOption(e.getValue(), e.getKey(),
                        maxNameLength, descriptionStart, sb);
        }

        return sb.toString();
    }

    /**
     * Returns a string description of the options that are currently set and
     * their values.
     */
    public String toString() {
        StringBuilder s = new StringBuilder(12 * optionToValue.size());
        s.append("Options[");
        for (Map.Entry<Option,String> e : optionToValue.entrySet()) {
            s.append(e.getKey()).append("=").append(e.getValue()).append(",");
        }
        s.setCharAt(s.length()-1, ']');
        return s.toString();
    }
    
    private void printOption(Set<Option> options, String optionCategory,
                             int maxNameLength, int descriptionStart,
                             StringBuilder sb) {
        // write out the group name
        sb.append(optionCategory).append(":\n");
        for (Option o : options) {
            sb.append("  -").append(o.shortName);
            
            int spacesToAppend = -1;
            if (o.longName != null) {
                int length = o.longName.length();
                sb.append(", --").append(o.longName);
                if (o.valueName != null) {
                    sb.append("=").append(o.valueName);
                    length += o.valueName.length();
                }
                else {
                    // for the =
                    length -= 1;
                }
                spacesToAppend = (maxNameLength - length) + 4;
            }
            else {
                spacesToAppend = maxNameLength + 4;
            }
            
            // pad the white space for nice formatting
            for (int i = 0; i < spacesToAppend; ++i) {
                sb.append(" ");
            }
            
            if (o.description != null) {
                String[] descWords = o.description.split("\\s+");
                int spaceRemaining = 80 - descriptionStart;
                for (String word : descWords) {
                    if (spaceRemaining < word.length()) {
                        sb.append("\n");
                        for (int i = 0; i < descriptionStart;++i)
                            sb.append(" ");
                        spaceRemaining = 80 - descriptionStart;
                    }
                    sb.append(word).append(" ");
                    spaceRemaining -= (word.length() + 1);
                }
            }
            sb.append("\n");
        }
    }

    /**
     * A helper class for containing all the information on an option.
     */
    private static class Option implements Comparable<Option> {

        final char shortName;
        
        final String longName;

        final String description;

        final String valueName;
        
        public Option(char shortName, String longName, 
                      String description, String valueName) {
            this.shortName = shortName;
            this.longName = longName;
            this.description = description;
            this.valueName = valueName;
        }

        public int compareTo(Option o) {
            return shortName - o.shortName;
        }
        
        public boolean equals(Object o) {
            if (o instanceof Option) {
                Option p = (Option)o;
                return p.shortName == shortName || 
                    (p.longName != longName && (p.longName.equals(longName)));
            }
            return false;
        }
        
        public int hashCode() {
            return longName.hashCode();
        }
        
        public boolean hasValue() {
            return valueName != null;
        }

        public String toString() {
            return "-" + shortName + ", --" + longName;
        }
    }

}
