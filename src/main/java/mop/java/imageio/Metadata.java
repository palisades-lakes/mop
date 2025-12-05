package mop.java.imageio;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** ImageIO metadata.
 *
 * @author palisades dot lakes at gmail dot com
 * @version "2025-12-04
 */

public final class Metadata {

  //-------------------------------------------------------------------
  // print for debugging
  //-------------------------------------------------------------------
  // see
  // https://docs.oracle.com/javase/8/docs/technotes/guides/imageio/spec/apps.fm3.html

  private static void indent (final int level) {
    for (int i = 0; i < level; i++) {
      System.out.print(" "); } }

  //-------------------------------------------------------------------
  // xml
  //-------------------------------------------------------------------

  private static final int PRINT_LENGTH = 8;

  private static void print (final Node node,
                             final int level) {
    indent(level); // emit open tag
    System.out.println(node.getNodeName());
    final NamedNodeMap map = node.getAttributes();
    if (map != null) { // print attribute values
      final int length = map.getLength();
      for (int i = 0; i < Math.min(length,PRINT_LENGTH); i++) {
        final Node attr = map.item(i);
        System.out.print(" " + attr.getNodeName() +
                           ": " + attr.getNodeValue()); }
      if (length > PRINT_LENGTH) { System.out.print("..."); } }

    final NodeList children = node.getChildNodes();
    final int length = children.getLength();
    for (int i = 0; i < Math.min(length,PRINT_LENGTH); i++) {
      print(children.item(i), level + 1); }
    if (length > PRINT_LENGTH) { System.out.print("..."); }

    System.out.println(); }

  @SuppressWarnings("unused")
  public static final void print (final Node root) {
    print(root, 0); }

  //-------------------------------------------------------------------
  // xml
  //-------------------------------------------------------------------

  private static void toXml (final Node node,
                             final int level) {
    indent(level); // emit open tag
    System.out.print("<" + node.getNodeName());
    final NamedNodeMap map = node.getAttributes();
    if (map != null) { // print attribute values
      final int length = map.getLength();
      for (int i = 0; i < length; i++) {
        final Node attr = map.item(i);
        System.out.print(" " + attr.getNodeName() +
                           "=\"" + attr.getNodeValue() + "\""); } }

    Node child = node.getFirstChild();
    if (child == null) {
      System.out.println("/>"); }
    else {
      System.out.println(">"); // close current tag
      while (child != null) { // emit child tags recursively
        toXml(child, level + 1);
        child = child.getNextSibling(); }
      indent(level); // emit close tag
      System.out.println("</" + node.getNodeName() + ">"); } }

  @SuppressWarnings("unused")
  public static final void toXml (final Node root) {
    toXml(root, 0); }

  //--------------------------------------------------------------------
  // disabled constructor
  //--------------------------------------------------------------------
  private Metadata () {
    throw new UnsupportedOperationException(
      "Can't instantiate " + getClass()); }
  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
