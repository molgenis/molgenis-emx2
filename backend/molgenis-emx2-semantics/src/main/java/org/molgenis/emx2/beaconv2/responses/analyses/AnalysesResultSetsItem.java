package org.molgenis.emx2.beaconv2.responses.analyses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AnalysesResultSetsItem {

  String id;
  String runId;
  String biosampleId;
  String individualId;
  String analysisDate; // todo LocalDate using jackson-datatype-jsr310 ?
  String pipelineName;
  String pipelineRef;
  String aligner;
  String variantCaller;

  public AnalysesResultSetsItem(
      String id,
      String runId,
      String biosampleId,
      String individualId,
      String analysisDate,
      String pipelineName,
      String pipelineRef,
      String aligner,
      String variantCaller) {
    this.id = id;
    this.runId = runId;
    this.biosampleId = biosampleId;
    this.individualId = individualId;
    this.analysisDate = analysisDate;
    this.pipelineName = pipelineName;
    this.pipelineRef = pipelineRef;
    this.aligner = aligner;
    this.variantCaller = variantCaller;
  }

  public AnalysesResultSetsItem() {
    super();
  }
}
