package org.molgenis.emx2.rdf;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.Schema;

public enum RDFServiceFactory {
  TABLE(RDFTableService.class) {
    @Override
    public RDFService getNewInstance(String baseURL, String rdfAPIPath, RDFFormat format) {
      return new RDFTableService(baseURL, rdfAPIPath, format);
    }
  },
  SEMANTIC(RDFSemanticService.class) {
    @Override
    public RDFService getNewInstance(String baseURL, String rdfAPIPath, RDFFormat format) {
      return new RDFSemanticService(baseURL, rdfAPIPath, format);
    }
  };

  private Class<? extends RDFService> rdfServiceClass;

  RDFServiceFactory(Class<? extends RDFService> rdfServiceClass) {
    this.rdfServiceClass = rdfServiceClass;
  }

  /**
   * Defines which RDFService to use. If any database does not require semantics, uses {@link
   * RDFTableService}. Otherwise, uses {@link RDFSemanticService}.
   *
   * @param schemas
   * @return the {@link RDFService} to be used
   */
  public static RDFServiceFactory defineService(Schema[] schemas) {
    for (Schema schema : schemas) {
      if (defineService(schema) != SEMANTIC) return TABLE;
    }
    return SEMANTIC;
  }

  /**
   * Defines which RDFService to use. If semantics are required uses {@link RDFSemanticService},
   * otherwise uses {@link RDFTableService}.
   *
   * @param schemas
   * @return the {@link RDFService} to be used
   */
  public static RDFServiceFactory defineService(Schema schema) {
    if (!schema.hasSetting(RDFService.SETTING_SEMANTICS_REQUIRED)
        || !Boolean.parseBoolean(schema.getSettingValue(RDFService.SETTING_SEMANTICS_REQUIRED))) {
      return TABLE;
    }
    return SEMANTIC;
  }

  public abstract RDFService getNewInstance(
      final String baseURL, final String rdfAPIPath, final RDFFormat format);
}
