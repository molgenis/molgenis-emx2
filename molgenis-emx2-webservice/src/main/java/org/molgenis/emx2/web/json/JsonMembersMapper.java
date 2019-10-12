package org.molgenis.emx2.web.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.molgenis.emx2.Member;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class JsonMembersMapper {
  private JsonMembersMapper() {
    // hide constructor
  }

  public static List<Member> jsonToMembers(String json) throws IOException {
    return Arrays.asList(new ObjectMapper().readValue(json, Member[].class));
  }

  public static String membersToJson(List<Member> members) throws JsonProcessingException {
    return JsonMapper.getWriter().writeValueAsString(members);
  }
}
