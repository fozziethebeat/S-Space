package edu.ucla.sspace.common;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.text.*; 
import edu.ucla.sspace.util.*;

import org.junit.*;

import static org.junit.Assert.*;

public class ArgOptionsTest {

  @Test public void shortWithValueAfterPositionalTest() {
    ArgOptions options = new ArgOptions();
    options.addOption('v', "verbose", "verbosity", true, "INT");
    String[] testArgs = {"location", "-v", "5"};
    options.parseOptions(testArgs);
    assertEquals(1, options.numPositionalArgs());
    assertEquals("location", options.getPositionalArg(0));
    assertTrue(options.hasOption("verbose"));
    assertTrue(options.hasOption('v'));
    assertEquals(5, options.getIntOption("verbose"));
  }

  @Test public void shortWithValuePositionalTest() {
    ArgOptions options = new ArgOptions();
    options.addOption('v', "verbose", "verbosity", true, "INT");
    String[] testArgs = {"-v", "5", "location"};
    options.parseOptions(testArgs);
    assertEquals(1, options.numPositionalArgs());
    assertEquals("location", options.getPositionalArg(0));
    assertTrue(options.hasOption("verbose"));
    assertTrue(options.hasOption('v'));
    assertEquals(5, options.getIntOption("verbose"));
  }

  @Test public void shortPositionalTest() {
    ArgOptions options = new ArgOptions();
    options.addOption('v', "verbose", "verbosity");
    String[] testArgs = {"-v", "location"};
    options.parseOptions(testArgs);
    assertEquals(1, options.numPositionalArgs());
    assertEquals("location", options.getPositionalArg(0));
    assertTrue(options.hasOption("verbose"));
    assertTrue(options.hasOption('v'));
  }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(ArgOptionsTest.class);
  }
}
