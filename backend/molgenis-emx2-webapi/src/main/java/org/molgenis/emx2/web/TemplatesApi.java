package org.molgenis.emx2.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.YamlWorkspaceLoader;
import org.molgenis.emx2.datamodels.YamlWorkspaceLoader.TemplateInfo;

/**
 * Serves the merged list of schema-create templates: the built-in {@link DataModels} enum templates
 * (source {@code classic}) plus the discovered {@code /templates} YAML workspace bundles (source
 * {@code yaml}). The create-schema modal fetches this instead of a hardcoded client allowlist.
 */
public class TemplatesApi {

  private static final String SOURCE_CLASSIC = "classic";
  private static final String SOURCE_YAML = "yaml";
  private static final String TEMPLATE_PATH = "/api/templates/";

  private TemplatesApi() {
    // hide constructor
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TemplateEntry(
      String name,
      String label,
      String description,
      boolean hasDemoData,
      String source,
      String url) {}

  public static void create(Javalin app) {
    app.get("/api/templates", TemplatesApi::getTemplates);
    app.get("/api/templates/{name}", TemplatesApi::getTemplate);
  }

  private static void getTemplate(Context ctx) {
    String name = ctx.pathParam("name");
    YamlWorkspaceLoader workspace = new YamlWorkspaceLoader();
    if (workspace.hasTemplate(name)) {
      ctx.contentType(Constants.ACCEPT_YAML);
      ctx.status(200);
      ctx.result(workspace.toSingleFileWireForm(name));
      return;
    }
    ctx.status(404);
    ctx.result("no yaml template named '" + name + "'");
  }

  private static void getTemplates(Context ctx) {
    List<TemplateEntry> templates = new ArrayList<>();
    for (DataModels.Profile profile : DataModels.Profile.values()) {
      templates.add(classicEntry(profile.name()));
    }
    for (DataModels.Regular regular : DataModels.Regular.values()) {
      templates.add(classicEntry(regular.name()));
    }
    YamlWorkspaceLoader workspace = new YamlWorkspaceLoader();
    if (workspace.isAvailable()) {
      for (TemplateInfo info : workspace.availableTemplates()) {
        templates.add(
            new TemplateEntry(
                info.name(),
                info.label(),
                null,
                info.hasDemoData(),
                SOURCE_YAML,
                TEMPLATE_PATH + info.name()));
      }
    }
    ctx.json(templates);
  }

  private static TemplateEntry classicEntry(String name) {
    return new TemplateEntry(name, name, null, true, SOURCE_CLASSIC, null);
  }
}
