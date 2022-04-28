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
      for (String parentId : hpoTermsParent) {
        logger.info("Querying for the children of the parent with the id: " + parentId);
        ArrayList<String> hpoParentChildren = OwlQuerier.getSubClasses(parentId);
        hpoTerm.addChildren(hpoParentChildren);
      }
    }
    return Serialize.serializeHpo(hpoTerm);
  }
}
