package mop.java;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

//---------------------------------------------------------------------
/** Generic object description strings, for debugging.
 * <br>
 * Java implementation to ease calling in Java.
 * @author palisades dot lakes at gmail dot com
 * @version "2025-12-04
 */

@SuppressWarnings("unused")
public final class Description {

  //===================================================================
  // constants
  //===================================================================

  public static final String LINE_SEPARATOR = System.lineSeparator();

  //-------------------------------------------------------------------
  /** standard format for vector, matrix, ..., coordinates
   */

  @SuppressWarnings("unused")
  public static final String
    COORDINATE_FORMAT = "%#-+8.3g";
  //COORDINATE_FORMAT = "%#-+11.6g";
  //COORDINATE_FORMAT = "%#-+13.8g";

  //===================================================================
  // methods
  //===================================================================

  public static final String toString (final Object x) {
    if (x instanceof byte[]) {
      return java.util.Arrays.toString((byte[]) x); }
    if (x instanceof boolean[]) {
      return java.util.Arrays.toString((boolean[]) x); }
    if (x instanceof char[]) {
      return java.util.Arrays.toString((char[]) x); }
    if (x instanceof int[]) {
      return java.util.Arrays.toString((int[]) x); }
    if (x instanceof long[]) {
      return java.util.Arrays.toString((long[]) x); }
    if (x instanceof float[]) {
      return java.util.Arrays.toString((float[]) x); }
    if (x instanceof double[]) {
      return java.util.Arrays.toString((double[]) x); }
    if (x instanceof Object[]) {
      return Arrays.toString((Object[]) x); }
    return Objects.toString(x); }

  //-------------------------------------------------------------------
  // description
  // TODO: pretty printing, eg indentation, adaptive line breaks, depth...
  //-------------------------------------------------------------------
  private static final Object getValue (final Field f, final Object x) {
    try { return f.get(x); }
    catch (final IllegalAccessException e) {
      throw new RuntimeException(e); } }
  //-------------------------------------------------------------------
  private static final String safeString (final Object x) {
    if (null==x) { return "null"; }
    return x.toString(); }
  //-------------------------------------------------------------------
  /** Return a string containing a reasonably complete description
   ** of the <code>object</code>'s state,
   ** for debugging and logging purposes.
   ** The description string should end with a line separator.
   **/

  @SuppressWarnings("unused")
  public static final String
  description (final Object x) {
    if (null == x) { return "nil" + LINE_SEPARATOR; }
    final Class c = x.getClass();
    final StringBuilder d = new StringBuilder();
    d.append(c.getSimpleName());
    d.append("[");
    d.append(LINE_SEPARATOR);
    for (final Field f :  c.getDeclaredFields()) {
      f.setAccessible(true);
      //final Class t = f.getType();
      final Object v = getValue(f,x);
      d.append(f.getName());
      d.append(": ");
      d.append(safeString(v));
      d.append(LINE_SEPARATOR); }
    d.append("]");
    d.append(LINE_SEPARATOR);

    return d.toString(); }

  //-------------------------------------------------------------------

  @SuppressWarnings("unused")
  public static final String
  description (final String a) {
    return safeString(a) + LINE_SEPARATOR; }

  //-------------------------------------------------------------------

  @SuppressWarnings("unused")
  public static final String
  description (final boolean a) {
    return "boolean[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final byte a) {
    return "byte[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final short a) {
    return "short[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final int a) {
    return "int[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final long a) {
    return "long[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final float a) {
    return "float[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final double a) {
    return "double[" + a + "]" + LINE_SEPARATOR; }

  //-------------------------------------------------------------------

  @SuppressWarnings("unused")
  public static final String
  description (final Boolean a) {
    return "Boolean[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final Byte a) {
    return "Byte[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final Short a) {
    return "Short[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final Integer a) {
    return "Integer[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final Long a) {
    return "Long[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final Float a) {
    return "Float[" + a + "]" + LINE_SEPARATOR; }

  @SuppressWarnings("unused")
  public static final String
  description (final Double a) {
    return "Double[" + a + "]" + LINE_SEPARATOR; }

//----------------------------------------------------------
  /** Return the array's description string. **/

