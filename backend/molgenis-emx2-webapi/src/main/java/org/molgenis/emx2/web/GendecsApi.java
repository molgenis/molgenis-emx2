package org.molgenis.emx2.web;

import static spark.Spark.get;
import static spark.Spark.post;

import com.google.gson.*;
import java.util.ArrayList;
import org.molgenis.emx2.semantics.gendecs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class GendecsApi {
  private static final Logger logger = LoggerFactory.getLogger(GendecsApi.class);
  private static final String genesToPheno =
      "/Users/jonathan/Documents/GitHub/molgenis-emx2/data/gendecs/genes_to_phenotype.txt";

  public static void create() {
    post("/:schema/api/gendecs/queryHpo", GendecsApi::queryHpo);
    get("/:schema/api/gendecs/idToHpo/:id", GendecsApi::idToHpo);
    get("/:schema/api/gendecs/hpoToId/:hpoTerm", GendecsApi::hpoToId);
  }

  private static String queryHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    String hpoId = jsonObject.get("hpoId").getAsString();
    String hpoTermIn = jsonObject.get("hpoTerm").getAsString();
    JsonArray searchAssociates = jsonObject.get("searchAssociates").getAsJsonArray();

    HpoTerm hpoTerm = new HpoTerm(hpoTermIn);

    if (searchAssociates.toString().contains("more")) {
      logger.info("Started querying for the children of: " + hpoId);
      ArrayList<String> hpoTermChildren = OwlQuerier.getSubClasses(hpoId);
      logger.debug("resulting children terms: " + hpoTermChildren);
      hpoTerm.addChildren(hpoTermChildren);
    }
    if (searchAssociates.toString().contains("less")) {
      logger.info("Started querying for the parents of: " + hpoId);
      ArrayList<String> hpoTermsParent = OwlQuerier.getParentClasses(hpoId);
      logger.debug("resulting parent terms: " + hpoTermsParent);
      hpoTerm.setParents(hpoTermsParent);
      for (String parentTerm : hpoTermsParent) {
        String parentId = HpoConverter.getHpoId(parentTerm, genesToPheno);
        logger.info("Querying for the children of the parent with the id: " + parentId);
        ArrayList<String> hpoParentChildren = OwlQuerier.getSubClasses(parentId);
        hpoTerm.addChildren(hpoParentChildren);
      }
    }
    return serializeHpo(hpoTerm);
  }

  private static String idToHpo(Request request, Response response) {
    String id = request.params("id");
    return HpoConverter.getHpoTerm(id, genesToPheno);
  }

  private static String hpoToId(Request request, Response response) {
    String hpoTerm = request.params("hpoterm");
    return HpoConverter.getHpoId(hpoTerm, genesToPheno);
  }

  private static String serializeHpo(HpoTerm hpoTerm) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    return gson.toJson(hpoTerm);
  }
}
