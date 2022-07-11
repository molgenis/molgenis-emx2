package org.molgenis.emx2.beaconv2.responses;

import org.molgenis.emx2.beaconv2.common.BeaconEnvironment;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/ga4gh-service-info-1-0-0-schema.json
public class BeaconServiceInfoResponse {
  // "Unique ID of this service. Reverse domain name notation is recommended, though not required.
  // The identifier should attempt to be globally unique so it can be used in downstream aggregator
  // services e.g. Service Registry.",
  private String id;

  // Name of this service. Should be human readable.
  private String name;
  private ServiceType type;

  // Description of the service. Should be human readable and provide information about the service.
  // todo, use schema description?
  private String description;

  // Organization providing the service
  // todo, probably requires us to create org metadata in a schema
  private Organization organization;

  // URL of the contact for the provider of this service, e.g. a link to a contact form (RFC 3986
  // format), or an email (RFC 2368 format).
  // todo, probably requires us to create org metadata in a schema
  private String contactUrl;

  // URL of the documentation of this service (RFC 3986 format). This should help someone learn how
  // to use your service, including any specifics required to access data, e.g. authentication.
  private String documentationUrl;

  // Timestamp describing when the service was first deployed and available (RFC 3339 format)
  private String createdAt;

  // Timestamp describing when the service was last updated (RFC 3339 format)
  private String updatedAt;

  // Environment the service is running in. Use this to distinguish between production, development
  // and testing/staging deployments. Suggested values are prod, test, dev, staging. However this is
  // advised and not enforced.
  private BeaconEnvironment environment;

  // Version of the service being described. Semantic versioning is recommended, but other
  // identifiers, such as dates or commit hashes, are also allowed. The version should be changed
  // whenever the service is updated.
  // todo: molgenis version?
  private String version;

  // https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/ga4gh-service-info-1-0-0-schema.json
  public static class ServiceType {
    // "Namespace in reverse domain name format. Use `org.ga4gh` for implementations compliant with
    // official GA4GH specifications. For services with custom APIs not standardized by GA4GH, or
    // implementations diverging from official GA4GH specifications, use a different namespace (e.g.
    // your organization's reverse domain name)."
    private String group;

    // "Name of the API or GA4GH specification implemented. Official GA4GH types should be assigned
    // as
    // part of standards approval process. Custom artifacts are supported.",
    //          "example": "beacon"
    private String artifact;

    // "Version of the API or specification. GA4GH specifications use semantic versioning.",
    private String version;
  }

  public static class Organization {
    private String name;
    private String url;
  }
}
