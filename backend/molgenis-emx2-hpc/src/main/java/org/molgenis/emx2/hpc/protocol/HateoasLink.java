package org.molgenis.emx2.hpc.protocol;

/** A single HATEOAS link with href and HTTP method. */
public record HateoasLink(String href, String method) {

  public static HateoasLink get(String href) {
    return new HateoasLink(href, "GET");
  }

  public static HateoasLink post(String href) {
    return new HateoasLink(href, "POST");
  }

  public static HateoasLink put(String href) {
    return new HateoasLink(href, "PUT");
  }

  public static HateoasLink delete(String href) {
    return new HateoasLink(href, "DELETE");
  }
}
