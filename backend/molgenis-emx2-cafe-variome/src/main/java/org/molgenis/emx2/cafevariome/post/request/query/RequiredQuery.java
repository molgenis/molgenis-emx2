package org.molgenis.emx2.cafevariome.post.request.query;

public class RequiredQuery {
  private String metaSubjectVariantVersion;
  private String metaSubjectEAVVersion;
  private String metaPhenotypeVersion;
  private String metaQueryIDVersion;
  private String metaApiVersion;
  private String metaQueryID;
  private String metaQueryLabel;
  private String reqExistsVersion;
  private String reqCountVersion;
  private String networkKey;
  private String CSRFTokenName;

  public String getMetaSubjectVariantVersion() {
    return metaSubjectVariantVersion;
  }

  public void setMetaSubjectVariantVersion(String metaSubjectVariantVersion) {
    this.metaSubjectVariantVersion = metaSubjectVariantVersion;
  }

  public String getMetaSubjectEAVVersion() {
    return metaSubjectEAVVersion;
  }

  public void setMetaSubjectEAVVersion(String metaSubjectEAVVersion) {
    this.metaSubjectEAVVersion = metaSubjectEAVVersion;
  }

  public String getMetaPhenotypeVersion() {
    return metaPhenotypeVersion;
  }

  public void setMetaPhenotypeVersion(String metaPhenotypeVersion) {
    this.metaPhenotypeVersion = metaPhenotypeVersion;
  }

  public String getMetaQueryIDVersion() {
    return metaQueryIDVersion;
  }

  public void setMetaQueryIDVersion(String metaQueryIDVersion) {
    this.metaQueryIDVersion = metaQueryIDVersion;
  }

  public String getMetaApiVersion() {
    return metaApiVersion;
  }

  public void setMetaApiVersion(String metaApiVersion) {
    this.metaApiVersion = metaApiVersion;
  }

  public String getMetaQueryID() {
    return metaQueryID;
  }

  public void setMetaQueryID(String metaQueryID) {
    this.metaQueryID = metaQueryID;
  }

  public String getMetaQueryLabel() {
    return metaQueryLabel;
  }

  public void setMetaQueryLabel(String metaQueryLabel) {
    this.metaQueryLabel = metaQueryLabel;
  }

  public String getReqExistsVersion() {
    return reqExistsVersion;
  }

  public void setReqExistsVersion(String reqExistsVersion) {
    this.reqExistsVersion = reqExistsVersion;
  }

  public String getReqCountVersion() {
    return reqCountVersion;
  }

  public void setReqCountVersion(String reqCountVersion) {
    this.reqCountVersion = reqCountVersion;
  }

  public String getNetworkKey() {
    return networkKey;
  }

  public void setNetworkKey(String networkKey) {
    this.networkKey = networkKey;
  }

  public String getCSRFTokenName() {
    return CSRFTokenName;
  }

  public void setCSRFTokenName(String CSRFTokenName) {
    this.CSRFTokenName = CSRFTokenName;
  }

  @Override
  public String toString() {
    return "RequiredQuery{"
        + "metaSubjectVariantVersion='"
        + metaSubjectVariantVersion
        + '\''
        + ", metaSubjectEAVVersion='"
        + metaSubjectEAVVersion
        + '\''
        + ", metaPhenotypeVersion='"
        + metaPhenotypeVersion
        + '\''
        + ", metaQueryIDVersion='"
        + metaQueryIDVersion
        + '\''
        + ", metaApiVersion='"
        + metaApiVersion
        + '\''
        + ", metaQueryID='"
        + metaQueryID
        + '\''
        + ", metaQueryLabel='"
        + metaQueryLabel
        + '\''
        + ", reqExistsVersion='"
        + reqExistsVersion
        + '\''
        + ", reqCountVersion='"
        + reqCountVersion
        + '\''
        + ", networkKey='"
        + networkKey
        + '\''
        + ", CSRFTokenName='"
        + CSRFTokenName
        + '\''
        + '}';
  }
}
