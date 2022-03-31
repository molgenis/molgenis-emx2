package org.molgenis.emx2.web;

import static spark.Spark.post;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import org.molgenis.emx2.semantics.gendecs.*;
import org.molgenis.emx2.semantics.gendecs.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class GendecsApi {
  private static final Logger logger = LoggerFactory.getLogger(GendecsApi.class);

  public static void create() {
    post("/:schema/api/gendecs/queryHpo", GendecsApi::queryHpo);
    post("/:scheme/api/gendecs/vcffile", GendecsApi::matchVcfWithHpo);
  }

  private static String queryHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    String hpoId = jsonObject.get("hpoId").getAsString();
    JsonArray searchAssociates = jsonObject.get("searchAssociates").getAsJsonArray();

    HpoTerm hpoTerm = new HpoTerm();
    OwlQuerier owlQuerier = new OwlQuerier(hpoId);

    logger.info("Started querying for parents and children of: " + hpoId);
    if (searchAssociates.toString().contains("parents")) {
      ArrayList<String> hpoTermsParent = owlQuerier.getParentClasses();
      hpoTerm.setParents(hpoTermsParent);
    }
    if (searchAssociates.toString().contains("children")) {
      ArrayList<String> hpoTermChildren = owlQuerier.getSubClasses();
      hpoTerm.setChildren(hpoTermChildren);
    }

    return Serialize.serializeHpo(hpoTerm);
  }

  private static String matchVcfWithHpo(Request request, Response response) {
    ArrayList<String> hpoTerms = new ArrayList<>();

    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    JsonArray hpoTermsIn = jsonObject.get("hpoTerms").getAsJsonArray();

    for (int i = 0; i < hpoTermsIn.size(); i++) {
      hpoTerms.add(hpoTermsIn.get(i).getAsString());
    }
    if (jsonObject.get("hpoChildren") != null) {
      addAssociates("hpoChildren", hpoTerms, jsonObject);
    }
    if (jsonObject.get("hpoParents") != null) {
      addAssociates("hpoParents", hpoTerms, jsonObject);
    }

    HashMap<String, String> genesHpo = getGenesHpo(hpoTerms);

    return Serialize.serializeMap(genesHpo);
  }

  private static HashMap<String, String> getGenesHpo(ArrayList<String> hpoTerms) {
    StarRating starRating = StarRating.ONESTAR;
    ClinvarFilter clinvarFilter = new ClinvarFilter(starRating);
    String filteredClinvar = clinvarFilter.removeStatus();

    logger.info("Removed " + starRating + " and lower from " + Constants.FILENAMECLINVAR);
    logger.info("Matching variants with the entered HPO terms");
    logger.debug("Matching variants with the following HPO terms: " + hpoTerms);
    HpoMatcher hpoMatcher = new HpoMatcher(hpoTerms, filteredClinvar);
    Variants variants = hpoMatcher.getHpoMatches();

    return variants.getGeneHpo();
  }

  private static void addAssociates(
      String name, ArrayList<String> hpoTerms, JsonObject jsonObject) {
    JsonArray hpoChildren = jsonObject.get(name).getAsJsonArray();
    for (int i = 0; i < hpoChildren.size(); i++) {
      hpoTerms.add(hpoChildren.get(i).getAsString());
    }
  }
}
