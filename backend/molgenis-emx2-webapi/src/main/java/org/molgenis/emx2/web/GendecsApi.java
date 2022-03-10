package org.molgenis.emx2.web;

import static spark.Spark.post;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.molgenis.emx2.semantics.gendecs.OwlQuerier;
import spark.Request;
import spark.Response;

public class GendecsApi {
  private static MolgenisSessionManager sessionManager;

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;
    post("/:schema/api/gendecs", GendecsApi::queryHpo);
  }

  private static String queryHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    String hpoId = jsonObject.get("hpoId").getAsString();

    OwlQuerier owlQuerier = new OwlQuerier(hpoId);
    owlQuerier.executeQuery();
    System.out.println(owlQuerier.getParents());
    System.out.println(owlQuerier.getSubClasses());
    return "I am in the queryHpo";
  }
}
