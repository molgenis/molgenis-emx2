package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ISO8601duration {
  private String iso8601duration;

  /**
   * Constructor (behaves like a setter because there is only 1 variable...)
   *
   * @param value
   */
  public ISO8601duration(String value) {
    this.iso8601duration = value;
  }

  public String getIso8601duration() {
    return iso8601duration;
  }
}
