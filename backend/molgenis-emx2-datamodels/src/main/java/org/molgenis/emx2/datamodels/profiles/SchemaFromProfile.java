package org.molgenis.emx2.datamodels.profiles;

import static org.molgenis.emx2.datamodels.profiles.PostProcessProfiles.csvStringToList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.AbstractDataLoader;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class SchemaFromProfile {

  private final Profiles profiles;

  public SchemaFromProfile(String yamlFileLocation) {
    if (!yamlFileLocation.endsWith(".yaml")) {
      throw new MolgenisException("Input YAML file name must end in '.yaml'");
    }
    InputStreamReader yaml =
        new InputStreamReader(
            Objects.requireNonNull(
                AbstractDataLoader.class.getClassLoader().getResourceAsStream(yamlFileLocation)));
    try {
      this.profiles = new ObjectMapper(new YAMLFactory()).readValue(yaml, Profiles.class);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
  }

  public Profiles getProfiles() {
    return profiles;
  }

  public SchemaMetadata create() throws MolgenisException {
    String sharedModelsDir = "/_models/shared";
    String specificModelsDir = "/_models/specific";
    List<Row> keepRows = new ArrayList<>();
    try {
      keepRows.addAll(getProfilesFromAllModels(sharedModelsDir));
      keepRows.addAll(getProfilesFromAllModels(specificModelsDir));
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
    return Emx2.fromRowList(keepRows);
  }

  /** From a classpath dir, get all EMX2 model files and slice for profiles */
  public List<Row> getProfilesFromAllModels(String directory)
      throws URISyntaxException, IOException {
    List<Row> keepRows = new ArrayList<>();
    String[] modelsList = getResourceListing(directory);
    for (String schemaLoc : modelsList) {

      Iterable<Row> rowIterable =
          CsvTableReader.read(
              new InputStreamReader(
                  Objects.requireNonNull(
                      getClass().getResourceAsStream(directory + "/" + schemaLoc))));

      for (Row row : rowIterable) {
        List<String> profiles = csvStringToList(row.getString("profiles"));
        if (profiles.isEmpty()) {
          throw new MolgenisException("No profiles for " + row);
        }
        for (String profile : profiles) {
          if (this.profiles.getProfileTagsList().contains(profile)) {
            keepRows.add(row);
            break;
          }
        }
      }
    }
    return keepRows;
  }

  /**
   * List directory contents for a resource folder. Not recursive. Works for regular files and JARs.
   * Based on original by Greg Briggs, see:
   * https://www.uofr.net/~greg/java/get-resource-listing.html
   *
   * @return Just the name of each member item, not the full paths.
   */
  String[] getResourceListing(String path) throws URISyntaxException, IOException {
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
      if (matchAgainstPath.startsWith("/")) {
        matchAgainstPath = path.substring(1);
      }

      // SonarCloud Recommended Secure Coding Practices preventing Zip Bomb attacks
      int THRESHOLDENTRIES = 10000;
      int THRESHOLDSIZE = 1000000000; // 1 GB
      double THRESHOLDRATIO = 10;
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
            if (compressionRatio > THRESHOLDRATIO) {
              // ratio between compressed and uncompressed data is highly suspicious, looks like a
              // Zip
              // Bomb Attack
              break;
            }
          }

          if (totalSizeArchive > THRESHOLDSIZE) {
            // the uncompressed data size is too much for the application resource capacity
            break;
          }

          if (totalEntryArchive > THRESHOLDENTRIES) {
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
