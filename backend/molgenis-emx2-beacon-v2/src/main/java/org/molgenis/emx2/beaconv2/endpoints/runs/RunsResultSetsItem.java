package org.molgenis.emx2.beaconv2.endpoints.runs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.semantics.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RunsResultSetsItem {

  private String id;
  private String biosampleId;
  private String individualId;
  private String runDate;
  private OntologyTerm librarySource;
  private String librarySelection;
  private OntologyTerm libraryStrategy;
  private String libraryLayout;
  private OntologyTerm platform;
  private OntologyTerm platformModel;

  public void setId(String id) {
    this.id = id;
  }

  public void setBiosampleId(String biosampleId) {
    this.biosampleId = biosampleId;
  }

  public void setIndividualId(String individualId) {
    this.individualId = individualId;
  }

  public void setRunDate(String runDate) {
    this.runDate = runDate;
  }

  public void setLibrarySource(OntologyTerm librarySource) {
    this.librarySource = librarySource;
  }

  public void setLibrarySelection(String librarySelection) {
    this.librarySelection = librarySelection;
  }

  public void setLibraryStrategy(OntologyTerm libraryStrategy) {
    this.libraryStrategy = libraryStrategy;
  }

  public void setLibraryLayout(String libraryLayout) {
    this.libraryLayout = libraryLayout;
  }

  public void setPlatform(OntologyTerm platform) {
    this.platform = platform;
  }

  public void setPlatformModel(OntologyTerm platformModel) {
    this.platformModel = platformModel;
  }
}
