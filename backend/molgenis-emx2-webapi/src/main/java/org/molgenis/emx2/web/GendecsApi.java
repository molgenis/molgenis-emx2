package org.molgenis.emx2.web;

import static spark.Spark.post;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import org.molgenis.emx2.semantics.gendecs.*;
import org.molgenis.emx2.semantics.gendecs.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class GendecsApi {
  private static final Logger logger = LoggerFactory.getLogger(GendecsApi.class);
  private static final ArrayList<HpoTerm> hpoTermsObjects = new ArrayList<>();
  private static ArrayList<Variant> variantsList = new ArrayList<>();

  public static void create() {
    post("/:schema/api/gendecs/queryHpo", GendecsApi::queryHpo);
    post("/:scheme/api/gendecs/vcffile", GendecsApi::matchVcfWithHpo);
  }

  private static String queryHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    String hpoId = jsonObject.get("hpoId").getAsString();
    String hpoTermIn = jsonObject.get("hpoTerm").getAsString();
    JsonArray searchAssociates = jsonObject.get("searchAssociates").getAsJsonArray();

    HpoTerm hpoTerm = new HpoTerm(hpoTermIn);
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
    hpoTermsObjects.add(hpoTerm);
    return Serialize.serializeHpo(hpoTerm);
  }

  private static String matchVcfWithHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    JsonArray hpoTermsIn = jsonObject.get("hpoTerms").getAsJsonArray();

    if (hpoTermsObjects.size() == 0) {
      for (int i = 0; i < hpoTermsIn.size(); i++) {
        HpoTerm hpoTerm = new HpoTerm(hpoTermsIn.get(i).getAsString());
        hpoTermsObjects.add(hpoTerm);
      }
    }
    String variants = getVariants();

    hpoTermsObjects.clear();
    return variants;
  }

  private static String getVariants() {
    StarRating starRating = StarRating.ONESTAR;
    ClinvarFilter clinvarFilter = new ClinvarFilter(starRating);
    String filteredClinvar = clinvarFilter.removeStatus();

    logger.info("Removed " + starRating + " and lower from " + Constants.FILENAMECLINVAR);
    logger.info("Matching variants with the entered HPO terms");
    logger.debug("Matching variants with the following HPO terms: " + hpoTermsObjects);
    HpoMatcher hpoMatcher = new HpoMatcher(hpoTermsObjects, filteredClinvar);
    variantsList = hpoMatcher.getHpoMatches();
    return Serialize.serialzeVariants(variantsList);
  }
}
