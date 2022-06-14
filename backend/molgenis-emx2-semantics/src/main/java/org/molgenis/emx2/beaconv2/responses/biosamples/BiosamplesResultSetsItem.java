package org.molgenis.emx2.beaconv2.responses.biosamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Ontology;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BiosamplesResultSetsItem {

  String id;
  Ontology biosampleStatus;
  Ontology[] sampleOriginType;
  String collectionMoment;
  String collectionDate;
  ObtentionProcedure obtentionProcedure;
}
