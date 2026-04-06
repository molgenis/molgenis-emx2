package org.molgenis.emx2;

import java.util.Map;

public class BundleContext {
  private final String bundleName;
  private final String bundleDescription;
  private final SchemaMetadata bundleSchema;
  private final Map<String, ProfileEntry> profileRegistry;
  private final Map<String, ProfileEntry> templateRegistry;

  public BundleContext(
      SchemaMetadata bundleSchema,
      Map<String, ProfileEntry> profileRegistry,
      Map<String, ProfileEntry> templateRegistry) {
    this.bundleName = null;
    this.bundleDescription = null;
    this.bundleSchema = bundleSchema;
    this.profileRegistry = profileRegistry;
    this.templateRegistry = templateRegistry;
  }

  public BundleContext(
      String bundleName,
      String bundleDescription,
      SchemaMetadata bundleSchema,
      Map<String, ProfileEntry> profileRegistry,
      Map<String, ProfileEntry> templateRegistry) {
    this.bundleName = bundleName;
    this.bundleDescription = bundleDescription;
    this.bundleSchema = bundleSchema;
    this.profileRegistry = profileRegistry;
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

  public Map<String, ProfileEntry> getProfileRegistry() {
    return profileRegistry;
  }

  public Map<String, ProfileEntry> getTemplateRegistry() {
    return templateRegistry;
  }
}
