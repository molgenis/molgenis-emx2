package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record Range(Integer min, Integer max) {}
