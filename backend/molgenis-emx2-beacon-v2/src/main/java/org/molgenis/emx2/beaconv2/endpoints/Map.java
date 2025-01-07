package org.molgenis.emx2.beaconv2.endpoints;

import static org.molgenis.emx2.utils.URIUtils.extractHost;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import io.javalin.http.Context;
import java.util.List;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import org.molgenis.emx2.beaconv2.EntryType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Map {

  private BeaconSpec spec;
  private List<EntryType> entryTypes;
  private String host;

  public Map() {}

  @JsonIgnore
  public void getResponse(Context ctx) {
    this.spec = BeaconSpec.findByPath(ctx.attribute("specification"));
    this.entryTypes = EntryType.getEntryTypesOfSpec(spec);
    this.host = extractHost(ctx.url());
    String jsltPath = "informational/map.jslt";
    Expression jslt = Parser.compileResource(jsltPath);
    ctx.json(jslt.apply(new ObjectMapper().valueToTree(this)));
  }
}
