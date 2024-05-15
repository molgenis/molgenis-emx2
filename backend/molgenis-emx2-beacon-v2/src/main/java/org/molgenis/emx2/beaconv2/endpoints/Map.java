package org.molgenis.emx2.beaconv2.endpoints;

import static org.molgenis.emx2.rdf.RDFUtils.extractHost;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import java.util.List;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import org.molgenis.emx2.beaconv2.EntryType;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Map {

  private final BeaconSpec spec;
  private final List<EntryType> entryTypes;
  private final String host;

  public Map(Request request) {
    this.spec = BeaconSpec.findByPath(request.attribute("specification"));
    this.entryTypes = EntryType.getEntryTypesOfSpec(spec);
    this.host = extractHost(request.url());
  }

  @JsonIgnore
  public JsonNode getResponse() {
    String jsltPath = "informational/map.jslt";
    Expression jslt = Parser.compileResource(jsltPath);
    return jslt.apply(new ObjectMapper().valueToTree(this));
  }
}
