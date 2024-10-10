package org.molgenis.emx2.beaconv2.requests;

import static org.molgenis.emx2.utils.URIUtils.extractHost;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import io.javalin.http.Context;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import org.molgenis.emx2.beaconv2.EntryType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconRequestBody {

  private String $schema;
  private BeaconRequestMeta meta = new BeaconRequestMeta();
  private BeaconQuery query = new BeaconQuery();

  public BeaconRequestBody() {}

  public BeaconRequestBody(Context ctx) {
    this.addRequestParameters(ctx);
  }

  public void addRequestParameters(Context ctx) {
    BeaconSpec specification = addSpecification(ctx);
    EntryType entryType = addUrlParameters(ctx);
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

  private BeaconSpec addSpecification(Context ctx) {
    String specification = ctx.attribute("specification").toString();
    BeaconSpec beaconSpec = BeaconSpec.findByPath(specification);
    meta.setSpecification(beaconSpec);
    return beaconSpec;
  }

  private EntryType addUrlParameters(Context ctx) {
    String host = extractHost(ctx.url());
    this.getMeta().setHost(host);
    return query.addUrlParameters(ctx);
  }
}
