package org.molgenis.emx2.datamodels.profiles;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;

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
