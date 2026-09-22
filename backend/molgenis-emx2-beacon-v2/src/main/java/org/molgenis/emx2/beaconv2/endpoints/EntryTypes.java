package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import org.molgenis.emx2.beaconv2.EntryType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypes {

  private List<EntryType> entryTypes = new ArrayList<>();
  private BeaconSpec spec;

  public EntryTypes() {}

  @JsonIgnore
  public JsonNode getResponse(Context ctx) {
    this.spec = BeaconSpec.findByPath(ctx.attribute("specification"));
    this.entryTypes = EntryType.getEntryTypesOfSpec(spec);
    String jsltPath = "informational/entry_types.jslt";
    Expression jslt = Parser.compileResource(jsltPath);
    return jslt.apply(new ObjectMapper().valueToTree(this));
  }
}
