package org.molgenis.emx2.beaconv2.responses.biosamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BiosamplesResultSetsItem {

  String id;
  String biosampleStatus_id;
  String biosampleStatus_label;
  String sampleOriginType_id;
  String sampleOriginType_label;
  String collectionMoment;
  String collectionDate;
  ObtentionProcedure obtentionProcedure;
}
