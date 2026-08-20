package org.molgenis.emx2.datamodels.profiles;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.molgenis.emx2.MolgenisException;

public class ResourceListing {

  private static final String SLASH = "/";

  /**
   * List directory contents for a resource folder, merged across every classpath root that carries
   * the folder (so test resources and main resources contribute to the same listing). Not
   * recursive. Works for regular files and JARs. Based on original by Greg Briggs, see:
   * https://www.uofr.net/~greg/java/get-resource-listing.html
   *
   * @return Just the name of each member item, not the full paths, sorted for deterministic order.
   */
  public String[] retrieve(String path) throws URISyntaxException, IOException {
    String resource = path.startsWith(SLASH) ? path.substring(1) : path;
    Enumeration<URL> dirUrls = getClass().getClassLoader().getResources(resource);
    if (!dirUrls.hasMoreElements()) {
      throw new MolgenisException(
          "Import failed: Directory " + path + " doesn't exist in classpath");
    }
    Set<String> result = new TreeSet<>();
    while (dirUrls.hasMoreElements()) {
      collectEntries(dirUrls.nextElement(), path, result);
    }
    return result.toArray(new String[0]);
  }

  private void collectEntries(URL dirURL, String path, Set<String> result)
      throws URISyntaxException, IOException {
    if (dirURL.getProtocol().equals("file")) {
      String[] names = new File(dirURL.toURI()).list();
      if (names != null) {
        result.addAll(Arrays.asList(names));
      }
      return;
    }
    if (dirURL.getProtocol().equals("jar")) {
      collectJarEntries(dirURL, path, result);
      return;
    }
    throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
  }

  private void collectJarEntries(URL dirURL, String path, Set<String> result) throws IOException {
    String jarPath =
        dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); // strip out only the JAR file
    String matchAgainstPath = path.startsWith(SLASH) ? path.substring(1) : path;

    // SonarCloud Recommended Secure Coding Practices preventing Zip Bomb attacks
    int thresholdEntries = 10000;
    int thresholdSize = 1000000000; // 1 GB
    double thresholdRatio = 10;
    int totalSizeArchive = 0;
    int totalEntryArchive = 0;

    try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
      Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
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
            // ratio between compressed and uncompressed data is highly suspicious, looks like a Zip
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

        addJarEntry(ze.getName(), matchAgainstPath, path, result);
      }
    }
  }

  private void addJarEntry(String name, String matchAgainstPath, String path, Set<String> result) {
    if (!name.startsWith(matchAgainstPath)) { // filter according to the path
      return;
    }
    String entry = name.substring(path.length());
    int checkSubdir = entry.indexOf(SLASH);
    if (checkSubdir >= 0) {
      // if it is a subdirectory, we just return the directory name
      entry = entry.substring(0, checkSubdir);
    }
    if (!entry.isEmpty()) {
      result.add(entry);
    }
  }
}
