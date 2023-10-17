package org.molgenis.emx2.beaconv2.endpoints.biosamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.rdf.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BiosamplesResultSetsItem {

  private String id;
  private OntologyTerm biosampleStatus;
  private OntologyTerm sampleOriginType;
  private String collectionMoment;
  private String collectionDate;
  private ObtentionProcedure obtentionProcedure;

  public void setId(String id) {
    this.id = id;
  }

  public void setBiosampleStatus(OntologyTerm biosampleStatus) {
    this.biosampleStatus = biosampleStatus;
  }

  public void setSampleOriginType(OntologyTerm sampleOriginType) {
    this.sampleOriginType = sampleOriginType;
  }

  public void setCollectionMoment(String collectionMoment) {
    this.collectionMoment = collectionMoment;
  }

  public void setCollectionDate(String collectionDate) {
    this.collectionDate = collectionDate;
  }

  public void setObtentionProcedure(ObtentionProcedure obtentionProcedure) {
    this.obtentionProcedure = obtentionProcedure;
  }
}
