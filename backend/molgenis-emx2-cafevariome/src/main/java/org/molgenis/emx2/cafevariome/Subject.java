package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Subject {

  boolean affectedOnly;
  Range age;
  Range ageFirstSymptoms;
  Range ageFirstDiagnosis;

  String gender;
  FamilyType familyType;
}
