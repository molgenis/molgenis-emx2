package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CafeVariomeQuery {

  Subject subject;
  List<HPO> hpo;
  Advanced advanced;
}
