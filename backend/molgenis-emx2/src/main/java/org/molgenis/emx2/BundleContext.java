package org.molgenis.emx2;

import java.util.Map;

public class BundleContext {
  private final String bundleName;
  private final String bundleDescription;
  private final SchemaMetadata bundleSchema;
  private final Map<String, ProfileEntry> internalProfileRegistry;
  private final Map<String, ProfileEntry> profileRegistry;

  public BundleContext(
      SchemaMetadata bundleSchema,
      Map<String, ProfileEntry> internalProfileRegistry,
      Map<String, ProfileEntry> profileRegistry) {
    this.bundleName = null;
    this.bundleDescription = null;
    this.bundleSchema = bundleSchema;
    this.internalProfileRegistry = internalProfileRegistry;
    this.profileRegistry = profileRegistry;
  }

  public BundleContext(
      String bundleName,
      String bundleDescription,
      SchemaMetadata bundleSchema,
      Map<String, ProfileEntry> internalProfileRegistry,
      Map<String, ProfileEntry> profileRegistry) {
    this.bundleName = bundleName;
    this.bundleDescription = bundleDescription;
    this.bundleSchema = bundleSchema;
    this.internalProfileRegistry = internalProfileRegistry;
    this.profileRegistry = profileRegistry;
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

  public Map<String, ProfileEntry> getInternalProfileRegistry() {
    return internalProfileRegistry;
  }

  public Map<String, ProfileEntry> getProfileRegistry() {
    return profileRegistry;
  }
}
