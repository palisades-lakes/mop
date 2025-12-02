package mop.java;

//---------------------------------------------------------------------
/** Numerical array utilities.
 *
 * @author palisades dot lakes at gmail dot com
 * @version "2025-12-01
 */
public final class Arrays {

  //-------------------------------------------------------------------
  /** Ignore NaN. If all NaN, return null;
   */
//  public final static float[] minMax (final float[] a) {
//    assert null != a;
//    final int n = a.length;
//    assert 0 < n;
//    float min = Float.POSITIVE_INFINITY;
//    float max = Float.NEGATIVE_INFINITY;
//    for (final float ai : a) {
//      if (min > ai) { min = ai; }
//      if (ai > max) { max = ai; } }
//    if (min > max) { return null; }
//    return new float[] { min, max}; }
//
//
// -------------------------------------------------------------------
//  public final static boolean equals (final float[] a,
//                                      final float[] b) {
//    return java.util.Arrays.equals(a,b); }
////-------------------------------------------------------------------
/** NaNs are treated as equal, as are 0.0 and -0.0.
 */

//public final static boolean areEqual (final float[] a,
//                                      final float[] b) {
//  if (a==b) { return true; }
//  if ((a==null) || (b==null)) { return false; }
//  final int n = a.length;
//  if (n != b.length) { return false;}
//  for (int i=0;i<n;i++) {
//    final float ai = a[i];
//    final float bi = b[i];
//    if (Float.isNaN(ai)) {
//      if (Float.isNaN(bi)) { continue; }
//      else {
//        System.err.println("Mismatch at " + i + ",\n" + ai + "\n" + bi);
//        return false; } }
//    else if (Float.isNaN(bi)) {
//      System.err.println("Mismatch at " + i + ",\n" + ai + "\n" + bi);
//      return false; }
//    if (ai != bi) {
//      System.err.println("Mismatch at " + i + ",\n" + ai + "\n" + bi);
//      return false; } }
//  return true; }
//
//-------------------------------------------------------------------
// disabled constructor
//-------------------------------------------------------------------
private Arrays () {
  throw new UnsupportedOperationException(
    "Can't instantiate " + getClass()); }
//-------------------------------------------------------------------
} // end class
//-------------------------------------------------------------------
