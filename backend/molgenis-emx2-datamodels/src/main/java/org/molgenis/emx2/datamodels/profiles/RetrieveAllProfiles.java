package org.molgenis.emx2.datamodels.profiles;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

public class RetrieveAllProfiles {

  private List<SchemaFromProfile> allSchemaFromProfiles;
  private List<Profiles> allProfiles;
  private List<SchemaMetadata> allSchemas;

  /** Lazy constructor */
  public RetrieveAllProfiles() {
    super();
  }

  /**
   * Getter for all profiles with on-demand retrieve
   *
   * @return
   */
  public List<Profiles> getAllProfiles() {
    if (this.allProfiles == null) {
      retrieveAllProfiles();
    }
    return allProfiles;
  }

  /**
   * Getter for all schemas with on-demand retrieve
   *
   * @return
   */
  public List<SchemaMetadata> getAllSchemas() {
    if (this.allSchemas == null) {
      retrieveAllSchemas();
    }
    return allSchemas;
  }

  /**
   * Soft merge means that regular schema consistency checks are bypassed when putting all schemas
   * from profiles together. The schema as a whole may not be valid, in fact, it should not be
   * considered a valid importable EMX2 schema. This merge is useful to create documentation
   * instead.
   */
  public SchemaMetadata getSoftMergedFullSchema() throws Exception {
    if (this.allSchemas == null) {
      retrieveAllSchemas();
    }
    if (allSchemas.size() == 0) {
      throw new Exception("No schemas available for merging");
    }
    SchemaMetadata mergedSchema = null;
    for (SchemaMetadata schemaMetadata : allSchemas) {
      if (mergedSchema == null) {
        mergedSchema = schemaMetadata;
      } else {
        for (TableMetadata tableMetadata : schemaMetadata.getTables()) {
          String tableName = tableMetadata.getTableName();
          // add table to schema simply by name, may miss tables with the same name
          if (!mergedSchema.getTableNames().contains(tableName)) {
            mergedSchema.addTable(tableName, tableMetadata);
          }
          // add columns to table simply by name, may miss columns with the same name
          for (Column column : tableMetadata.getColumns()) {
            if (!mergedSchema
                .getTableMetadata(tableName)
                .getColumnNames()
                .contains(column.getName())) {
              mergedSchema.getTableMetadata(tableName).add(column);
            }
          }
        }
      }
    }
    return mergedSchema;
  }

  /** Internal function to retrieve all profiles */
  private void retrieveAllProfiles() {
    this.allSchemaFromProfiles = new ArrayList<>();
    this.allProfiles = new ArrayList<>();
    try {
      String[] profileLocs = new ResourceListing().retrieve("/_profiles");
      for (String pl : profileLocs) {
        String loc = "_profiles/" + pl;
        SchemaFromProfile sfp = new SchemaFromProfile(loc);
        this.allSchemaFromProfiles.add(sfp);
        this.allProfiles.add(sfp.getProfiles());
      }
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
  }

  /** Internal function to create and retrieve all schemas from all profiles */
  private void retrieveAllSchemas() {
    if (this.allProfiles == null) {
      retrieveAllProfiles();
    }
    this.allSchemas = new ArrayList<>();
    for (SchemaFromProfile sfp : this.allSchemaFromProfiles) {
      SchemaMetadata sm = sfp.create();
      this.allSchemas.add(sm);
    }
  }
}
