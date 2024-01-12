package org.molgenis.emx2.datamodels.profiles;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.molgenis.emx2.MolgenisException;

public class ResourceListing {

  /**
   * List directory contents for a resource folder. Not recursive. Works for regular files and JARs.
   * Based on original by Greg Briggs, see:
   * https://www.uofr.net/~greg/java/get-resource-listing.html
   *
   * @return Just the name of each member item, not the full paths.
   */
  public String[] retrieve(String path) throws URISyntaxException, IOException {
    URL dirURL = getClass().getResource(path);
    if (dirURL == null) {
      throw new MolgenisException(
          "Import failed: Directory " + path + " doesn't exist in classpath");
    }
    if (dirURL.getProtocol().equals("file")) {
      return new File(dirURL.toURI()).list();
    }
    if (dirURL.getProtocol().equals("jar")) {
      String jarPath =
          dirURL
              .getPath()
              .substring(5, dirURL.getPath().indexOf("!")); // strip out only the JAR file
      String matchAgainstPath = path;
      if (matchAgainstPath.startsWith(File.separator)) {
        matchAgainstPath = path.substring(1);
      }

      // SonarCloud Recommended Secure Coding Practices preventing Zip Bomb attacks
      int thresholdEntries = 10000;
      int thresholdSize = 1000000000; // 1 GB
      double thresholdRatio = 10;
      int totalSizeArchive = 0;
      int totalEntryArchive = 0;

      try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
        Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
        Set<String> result = new HashSet<>(); // avoid duplicates in case it is a subdirectory
        while (entries.hasMoreElements()) {

          ZipEntry ze = entries.nextElement();
          InputStream in = new BufferedInputStream(jar.getInputStream(ze));
          OutputStream out = new BufferedOutputStream(OutputStream.nullOutputStream());

          totalEntryArchive++;

          int nBytes;
          byte[] buffer = new byte[2048];
          int totalSizeEntry = 0;

          while ((nBytes = in.read(buffer)) > 0) { // Compliant
            out.write(buffer, 0, nBytes);
            totalSizeEntry += nBytes;
            totalSizeArchive += nBytes;

            double compressionRatio = totalSizeEntry / (double) ze.getCompressedSize();
            if (compressionRatio > thresholdRatio) {
              // ratio between compressed and uncompressed data is highly suspicious, looks like a
              // Zip
              // Bomb Attack
              break;
            }
          }

          if (totalSizeArchive > thresholdSize) {
            // the uncompressed data size is too much for the application resource capacity
            break;
          }

          if (totalEntryArchive > thresholdEntries) {
            // too much entries in this archive, can lead to inodes exhaustion of the system
            break;
          }

          String name = ze.getName();
          if (name.startsWith(matchAgainstPath)) { // filter according to the path
            String entry = name.substring(path.length());
            int checkSubdir = entry.indexOf("/");
            if (checkSubdir >= 0) {
              // if it is a subdirectory, we just return the directory name
              entry = entry.substring(0, checkSubdir);
            }
            result.add(entry);
          }
        }
        return result.toArray(new String[0]);
      }
    }
    throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
  }
}
