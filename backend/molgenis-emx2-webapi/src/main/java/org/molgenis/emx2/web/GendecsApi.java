package org.molgenis.emx2.web;

import static spark.Spark.post;

import spark.Request;
import spark.Response;

public class GendecsApi {
  private static MolgenisSessionManager sessionManager;

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;
    post("/:schema/api/gendecs", GendecsApi::queryHpo);
  }

  private static String queryHpo(Request request, Response response) {
    System.out.println(request.body());
    System.out.println("In queryHpo");
    return "I am in the queryHpo";
  }
}
