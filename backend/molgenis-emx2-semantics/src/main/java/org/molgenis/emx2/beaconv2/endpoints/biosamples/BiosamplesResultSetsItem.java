package org.molgenis.emx2.beaconv2.endpoints.biosamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BiosamplesResultSetsItem {

  String id;
  OntologyTerm biosampleStatus;
  OntologyTerm[] sampleOriginType;
  String collectionMoment;
  String collectionDate;
  ObtentionProcedure obtentionProcedure;
}
