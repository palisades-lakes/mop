package mop.java.jfx;

// mvn clean install
// mvn -q -DskipTests -Dclojure-maven-plugin.clojure.test.skip=true -Dmaven.test.skip=true install & jfx mop.java.jfx.Main

//---------------------------------------------------------------------

/**
 * Set system properties before JFX startup.
 *
 * @author palisades dot lakes at gmail dot com
 * @version 2026-03-22
 */

@SuppressWarnings({"unchecked","unused"})
public final class Main  {

  //-------------------------------------------------------------------
  // main
  //-------------------------------------------------------------------

  public final static void main (final String[] args) {
    System.out.println(System.getProperty("glass.win.uiScale"));
    System.setProperty("glass.win.uiScale", "1");
    System.out.println(System.getProperty("glass.win.uiScale"));
    IcosahedronS2.run(args); }

}
//---------------------------------------------------------------------
