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

package edu.ucla.sspace.util;

import java.util.Calendar;
import java.util.Date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for representing a span of time.  Instance of this class can
 * be used to determine whether two moments fall within the time span.<p>
 *
 * Time spans are expressed as a combination of time units and their associated
 * amounts.  Each amount may add up to be more than one of a larger time unit.
 * For example a time unit may be expressed as 1 month and 6 weeks.  Time
 * amounts that are not used should be set to 0, e.g. a time span representing a
 * single day would have only {@code days} set to 1 and all other time units as
 * 0.<p>
 *
 * Note that when the overloaded {@code Date} and {@code long} methods are used,
 * the instances are converted to the JVM's current calendar system.  This may
 * have a slight, noticeable affect due to daylight savings or any other
 * calendar-specific differences to how a date is calculated.
 *
 * @see Calendar
 * @see Date
 */
public class TimeSpan {

    /**
     * The pattern for recognizing an amount of time as a number followed by a
     * single character denoting the unit
     */
    private static final Pattern TIME_SPAN_PATTERN = 
	Pattern.compile("\\d+[a-zA-Z]");

    /**
     * The number of years in this time span
     */
    private final int years;

    /**
     * The number of months in this time span
     */
    private final int months;

    /**
     * The number of weeks in this time span
     */
    private final int weeks;

    /**
     * The number of days in this time span
     */
    private final int days;

    /**
     * The number of hours in this time span
     */
    private final int hours;

    /**
     * Creates a time span using the date string to specify the time units and
     * amounts.  Time units are specified using a single lower case letter of
     * the word for the time unit, e.g. {@code y} for year, {@code m} for month.
     *
     * @param timespan a string containing numbers each followed by a single
     *        character to denote the time unit
     *
     * @throws IllegalArgumentException if <ul> <li> any of the parameters are
     *         negative <li> if any of the time units are specified more than
     *         once</ul>
     */
    public TimeSpan(String timespan) {
	
	Matcher matcher = TIME_SPAN_PATTERN.matcher(timespan);

	// Keep track of which time units have been set so far using a bit flag
	// pattern.  Each of the 5 units is stored as a single bit in order of
	// length, with longest first.
	int unitBitFlags = 0;

	// Assign default values of 0 to all of the time ranges before hand,
	// then overwite those with what data the timespan string contains
	int y = 0;
	int m = 0;
	int w = 0;
	int d = 0;
	int h = 0;

	int prevEnd = 0;
	while (matcher.find()) {
	    // check that the next time unit has a proper format by ensuring
	    // that all the patterns occur with no character gaps, e.g. 30d is
	    // valid but 30dd will cause an error from the extra 'd'.
	    if (matcher.start() != prevEnd) {
		throw new IllegalArgumentException(
		    "invalid time unit format: " + timespan);
	    }
	    prevEnd = matcher.end();
	    String lengthStr = 
		timespan.substring(matcher.start(), matcher.end() -1);
	    int length = Integer.parseInt(lengthStr);
	    checkDuration(length);
	    char timeUnit = timespan.charAt(matcher.end() - 1);

	    // Update the appropriate time based on the unit
	    switch (timeUnit) {
	    case 'y':
		checkSetTwice(unitBitFlags, 0, "years");
		unitBitFlags |= (1 << 0);
		y = length;
		break;
	    case 'm':
		checkSetTwice(unitBitFlags, 1, "months");
		unitBitFlags |= (1 << 1);
		m = length;
		break;
	    case 'w':
		checkSetTwice(unitBitFlags, 2, "weeks");
		unitBitFlags |= (1 << 2);
		w = length;
		break;
	    case 'd':
		checkSetTwice(unitBitFlags, 3, "days");
		unitBitFlags |= (1 << 3);
		d = length;
		break;
	    case 'h':
		checkSetTwice(unitBitFlags, 4, "hours");
		unitBitFlags |= (1 << 4);
		h = length;
		break;
	    default:
		throw new IllegalArgumentException(
		    "Unknown time unit: " + timeUnit);
	    }
	}

	// update the final variables;
	years = y;
	months = m;
	weeks = w;
	days = d;
	hours = h;
    }
    
    /**
     * Creates a time span for the specified duration.
     *
     * @param years the number of years for this time span
     * @param months the number of years for this time span
     * @param weeks the number of years for this time span
     * @param days the number of years for this time span
     * @param hours the number of years for this time span
     *
     * @throws IllegalArgumentException if any of the parameters are negative
     */
    public TimeSpan(int years, int months, int weeks, int days, int hours) {
	checkDuration(years);
	checkDuration(months);
	checkDuration(weeks);
	checkDuration(days);
	checkDuration(hours);
	
	this.years = years;
	this.months = months;
	this.weeks = weeks;
	this.days = days;
	this.hours = hours;
    }

    /**
     * Adds the duration of this time span to the provided {@code Calendar}
     * instance, moving it forward in time.
     *
     * @param c the calendar whose date will be moved forward by the duration of
     *        this time span
     */
    public void addTo(Calendar c) {
        c.add(Calendar.YEAR, years);
        c.add(Calendar.MONTH, months);
        c.add(Calendar.WEEK_OF_YEAR, weeks);
        c.add(Calendar.DAY_OF_YEAR, days);
        c.add(Calendar.HOUR_OF_DAY, hours);
    }

