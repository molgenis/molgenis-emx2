package org.molgenis.emx2.datamodels.profiles;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.molgenis.emx2.*;

public record ProfileDocGen(String outputFile) {

  private static final String LE = System.lineSeparator();

  public void makeDocs() throws Exception {
    // Map<String,List<Row>> fullSchema = new SchemaFromProfile().createRowsPerTable(false);
    RetrieveAllProfiles ap = new RetrieveAllProfiles();
    SchemaMetadata fullSchema = ap.getSoftMergedFullSchema();

    FileWriter fw = new FileWriter(this.outputFile);
    try (BufferedWriter bw = new BufferedWriter(fw)) {

      bw.write("# EMX2 profile documentation" + LE);
      bw.write(LE);
      bw.write(
          "The complete EMX2 data model contains %s tables and %s columns. There are %s application profiles drawing from this model by using %s profile tags."
                  .formatted(
                      fullSchema.getTableNames().size(),
                      countAllColumns(fullSchema),
                      ap.getAllProfiles().size(),
                      countAllTags(fullSchema))
              + LE);
      bw.write(LE);

      bw.write("## Application profiles" + LE);
      bw.write("| Name | Description | Profile tags |" + LE);
      bw.write("|---|---|---|" + LE);
      for (Profiles profiles : ap.getAllProfiles()) {
        bw.write(
            "| %s | %s | %s |"
                    .formatted(
                        profiles.getName(),
                        profiles.getDescription(),
                        (String.join(", ", profiles.getProfileTagsList())))
                + LE);
      }
      bw.write(LE);

      bw.write("## Tags" + LE);
      bw.write("| Name |" + LE);
      bw.write("|---|" + LE);
      bw.write("| %s |".formatted(getAllTags(fullSchema)) + LE);
      bw.write(LE);

      bw.write("## Tables" + LE);
      bw.write("| Name | Description | Semantics | Profile tags | Nr. of columns |" + LE);
      bw.write("|---|---|---|---|---|" + LE);
      for (TableMetadata table : fullSchema.getTables()) {
        bw.write(
            "| %s | %s | %s | %s | %s |"
                    .formatted(
                        table.getTableName(),
                        table.getDescription(),
                        table.getSemantics() != null
                            ? (String.join(", ", table.getSemantics()))
                            : "n/a",
                        table.getSubsets() != null
                            ? (String.join(", ", table.getSubsets()))
                            : "NO SUBSETS FOR TABLE",
                        table.getColumns().size())
                + LE);
      }
      bw.write(LE);

      bw.write("## Columns per table" + LE);
      for (TableMetadata table : fullSchema.getTables()) {
        bw.write("### Table: " + table.getTableName() + LE);
        bw.write("| Column | Description | Semantics | Values |" + LE);
        bw.write("|---|---|---|---|" + LE);
        for (Column column : table.getColumns()) {
          bw.write(
              "| %s | %s | %s | %s |"
                      .formatted(
                          column.getName(),
                          column.getDescriptions(),
                          (column.getSemantics() != null
                              ? String.join(", ", column.getSemantics())
                              : "n/a"),
                          column.getColumnType())
                  + LE);
        }
        bw.write(LE);
      }

      bw.write(LE);
      bw.flush();
    }
  }

  public int countAllColumns(SchemaMetadata fullSchema) {
    int cols = 0;
    for (TableMetadata t : fullSchema.getTables()) {
      cols += t.getColumns().size();
    }
    return cols;
  }

  public int countAllTags(SchemaMetadata fullSchema) {
    int tags = 0;
    for (TableMetadata t : fullSchema.getTables()) {
      if (t.getSubsets() != null) tags += t.getSubsets().length;
      for (Column c : t.getColumns()) {
        if (c.getSubsets() != null) tags += c.getSubsets().length;
      }
    }
    return tags;
  }

  public Set<String> getAllTags(SchemaMetadata fullSchema) {
    Set<String> tags = new HashSet<>();
    for (TableMetadata t : fullSchema.getTables()) {
      if (t.getSubsets() != null) tags.addAll(Arrays.asList(t.getSubsets()));
      for (Column c : t.getColumns()) {
        if (c.getSubsets() != null) tags.addAll(Arrays.asList(c.getSubsets()));
      }
    }
    return tags;
  }

  public static void main(String[] args) throws Exception {
    new ProfileDocGen("emx2-profile-doc.md").makeDocs();
  }
}
