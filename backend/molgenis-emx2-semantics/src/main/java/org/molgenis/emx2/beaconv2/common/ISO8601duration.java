package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ISO8601duration {
  String iso8601duration;

  public ISO8601duration(String value) {
    this.iso8601duration = value;
  }
}