    /**
     * Adds the duration of this time span to the provided {@code Date}
     * instance, moving it forward in time.
     *
     * @param d the date whose value will be moved forward by the duration of
     *        this time span
     */
    public void addTo(Date d) {
	Calendar c = Calendar.getInstance();
	c.setTime(d);
        addTo(c);
        d.setTime(c.getTime().getTime());
    }

    /**
     * Checks whether the index is already set in the bit flags and throws an
     * exception if so.
     *
     * @param bigFlag an {code int} bit sequence, where each bit represents a a
     *        time span value whose value is {@code 1} if that value has been
     *        set
     * @param index the index of the field whose value is to be checked whether
     *        it has already been set
     * @param field the name of the index, which is used in the exception
     *        message
     *
     * @throws IllegalArgumentException if the value for the field has already
     *         been set
     */
    private static void checkSetTwice(int bitFlag, int index, String field) {
	// check that the field's index has not already been set
	if ((bitFlag & (1 << index)) != 0) {
	    throw new IllegalArgumentException(field +  " is set twice");
	}	
    }

    /**
     * Throws an exception if the duration is negative
     */
    private static void checkDuration(int duration) {
	if (duration < 0) 
	    throw new IllegalArgumentException(
		"Duration must be non-negative");
    }    

    /**
     * Returns the day component of this time span.  This value does not reflect
     * the total number of days that make up this time span, but rather how many
     * days were specified in addition to the other time components to comprise
     * the total duration.
     *
     * @return the day component of this time span
     */
    public int getDays() {
        return days;
    }
     
    /**
     * Returns the hour component of this time span.  This value does not
     * reflect the total number of hours that make up this time span, but rather
     * how many hours were specified in addition to the other time components to
     * comprise the total duration.
     *
     * @return the hour component of this time span
     */
    public int getHours() {
        return hours;
    }

    /**
     * Returns the month component of this time span.  This value does not
     * reflect the total number of months that make up this time span, but
     * rather how many months were specified in addition to the other time
     * components to comprise the total duration.
     *
     * @return the month component of this time span
     */
    public int getMonths() {
        return months;
    }

    /**
     * Returns the week component of this time span.  This value does not
     * reflect the total number of weeks that make up this time span, but rather
     * how many weeks were specified in addition to the other time components to
     * comprise the total duration.
     *
     * @return the week component of this time span
     */
    public int getWeeks() {
        return weeks;
    }

    /**
     * Returns the year component of this time span.  This value does not
     * reflect the total number of years that make up this time span, but rather
     * how many years were specified in addition to the other time components to
     * comprise the total duration.
     *
     * @return the year component of this time span
     */
    public int getYears() {
        return years;
    }

    /**
     * Returns {@code true} if the end date occurs after the start date during
     * the period of time represented by this time span.
     */
    public boolean insideRange(Calendar startDate,
			       Calendar endDate) {
	// make a copy of the start time so that it is safe to modify it without
	// affecting the input parameter
	Calendar mutableStartDate = (Calendar)(startDate.clone());
	return isInRange(mutableStartDate, endDate);
    }
    
    /**
     * Returns {@code true} if the end date occurs after the start date during
     * the period of time represented by this time span.
     * 
     * @param mutableStartDate a <i>mutable<i> {@code Calendar} object that will
     *        be changed to the ending time of this time range as a side effect
     *        of this method
     */
    private boolean isInRange(Calendar mutableStartDate,
			      Calendar endDate) {
	
	// ensure that the ending date does not occur before the time span would
	// have started
	if (endDate.before(mutableStartDate))
	    return false;

	// update the start date to be the date at the end of the time span
	Calendar tsEnd = mutableStartDate;
	tsEnd.add(Calendar.YEAR, years);
	tsEnd.add(Calendar.MONTH, months);
	tsEnd.add(Calendar.WEEK_OF_YEAR, weeks);
	tsEnd.add(Calendar.DAY_OF_YEAR, days);
	tsEnd.add(Calendar.HOUR, hours);
		
	return endDate.before(tsEnd);
    }

    /**
     * Returns {@code true} if the end date occurs after the start date during
     * the period of time represented by this time span.
     */
    public boolean insideRange(Date startDate, Date endDate) {
	Calendar c1 = Calendar.getInstance();
	Calendar c2 = Calendar.getInstance();
	c1.setTime(startDate);
	c2.setTime(endDate);
	return isInRange(c1, c2);
    }

    /**
     * Returns {@code true} if the end date occurs after the start date during
     * the period of time represented by this time span.
     */
    public boolean insideRange(long startDate, long endDate) {
	Calendar c1 = Calendar.getInstance();
	Calendar c2 = Calendar.getInstance();
	c1.setTimeInMillis(startDate);
	c2.setTimeInMillis(endDate);
	return isInRange(c1, c2);
    }

    public String toString() {
	return String.format("TimeSpan: %dy%dm%dw%dd%dh", years, months, 
			     weeks, days, hours);
    }

}
