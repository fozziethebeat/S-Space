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

  @Test (expected=IllegalArgumentException.class)
  public void removeShortOptionTest() {
      ArgOptions options = new ArgOptions();
      options.addOption('v', null, null);
      options.removeOption('v');

      assertTrue(!options.prettyPrint().contains("-v"));

      String[] testArgs = {"-v", "location"};
      options.parseOptions(testArgs);
      assertEquals(2, options.numPositionalArgs());
  }

  @Test (expected=IllegalArgumentException.class)
  public void removeShortWithLongOptionTest() {
      ArgOptions options = new ArgOptions();
      options.addOption('v', "verbose", null);
      options.removeOption('v');

      assertTrue(!options.prettyPrint().contains("-v"));
      assertTrue(!options.prettyPrint().contains("--verbose"));

      String[] testArgs = {"-v", "location"};
      options.parseOptions(testArgs);
  }

  @Test (expected=IllegalArgumentException.class)
  public void removeLongOptionTest() {
      ArgOptions options = new ArgOptions();
      options.addOption('v', "verbose", null);
      options.removeOption("verbose");

      assertTrue(!options.prettyPrint().contains("-v"));
      assertTrue(!options.prettyPrint().contains("--verbose"));

      String[] testArgs = {"-v", "location"};
      options.parseOptions(testArgs);
  }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(ArgOptionsTest.class);
  }
}
