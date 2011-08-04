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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the {@link TimeSpan} class.
 */
@SuppressWarnings("deprecation")
public class TimeSpanTests {

    @Test public void testAddCalendar() {
        TimeSpan ts = new TimeSpan("1y");
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        ts.addTo(c);
        assertEquals(year + 1, c.get(Calendar.YEAR));

        ts = new TimeSpan("1m");
        for (int i = 0; i < 12; ++i) {
            int month = c.get(Calendar.MONTH);
            ts.addTo(c);
            assertEquals((month + 1) % 12, c.get(Calendar.MONTH));
        }
    }

    @SuppressWarnings("deprecated")
    @Test public void testAddDate() {
        TimeSpan ts = new TimeSpan("1y");
        Date d = new Date();
        int year = d.getYear();
        ts.addTo(d);
        assertEquals(year + 1, d.getYear());

        ts = new TimeSpan("1m");
        for (int i = 0; i < 12; ++i) {
            int month = d.getMonth();
            ts.addTo(d);
            assertEquals((month + 1) % 12, d.getMonth());
        }
    }
   
    @Test public void testStringConstructor() {
	TimeSpan ts = new TimeSpan("1y");
	ts = new TimeSpan("1m");
	ts = new TimeSpan("1w");
	ts = new TimeSpan("1d");
	ts = new TimeSpan("1h");
    }

    @Test public void testStringConstructorCombined() {
	TimeSpan ts = new TimeSpan("1y1m1w1d1h");
	assertEquals("TimeSpan: 1y1m1w1d1h", ts.toString());
    }

    @Test public void testStringConstructorCombinedRandomOrder() {
	TimeSpan ts = new TimeSpan("1h1d1m1y1w");
	assertEquals("TimeSpan: 1y1m1w1d1h", ts.toString());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testStringConstructorRepeat() {
	TimeSpan ts = new TimeSpan("1y1y");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testStringConstructorUnknownType() {
	TimeSpan ts = new TimeSpan("1z");
    }    

    @Test public void testYear() {
	TimeSpan ts = new TimeSpan("1y");
	Calendar now = Calendar.getInstance();
	Calendar lessThanYearFromNow = Calendar.getInstance();
	//lessThanYearFromNow.add(Calendar.YEAR, 1);
	lessThanYearFromNow.add(Calendar.MONTH, 1);
	assertTrue(ts.insideRange(now, lessThanYearFromNow));
    }

    @Test public void testYearOutside() {
	TimeSpan ts = new TimeSpan("1y");
	Calendar now = Calendar.getInstance();
	Calendar yearFromNow = Calendar.getInstance();
	yearFromNow.add(Calendar.YEAR, 1);
	assertFalse(ts.insideRange(now, yearFromNow));
    }

    @Test public void testMonth() {
	TimeSpan ts = new TimeSpan("1m");
	Calendar now = Calendar.getInstance();
	Calendar lessThanMonthFromNow = Calendar.getInstance();
	lessThanMonthFromNow.add(Calendar.WEEK_OF_YEAR, 1);
	assertTrue(ts.insideRange(now, lessThanMonthFromNow));
    }

    @Test public void testMonthOutside() {
	TimeSpan ts = new TimeSpan("1m");
	Calendar now = Calendar.getInstance();
	Calendar monthFromNow = Calendar.getInstance();
	monthFromNow.add(Calendar.MONTH, 1);
	assertFalse(ts.insideRange(now, monthFromNow));
    }

    @Test public void testWeek() {
	TimeSpan ts = new TimeSpan("1w");
	Calendar now = Calendar.getInstance();
	Calendar lessThanWeekFromNow = Calendar.getInstance();
	lessThanWeekFromNow.add(Calendar.DAY_OF_YEAR, 1);
	assertTrue(ts.insideRange(now, lessThanWeekFromNow));
    }

    @Test public void testWeekOutside() {
	TimeSpan ts = new TimeSpan("1w");
	Calendar now = Calendar.getInstance();
	Calendar weekFromNow = Calendar.getInstance();
	weekFromNow.add(Calendar.WEEK_OF_YEAR, 1);
	assertFalse(ts.insideRange(now, weekFromNow));
    }

    @Test public void testDay() {
	TimeSpan ts = new TimeSpan("1d");
	Calendar now = Calendar.getInstance();
	Calendar lessThanDayFromNow = Calendar.getInstance();
	lessThanDayFromNow.add(Calendar.HOUR, 1);
	assertTrue(ts.insideRange(now, lessThanDayFromNow));
    }

    @Test public void testDayOutside() {
	TimeSpan ts = new TimeSpan("1d");
	Calendar now = Calendar.getInstance();
	Calendar dayFromNow = Calendar.getInstance();
	dayFromNow.add(Calendar.DAY_OF_YEAR, 1);
	assertFalse(ts.insideRange(now, dayFromNow));
    }

    @Test public void testHour() {
	TimeSpan ts = new TimeSpan("1h");
	Calendar now = Calendar.getInstance();
	Calendar lessThanHourFromNow = Calendar.getInstance();
	lessThanHourFromNow.add(Calendar.MINUTE, 1);
	assertTrue(ts.insideRange(now, lessThanHourFromNow));
    }

    @Test public void testHourOutside() {
	TimeSpan ts = new TimeSpan("1h");
	Calendar now = Calendar.getInstance();
	Calendar hourFromNow = Calendar.getInstance();
	hourFromNow.add(Calendar.HOUR, 1);
	assertFalse(ts.insideRange(now, hourFromNow));
    }

    @Test public void testEndBeforeStart() {
	TimeSpan ts = new TimeSpan("1h");
	Calendar now = Calendar.getInstance();
	Calendar before = Calendar.getInstance();
	before.add(Calendar.HOUR, -1);
	assertFalse(ts.insideRange(now, before));
    }
}