package org.molgenis.emx2.beaconv2.endpoints;

import static org.molgenis.emx2.rdf.RDFUtils.extractHost;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import spark.Request;
import spark.Response;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Info {

  private String host;
  private BeaconSpec spec;

  public Info() {}

  @JsonIgnore
  public JsonNode getResponse(Request request, Response response) {
    this.host = extractHost(request.url());
    this.spec = BeaconSpec.findByPath(request.attribute("specification"));
    String jsltPath = "informational/info.jslt";
    Expression jslt = Parser.compileResource(jsltPath);
    return jslt.apply(new ObjectMapper().valueToTree(this));
  }
}
