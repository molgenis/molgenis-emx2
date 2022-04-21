package org.molgenis.emx2.web;

import static spark.Spark.post;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import org.molgenis.emx2.semantics.gendecs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class GendecsApi {
  private static final Logger logger = LoggerFactory.getLogger(GendecsApi.class);

  public static void create() {
    post("/:schema/api/gendecs/queryHpo", GendecsApi::queryHpo);
  }

  private static String queryHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    String hpoId = jsonObject.get("hpoId").getAsString();
    String hpoTermIn = jsonObject.get("hpoTerm").getAsString();
    JsonArray searchAssociates = jsonObject.get("searchAssociates").getAsJsonArray();

    HpoTerm hpoTerm = new HpoTerm(hpoTermIn);
    OwlQuerier owlQuerier = new OwlQuerier(hpoId);

    if (searchAssociates.toString().contains("parents")) {
      logger.info("Started querying for the parents of: " + hpoId);
      ArrayList<String> hpoTermsParent = owlQuerier.getParentClasses();
      logger.debug("resulting parent terms: " + hpoTermsParent);
      hpoTerm.setParents(hpoTermsParent);
    }
    if (searchAssociates.toString().contains("children")) {
      logger.info("Started querying for the children of: " + hpoId);
      ArrayList<String> hpoTermChildren = owlQuerier.getSubClasses();
      logger.debug("resulting children terms: " + hpoTermChildren);
      hpoTerm.setChildren(hpoTermChildren);
    }
    return Serialize.serializeHpo(hpoTerm);
  }
}
