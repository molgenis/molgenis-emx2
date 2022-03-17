package org.molgenis.emx2.web;

import static spark.Spark.get;
import static spark.Spark.post;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import org.molgenis.emx2.semantics.gendecs.*;
import spark.Request;
import spark.Response;

public class GendecsApi {

  public static void create() {
    post("/:schema/api/gendecs/queryHpo", GendecsApi::queryHpo);
    get("/:scheme/api/gendecs/vcffile", GendecsApi::vcfToGene);
  }

  private static String queryHpo(Request request, Response response) {
    JsonObject jsonObject = JsonParser.parseString(request.body()).getAsJsonObject();
    String hpoId = jsonObject.get("hpoId").getAsString();

    OwlQuerier owlQuerier = new OwlQuerier(hpoId);
    HpoTerm hpoTerm = owlQuerier.executeQuery();

    return Serialize.serializeHpo(hpoTerm);
  }

  private static String vcfToGene(Request request, Response response) {
    String filenameClinvar = "data/gendecs/clinvar_20220205.vcf";
    String filenameData = "data/gendecs/vcfdata.vcf";
    StarRating starRating = StarRating.ONESTAR;

    VcfParser vcfParser = new VcfParser(filenameData, starRating);

    if (vcfParser.removeStatus(filenameClinvar)) {
      System.out.println(
          "Successfully removed " + starRating + " and below from " + filenameClinvar);
    }
    Variants variants = vcfParser.matchWithClinvar();

    HashMap<String, String> genesHpo = variants.getGeneHpo();
    return Serialize.serializeMap(genesHpo);
  }
}
