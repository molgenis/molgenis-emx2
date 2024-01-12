package org.molgenis.emx2.beaconv2.endpoints.analyses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AnalysesResultSetsItem {

  private String id;
  private String runId;
  private String biosampleId;
  private String individualId;
  private String analysisDate; // todo LocalDate using jackson-datatype-jsr310 ?
  private String pipelineName;
  private String pipelineRef;
  private String aligner;
  private String variantCaller;

  public void setId(String id) {
    this.id = id;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  public void setBiosampleId(String biosampleId) {
    this.biosampleId = biosampleId;
  }

  public void setIndividualId(String individualId) {
    this.individualId = individualId;
  }

  public void setAnalysisDate(String analysisDate) {
    this.analysisDate = analysisDate;
  }

  public void setPipelineName(String pipelineName) {
    this.pipelineName = pipelineName;
  }

  public void setPipelineRef(String pipelineRef) {
    this.pipelineRef = pipelineRef;
  }

  public void setAligner(String aligner) {
    this.aligner = aligner;
  }

  public void setVariantCaller(String variantCaller) {
    this.variantCaller = variantCaller;
  }
}
