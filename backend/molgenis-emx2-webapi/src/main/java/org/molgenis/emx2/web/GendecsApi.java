package org.molgenis.emx2.web;

import static spark.Spark.post;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import org.molgenis.emx2.semantics.gendecs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class GendecsApi {
  static String filenameClinvar = "data/gendecs/clinvar_20220205.vcf";
  static String filenameData = "data/gendecs/vcfdata.vcf";
  private static final Logger logger = LoggerFactory.getLogger(GendecsApi.class);

  public static void create() {
    post("/:schema/api/gendecs/queryHpo", GendecsApi::queryHpo);
    post("/:scheme/api/gendecs/vcffile", GendecsApi::matchVcfWithHpo);
  }

  private static String queryHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    String hpoId = jsonObject.get("hpoId").getAsString();

    OwlQuerier owlQuerier = new OwlQuerier(hpoId);
    HpoTerm hpoTerm = owlQuerier.executeQuery();

    return Serialize.serializeHpo(hpoTerm);
  }

  private static String matchVcfWithHpo(Request request, Response response) {
    ArrayList<String> hpoTerms = new ArrayList<>();

    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    hpoTerms.add(jsonObject.get("hpoTerm").getAsString());

    if (jsonObject.get("hpoChildren") != null) {
      addAssociates("hpoChildren", hpoTerms, jsonObject);
    }
    if (jsonObject.get("hpoParents") != null) {
      addAssociates("hpoParents", hpoTerms, jsonObject);
    }

    StarRating starRating = StarRating.ONESTAR;
    VcfParser vcfParser = new VcfParser(filenameData, starRating, hpoTerms);

    if (vcfParser.removeStatus(filenameClinvar)) {
      logger.info("Successfully removed " + starRating + " and below from " + filenameClinvar);
    }

    Variants variants = vcfParser.matchWithClinvar();

    HashMap<String, String> genesHpo = variants.getGeneHpo();

    return Serialize.serializeMap(genesHpo);
  }

  private static void addAssociates(
      String name, ArrayList<String> hpoTerms, JsonObject jsonObject) {
    JsonArray hpoChildren = jsonObject.get(name).getAsJsonArray();
    for (int i = 0; i < hpoChildren.size(); i++) {
      hpoTerms.add(hpoChildren.get(i).getAsString());
    }
  }
}
