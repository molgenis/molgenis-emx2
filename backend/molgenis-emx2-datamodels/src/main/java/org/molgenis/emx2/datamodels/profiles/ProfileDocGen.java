package org.molgenis.emx2.datamodels.profiles;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
          "The complete EMX2 data model contains %s tables and XX columns. There are %s application profiles drawing from this model by using XX profile tags."
                  .formatted(fullSchema.getTableNames().size(), ap.getAllProfiles().size())
              + LE);
      bw.write(LE);
      bw.write("## Application profiles" + LE);
      bw.write("| Name | Description | Profile tags | xx |" + LE);
      bw.write("|---|---|---|---|" + LE);
      for (Profiles profiles : ap.getAllProfiles()) {
        bw.write(
            "| %s | %s | %s |---|"
                    .formatted(
                        profiles.getName(),
                        profiles.getDescription(),
                        (String.join(", ", profiles.getProfileTagsList())))
                + LE);
      }
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
                        (String.join(", ", table.getProfiles())),
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

  public static void main(String[] args) throws Exception {
    new ProfileDocGen("emx2-profile-doc.md").makeDocs();
  }
}
