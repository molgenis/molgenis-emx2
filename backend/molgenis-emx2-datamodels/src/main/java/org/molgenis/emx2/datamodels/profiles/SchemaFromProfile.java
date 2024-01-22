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

  /** Construct from YAML file */
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

  /** Construct from Profiles object */
  public SchemaFromProfile(Profiles profiles) {
    this.profiles = profiles;
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
    return Emx2.fromRowList(keepRows);
  }

  /** From a classpath dir, get all EMX2 model files and slice for profiles */
  public List<Row> getProfilesFromAllModels(String directory)
      throws URISyntaxException, IOException {

    // first, load everything into maps
    Map<String, Row> tableDeclarationsByTableName = new HashMap<>();
    Map<String, List<Row>> columnDeclarationsByTableName = new HashMap<>();
    List<Row> keepRows = new ArrayList<>();
    String[] modelsList = getResourceListing(directory);

    for (String schemaLoc : modelsList) {
      Iterable<Row> rowIterable =
          CsvTableReader.read(
              new InputStreamReader(
                  Objects.requireNonNull(
                      getClass().getResourceAsStream(directory + File.separator + schemaLoc))));
      for (Row row : rowIterable) {

        String tableName = row.getString("tableName");
        if (rowIsTableDeclaration(row)) {
          if (tableDeclarationsByTableName.containsKey(tableName)) {
            throw new MolgenisException("Duplicate table declaration for name: " + tableName);
          }
          tableDeclarationsByTableName.put(tableName, row);
        } else {
          if (!columnDeclarationsByTableName.containsKey(tableName)) {
            columnDeclarationsByTableName.put(tableName, new ArrayList<>());
          }
          columnDeclarationsByTableName.get(tableName).add(row);
        }
      }
    }

    // post process: if table is tagged, but no columns, add tag to all columns
    for (String tableName : tableDeclarationsByTableName.keySet()) {
      if (columnDeclarationsByTableName.get(tableName) == null) {
        // it is allowed to have tables without any columns, skip
        continue;
      }

      Row row = tableDeclarationsByTableName.get(tableName);

      List<String> profilesForTable = csvStringToList(row.getString("profiles"));

      for (String profileForTable : profilesForTable) {
        // table is tagged with selected profile, now check columns
        boolean atLeastOneColumnIsTagged = false;

        for (Row column : columnDeclarationsByTableName.get(tableName)) {

          List<String> profilesForColumn = csvStringToList(column.getString("profiles"));
          if (profilesForColumn.contains(profileForTable)) {
            atLeastOneColumnIsTagged = true;
            break;
          }
        }
        // tag all when none are tagged by this particular profileTag
        if (!atLeastOneColumnIsTagged) {
          for (Row column : columnDeclarationsByTableName.get(tableName)) {
            List<String> profilesForColumn =
                column.getString("profiles") != null
                    ? csvStringToList(column.getString("profiles"))
                    : new ArrayList<>();
            profilesForColumn.add(profileForTable);
            column.setString("profiles", String.join(",", profilesForColumn));
          }
        }
      }
    }

    // post process 2: add non-tagged inherited tables of tagged tables to eliminate the need to tag
    // them and inadvertently adding all the columns of these parent tables or including 1 random
    // column to prevent the rest from being added
    Map<String, String> tableToParent = new HashMap<>();
    for (String tableName : tableDeclarationsByTableName.keySet()) {
      Row row = tableDeclarationsByTableName.get(tableName);
      if (row.getString("tableExtends") != null) {
        tableToParent.put(tableName, row.getString("tableExtends"));
      }
    }
    for (String tableName : tableDeclarationsByTableName.keySet()) {
      Row row = tableDeclarationsByTableName.get(tableName);
      List<String> profilesForTable = csvStringToList(row.getString("profiles"));
      for (String profileForTable : profilesForTable) {
        if (this.profiles.getProfileTagsList().contains(profileForTable)
            && tableToParent.containsKey(tableName)) {
          // table has a parent, add the current tag to it and continue to its parent and so on
          String nextTable = tableName;
          while (tableToParent.containsKey(nextTable)) {
            String parentTableName = tableToParent.get(nextTable);
            Row parentTable = tableDeclarationsByTableName.get(parentTableName);
            List<String> updateProfilesForParentTable =
                parentTable.getString("profiles") != null
                    ? csvStringToList(parentTable.getString("profiles"))
                    : new ArrayList<>();
            if (!updateProfilesForParentTable.contains(profileForTable)) {
              updateProfilesForParentTable.add(profileForTable);
              parentTable.setString("profiles", String.join(",", updateProfilesForParentTable));
            }
            nextTable = parentTableName;
          }
        }
      }
    }

    // finally, run profile select as usual, first per table
    for (String tableName : tableDeclarationsByTableName.keySet()) {
      Row row = tableDeclarationsByTableName.get(tableName);
      List<String> profilesForTable = csvStringToList(row.getString("profiles"));
      if (profilesForTable.isEmpty()) {
        throw new MolgenisException("No profiles for table " + row);
      }
      for (String profileForTable : profilesForTable) {
        if (this.profiles.getProfileTagsList().contains(profileForTable)) {
          keepRows.add(row);
          break;
        }
      }
    }
    // then per column
    for (String tableName : columnDeclarationsByTableName.keySet()) {
      List<Row> rows = columnDeclarationsByTableName.get(tableName);
      for (Row row : rows) {
        List<String> profilesForTable = csvStringToList(row.getString("profiles"));
        if (profilesForTable.isEmpty()) {
          throw new MolgenisException("No profiles for column " + row);
        }
        for (String profileForTable : profilesForTable) {
          if (this.profiles.getProfileTagsList().contains(profileForTable)) {
            keepRows.add(row);
            break;
          }
        }
      }
    }
    return keepRows;
  }

  /** Check if a row declared a new table, or if it declares a column */
  private boolean rowIsTableDeclaration(Row row) {
    if (row.getString("columnType") == null && row.getString("columnName") == null) {
      return true;
    } else {
      return false;
    }
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
