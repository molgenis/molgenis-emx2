package org.molgenis.emx2.beaconv2_prev;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ServiceInfo {

  String $schema = "http://json-schema.org/draft-07/schema";
  String title = "GA4GH service-info API specification";
  String description =
      "A way for a service to describe basic metadata concerning a service alongside a set of capabilities and/or limitations of the service. More information on [GitHub](https://github.com/ga4gh-discovery/ga4gh-service-info/).";
  String version = "1.0.0";
  String type = "object";
  String[] required = new String[] {"id", "name", "type", "organization", "version"};
  Properties properties = new Properties();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class Properties {
    ID id = new ID();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class ID {
      String type = "string";
      String description =
          "Unique ID of this service. Reverse domain name notation is recommended, though not required. The identifier should attempt to be globally unique so it can be used in downstream aggregator services e.g. Service Registry.";
      String example = "org.ga4gh.myservice";
    }

    Name name = new Name();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Name {
      String type = "string";
      String description = "Name of this service. Should be human readable.";
      String example = "My project";
    }

    Type type = new Type();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Type {
      String $ref = "#/definitions/ServiceType";
    }

    Description description = new Description();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Description {
      String type = "string";
      String description =
          "Description of the service. Should be human readable and provide information about the service.";
      String example = "This service provides...";
    }

    Organization organization = new Organization();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Organization {
      String type = "object";
      String description = "Organization providing the service";
      String[] required = new String[] {"name", "url"};
      OrganizationProperties properties = new OrganizationProperties();

      @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
      public class OrganizationProperties {
        Name name = new Name();

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public class Name {
          String type = "string";
          String description = "Name of the organization responsible for the service";
          String example = "My organization";
        }

        URL url = new URL();

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public class URL {
          String type = "string";
          String format = "uri";
          String description = "URL of the website of the organization (RFC 3986 format)";
          String example = "https://example.com";
        }
      }
    }

    ContactURL contactUrl = new ContactURL();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class ContactURL {
      String type = "string";
      String format = "uri";
      String description =
          "URL of the contact for the provider of this service, e.g. a link to a contact form (RFC 3986 format), or an email (RFC 2368 format).";
      String example = "mailto:support@example.com";
    }

    DocumentationURL documentationUrl = new DocumentationURL();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class DocumentationURL {
      String type = "string";
      String format = "uri";
      String description =
          "URL of the documentation of this service (RFC 3986 format). This should help someone learn how to use your service, including any specifics required to access data, e.g. authentication.";
      String example = "https://docs.myservice.example.com";
    }

    CreatedAt createdAt = new CreatedAt();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class CreatedAt {
      String type = "string";
      String format = "date-time";
      String description =
          "Timestamp describing when the service was first deployed and available (RFC 3339 format)";
      String example = "2019-06-04T12:58:19Z";
    }

    UpdatedAt updatedAt = new UpdatedAt();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class UpdatedAt {
      String type = "string";
      String format = "date-time";
      String description =
          "Timestamp describing when the service was last updated (RFC 3339 format)";
      String example = "2019-06-04T12:58:19Z";
    }

    Environment environment = new Environment();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Environment {
      String type = "string";
      String description =
          "Environment the service is running in. Use this to distinguish between production, development and testing/staging deployments. Suggested values are prod, test, dev, staging. However this is advised and not enforced.";
      String example = "test";
    }

    Version version = new Version();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Version {
      String type = "string";
      String description =
          "Version of the service being described. Semantic versioning is recommended, but other identifiers, such as dates or commit hashes, are also allowed. The version should be changed whenever the service is updated.";
      String example = "1.0.0";
    }
  }

  Definitions definitions = new Definitions();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class Definitions {
    ServiceType ServiceType = new ServiceType();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class ServiceType {
      String description = "Type of a GA4GH service";
      String type = "object";
      String[] required = new String[] {"group", "artifact", "version"};
      ServiceTypeProperties properties = new ServiceTypeProperties();

      @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
      public class ServiceTypeProperties {
        Group group = new Group();

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public class Group {
          String type = "string";
          String description =
              "Namespace in reverse domain name format. Use `org.ga4gh` for implementations compliant with official GA4GH specifications. For services with custom APIs not standardized by GA4GH, or implementations diverging from official GA4GH specifications, use a different namespace (e.g. your organization's reverse domain name).";
          String example = "org.ga4gh";
        }

        Artifact artifact = new Artifact();

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public class Artifact {
          String type = "string";
          String description =
              "Name of the API or GA4GH specification implemented. Official GA4GH types should be assigned as part of standards approval process. Custom artifacts are supported.";
          String example = "beacon";
        }

        ServiceTypePropertiesVersion version = new ServiceTypePropertiesVersion();

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public class ServiceTypePropertiesVersion {
          String type = "string";
          String description =
              "Version of the API or specification. GA4GH specifications use semantic versioning.";
          String example = "1.0.0";
        }
      }
    }
  }
}
