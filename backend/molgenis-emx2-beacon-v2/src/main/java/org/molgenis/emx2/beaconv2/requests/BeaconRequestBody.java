package org.molgenis.emx2.beaconv2.requests;

import static org.molgenis.emx2.rdf.RDFUtils.extractHost;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import org.molgenis.emx2.beaconv2.EntryType;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconRequestBody {

  private String $schema;
  private BeaconRequestMeta meta = new BeaconRequestMeta();
  private BeaconQuery query = new BeaconQuery();

  public BeaconRequestBody() {}

  public BeaconRequestBody(Request request) {
    this.addRequestParameters(request);
  }

  public void addRequestParameters(Request request) {
    BeaconSpec specification = addSpecification(request);
    EntryType entryType = addUrlParameters(request);
    if (!entryType.validateSpecification(specification)) {
      throw new MolgenisException(
          "Invalid entry type: %s, for specification %s".formatted(entryType, specification));
    }
  }

  public String get$schema() {
    return $schema;
  }

  public BeaconRequestMeta getMeta() {
    return meta;
  }

  public BeaconQuery getQuery() {
    return query;
  }

  private BeaconSpec addSpecification(Request request) {
    String specification = request.attribute("specification").toString();
    BeaconSpec beaconSpec = BeaconSpec.findByPath(specification);
    meta.setSpecification(beaconSpec);
    return beaconSpec;
  }

  private EntryType addUrlParameters(Request request) {
    String host = extractHost(request.url());
    this.getMeta().setHost(host);
    return query.addUrlParameters(request);
  }
}