  @SuppressWarnings("unused")
  public static final String
  description (final boolean[] a) {
    final StringBuilder d = new StringBuilder();
    d.append("boolean[");
    final int n = a.length;
    d.append(n);
    d.append("]:");
    d.append(LINE_SEPARATOR);
    for (int i=0;i<n;i++) {
      d.append(i);
      d.append(" ");
      d.append(a[i]);
      d.append(LINE_SEPARATOR); }
    return d.toString(); }

  //-------------------------------------------------------------------
  /** Return the array's description string. **/

  @SuppressWarnings("unused")
  public static final String
  description (final byte[] a) {
    final StringBuilder d = new StringBuilder();
    d.append("byte[");
    final int n = a.length;
    d.append(n);
    d.append("]:");
    d.append(LINE_SEPARATOR);
    for (int i=0;i<n;i++) {
      d.append(i);
      d.append(" ");
      d.append(a[i]);
      d.append(LINE_SEPARATOR); }
    return d.toString(); }

  //-------------------------------------------------------------------
  /** Return the array's description string. **/

  @SuppressWarnings("unused")
  public static final String
  description (final short[] a) {
    final StringBuilder d = new StringBuilder();
    d.append("short[");
    final int n = a.length;
    d.append(n);
    d.append("]:");
    d.append(LINE_SEPARATOR);
    for (int i=0;i<n;i++) {
      d.append(i);
      d.append(" ");
      d.append(a[i]);
      d.append(LINE_SEPARATOR); }
    return d.toString(); }

  //-------------------------------------------------------------------
  /** Return the array's description string. **/

  @SuppressWarnings("unused")
  public static final String
  description (final int[] a) {
    final StringBuilder d = new StringBuilder();
    d.append("int[");
    final int n = a.length;
    d.append(n);
    d.append("]:");
    d.append(LINE_SEPARATOR);
    for (int i=0;i<n;i++) {
      d.append(i);
      d.append(" ");
      d.append(a[i]);
      d.append(LINE_SEPARATOR); }
    return d.toString(); }

  //-------------------------------------------------------------------
  /** Return the array's description string. **/

  @SuppressWarnings("unused")
  public static final String
  description (final long[] a) {
    final StringBuilder d = new StringBuilder();
    d.append("long[");
    final int n = a.length;
    d.append(n);
    d.append("]:");
    d.append(LINE_SEPARATOR);
    for (int i=0;i<n;i++) {
      d.append(i);
      d.append(" ");
      d.append(a[i]);
      d.append(LINE_SEPARATOR); }
    return d.toString(); }

  //-------------------------------------------------------------------
  /** Return the array's description string. **/

  @SuppressWarnings("unused")
  public static final String
  description (final float[] a) {
    final StringBuilder d = new StringBuilder();
    d.append("float[");
    final int n = a.length;
    d.append(n);
    d.append("]:");
    d.append(LINE_SEPARATOR);
    for (int i=0;i<n;i++) {
      d.append(i);
      d.append(" ");
      d.append(a[i]);
      d.append(LINE_SEPARATOR); }
    return d.toString(); }

  //-------------------------------------------------------------------
  /** Return the array's description string. **/

  @SuppressWarnings("unused")
  public static final String
  description (final double[] a) {
    final StringBuilder d = new StringBuilder();
    d.append("double[");
    final int n = a.length;
    d.append(n);
    d.append("]:");
    d.append(LINE_SEPARATOR);
    for (int i=0;i<n;i++) {
      d.append(i);
      d.append(" ");
      d.append((float) a[i]);
      d.append(LINE_SEPARATOR); }
    return d.toString(); }

  //-------------------------------------------------------------------
  /** Return a string containing a reasonably complete
   ** description of the array's elements,
   ** for debugging and logging purposes.
   ** The description string should end with a line separator.
   **/

  @SuppressWarnings("unused")
  public static final String
  description (final Object[] objects) {
    final StringBuilder d = new StringBuilder();
    if (null == objects) {
      d.append("null");
      d.append(LINE_SEPARATOR); }
    else {
      final int n = objects.length;
      for (int i=0;i<n;i++) {
        final Object object = objects[i];
        d.append(i);
        d.append(": ");
        if (null == object) { d.append("null"); }
        else { d.append(description(object)); }
        d.append(LINE_SEPARATOR); } }
    return d.toString(); }

  //--------------------------------------------------------------------
  // disabled constructor
  //--------------------------------------------------------------------
  private Description () {
    throw new UnsupportedOperationException(
      "Can't instantiate " + getClass()); }
  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
