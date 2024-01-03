package org.molgenis.emx2.datamodels.profiles;

import static org.molgenis.emx2.datamodels.profiles.PostProcessProfiles.csvStringToList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.AbstractDataLoader;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class SchemaFromProfile {

  private String yamlFileLocation;
  private Profiles profiles;
  private Character separator;

  public SchemaFromProfile(String yamlFileLocation) {
    if (!yamlFileLocation.endsWith(".yaml")) {
      throw new MolgenisException("Input YAML file name must end in '.yaml'");
    }
    this.yamlFileLocation = yamlFileLocation;
    InputStreamReader yaml =
        new InputStreamReader(
            Objects.requireNonNull(
                AbstractDataLoader.class
                    .getClassLoader()
                    .getResourceAsStream(this.yamlFileLocation)));
    try {
      this.profiles = new ObjectMapper(new YAMLFactory()).readValue(yaml, Profiles.class);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
    this.separator = ',';
  }

  public Profiles getProfiles() {
    return profiles;
  }

  public SchemaMetadata create() throws MolgenisException {
    String sharedModelsDir = File.separator + "_models" + File.separator + "shared";
    String specificModelsDir = File.separator + "_models" + File.separator + "specific";
    List<Row> keepRows = new ArrayList<>();
    try {
      keepRows.addAll(getProfilesFromAllModels(sharedModelsDir));
      keepRows.addAll(getProfilesFromAllModels(specificModelsDir));
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
    SchemaMetadata generatedSchema = Emx2.fromRowList(keepRows);
    return generatedSchema;
  }

  /**
   * From a classpath dir, get all EMX2 model files and slice for profiles
   *
   * @param directory
   * @return
   */
  public List<Row> getProfilesFromAllModels(String directory)
      throws URISyntaxException, IOException {
    List<Row> keepRows = new ArrayList<>();
    String[] modelsList = getResourceListing(directory);
    for (String schemaLoc : modelsList) {

      Iterable<Row> rowIterable =
          CsvTableReader.read(
              new InputStreamReader(
                  Objects.requireNonNull(
                      getClass().getResourceAsStream(directory + File.separator + schemaLoc))));

      for (Row row : rowIterable) {
        List<String> profiles = csvStringToList(row.getString("profiles"));
        if (profiles.isEmpty()) {
          throw new MolgenisException("No profiles for " + row);
        }
        for (String profile : profiles) {
          if (this.profiles.profileTagsList.contains(profile)) {
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
      if (matchAgainstPath.startsWith(File.separator)) {
        matchAgainstPath = path.substring(1);
      }
      JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
      Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
      Set<String> result = new HashSet<String>(); // avoid duplicates in case it is a subdirectory
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
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
      return result.toArray(new String[result.size()]);
    }
    throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
  }

  public static void main(String args[]) throws IOException, URISyntaxException {
    SchemaFromProfile sfp = new SchemaFromProfile("fairdatahub/FAIRDataHub.yaml");
    SchemaMetadata generatedSchema = sfp.create();
    System.out.println("resulting schema: " + generatedSchema);
  }
}
