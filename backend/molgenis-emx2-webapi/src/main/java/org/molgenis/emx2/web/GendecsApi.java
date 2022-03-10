package org.molgenis.emx2.web;

import static spark.Spark.post;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.molgenis.emx2.semantics.gendecs.HpoTerm;
import org.molgenis.emx2.semantics.gendecs.OwlQuerier;
import spark.Request;
import spark.Response;

public class GendecsApi {

  public static void create() {
    post("/:schema/api/gendecs", GendecsApi::queryHpo);
  }

  private static String queryHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    String hpoId = jsonObject.get("hpoId").getAsString();

    OwlQuerier owlQuerier = new OwlQuerier(hpoId);
    HpoTerm hpoTerm = owlQuerier.executeQuery();

    return owlQuerier.serializeHpo(hpoTerm);
  }
}
