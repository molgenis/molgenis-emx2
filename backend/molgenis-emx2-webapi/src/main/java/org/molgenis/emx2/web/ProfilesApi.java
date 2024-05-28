package org.molgenis.emx2.web;

import static spark.Spark.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.profiles.SchemaFromProfile;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.json.JsonUtil;
import spark.Request;
import spark.Response;

public class ProfilesApi {
  private ProfilesApi() {
    // hide constructor
  }

  private static String profilesJson;

  public static void create() {
    get("/api/profiles", ProfilesApi::getProfiles);
    get("/:schema/api/profiles", ProfilesApi::getProfiles);
  }

  private static String getProfiles(Request request, Response response)
      throws IOException, URISyntaxException {
    if (profilesJson == null) {
      List<Row> rows =
          new SchemaFromProfile()
              .getProfilesFromAllModels(SchemaFromProfile.SHARED_MODELS_DIR, false);
      SchemaMetadata schema = Emx2.fromRowList(rows);
      profilesJson = JsonUtil.schemaToJson(schema);
    }
    response.header("Content-Disposition", "attachment; filename=profiles.json");
    return profilesJson;
  }
}
