package org.molgenis.emx2.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.json.JsonUtil;

public class ProfilesApi {
  private ProfilesApi() {
    // hide constructor
  }

  private static String profilesJson;

  public static void create(Javalin app) {
    app.get("/api/profiles", ProfilesApi::getProfiles);
    app.get("/{schema}/api/profiles", ProfilesApi::getProfiles);
  }

  private static void getProfiles(Context ctx) throws IOException, URISyntaxException {
    if (profilesJson == null) {
      List<Row> rows =
          new SchemaFromProfile()
              .getProfilesFromAllModels(SchemaFromProfile.SHARED_MODELS_DIR, List.of());
      SchemaMetadata schema = Emx2.fromRowList(rows);
      profilesJson = JsonUtil.schemaToJson(schema);
    }
    ctx.header("Content-Disposition", "attachment; filename=profiles.json");
    ctx.json(profilesJson);
  }
}
