package org.molgenis.emx2.cafevariome.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.molgenis.emx2.cafevariome.Range;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecordResponse(Integer recordCount, Range recordRange, boolean exist) {}
