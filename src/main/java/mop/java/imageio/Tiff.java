package mop.java.imageio;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;

/** ImageIO tiff read/write.
 *
 * @author palisades dot lakes at gmail dot com
 * @version "2025-12-04
 */

@SuppressWarnings("unused")
public final class Tiff {

  // see
  // https://docs.oracle.com/en/java/javase/22/docs/api/java.desktop/javax/imageio/metadata/doc-files/tiff_metadata.html

  @SuppressWarnings("unused")
  public static final void writeCompressedTiff (final ImageReader reader,
                                                final File outputFile)
  throws IOException {

    final ImageWriter writer = ImageIO.getImageWriter(reader);
    final ImageWriteParam writeParam = writer.getDefaultWriteParam();
    // TODO: handle multi-image, multi-thumbnail cases
    final IIOImage image = reader.readAll(0,null);
    final IIOMetadata metadata = image.getMetadata();
    final ImageOutputStream output =
      ImageIO.createImageOutputStream(outputFile);
    writer.setOutput(output);

    // Set compression.
    writeParam.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
    writeParam.setCompressionType("LZW");
    writer.write(metadata,image,writeParam); }

//    // Write the APP1 Exif TIFF stream.
//    if (thumbnail != null) {
//      // Write the TIFF header.
//      writer.prepareWriteSequence(tiffStreamMetadata);
//
//      // Append the primary IFD.
//      writer.prepareInsertEmpty(-1, // append
//                                    new ImageTypeSpecifier(image),
//                                    image.getWidth(),
//                                    image.getHeight(),
//                                    primaryIFD,
//                                    null, // thumbnails
//                                    writeParam);
//      writer.endInsertEmpty();
//
//      // Append the thumbnail IFD and image data.
//      writer.writeToSequence(
//        new IIOImage(thumbnail, null,null), writeParam);
//
//      // End writing.
//      writer.endWriteSequence();
//    } else {
//      // Write only the primary IFD.
//      writer.prepareWriteEmpty(tiffStreamMetadata,
//                                   new ImageTypeSpecifier(image),
//                                   image.getWidth(),
//                                   image.getHeight(),
//                                   primaryIFD,
//                                   null, // thumbnails
//                                   writeParam);
//      writer.endWriteEmpty();
//    }
//
//    // Flush data into byte stream.
//    app1ExifOutput.flush();
//
//    // Create APP1 parameter array.
//    byte[] app1Parameters = new byte[6 + baos.size()];
//
//    // Add APP1 Exif ID bytes.
//    app1Parameters[0] = (byte) 'E';
//    app1Parameters[1] = (byte) 'x';
//    app1Parameters[2] = (byte) 'i';
//    app1Parameters[3] = (byte) 'f';
//    app1Parameters[4] = app1Parameters[5] = (byte) 0;
//
//    // Append TIFF stream to APP1 parameters.
//    System.arraycopy(baos.toByteArray(), 0, app1Parameters, 6, baos.size());
//
//    // Create the APP1 Exif node to be added to native JPEG image metadata.
//    IIOMetadataNode app1Node = new IIOMetadataNode("unknown");
//    app1Node.setAttribute("MarkerTag", String.valueOf(0xE1));
//    app1Node.setUserObject(app1Parameters);
//
//    // Append the APP1 Exif marker to the "markerSequence" node.
//    IIOMetadata jpegImageMetadata =
//      jpegWriter.getDefaultImageMetadata(new ImageTypeSpecifier(image),
//                                         jpegWriteParam);
//    String nativeFormat = jpegImageMetadata.getNativeMetadataFormatName();
//    Node tree = jpegImageMetadata.getAsTree(nativeFormat);
//    NodeList children = tree.getChildNodes();
//    int numChildren = children.getLength();
//    for (int i = 0; i < numChildren; i++) {
//      Node child = children.item(i);
//      if (child.getNodeName().equals("markerSequence")) {
//        child.appendChild(app1Node);
//        break;
//      }
//    }
//    jpegImageMetadata.setFromTree(nativeFormat, tree);
//
//    // Write the JPEG image data including the APP1 Exif marker.
//    jpegWriter.setOutput(output);
//    jpegWriter.write(new IIOImage(image, null, jpegImageMetadata));
//  }
//--------------------------------------------------------------------
// disabled constructor
//--------------------------------------------------------------------
private Tiff () {
  throw new UnsupportedOperationException(
    "Can't instantiate " + getClass()); }
  //--------------------------------------------------------------------
} // end class
//--------------------------------------------------------------------
