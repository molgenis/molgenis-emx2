package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Response(Integer recordCount, Range recordRange, boolean exists) {}
