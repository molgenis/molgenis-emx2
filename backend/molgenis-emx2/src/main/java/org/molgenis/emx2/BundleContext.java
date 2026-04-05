package org.molgenis.emx2;

import java.util.Map;

public class BundleContext {
  private final String bundleName;
  private final String bundleDescription;
  private final SchemaMetadata bundleSchema;
  private final Map<String, SubsetEntry> subsetRegistry;
  private final Map<String, SubsetEntry> templateRegistry;

  public BundleContext(
      SchemaMetadata bundleSchema,
      Map<String, SubsetEntry> subsetRegistry,
      Map<String, SubsetEntry> templateRegistry) {
    this.bundleName = null;
    this.bundleDescription = null;
    this.bundleSchema = bundleSchema;
    this.subsetRegistry = subsetRegistry;
    this.templateRegistry = templateRegistry;
  }

  public BundleContext(
      String bundleName,
      String bundleDescription,
      SchemaMetadata bundleSchema,
      Map<String, SubsetEntry> subsetRegistry,
      Map<String, SubsetEntry> templateRegistry) {
    this.bundleName = bundleName;
    this.bundleDescription = bundleDescription;
    this.bundleSchema = bundleSchema;
    this.subsetRegistry = subsetRegistry;
    this.templateRegistry = templateRegistry;
  }

  public String getBundleName() {
    return bundleName;
  }

  public String getBundleDescription() {
    return bundleDescription;
  }

  public SchemaMetadata getBundleSchema() {
    return bundleSchema;
  }

  public Map<String, SubsetEntry> getSubsetRegistry() {
    return subsetRegistry;
  }

  public Map<String, SubsetEntry> getTemplateRegistry() {
    return templateRegistry;
  }
}
