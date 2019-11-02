package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.molgenis.emx2.Member;
import org.molgenis.emx2.Schema;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.molgenis.emx2.web.Constants.ACCEPT_JSON;
import static spark.Spark.*;

public class MembersApi {

  private MembersApi() {
    // hide constructor
  }

  public static void create() {

    final String membersPath = "/api/members/:schema"; // NOSONAR
    get(membersPath, MembersApi::membersGet);
    post(membersPath, MembersApi::membersPost);
    delete(membersPath, MembersApi::membersDelete);
  }

  static String membersDelete(Request request, Response response) throws IOException {
    List<Member> members = jsonToMembers(request.body());
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));
    schema.removeMembers(members);
    response.status(200);
    return "" + members.size();
  }

  static String membersPost(Request request, Response response) throws IOException {
    List<Member> members = jsonToMembers(request.body());
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));
    schema.addMembers(members);
    response.status(200);
    return "" + members.size();
  }

  static String membersGet(Request request, Response response) throws JsonProcessingException {
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));
    response.status(200);
    response.type(ACCEPT_JSON);
    return membersToJson(schema.getMembers());
  }

  private static List<Member> jsonToMembers(String json) throws IOException {
    return Arrays.asList(new ObjectMapper().readValue(json, Member[].class));
  }

  private static String membersToJson(List<Member> members) throws JsonProcessingException {
    return JsonApi.getWriter().writeValueAsString(members);
  }
}
