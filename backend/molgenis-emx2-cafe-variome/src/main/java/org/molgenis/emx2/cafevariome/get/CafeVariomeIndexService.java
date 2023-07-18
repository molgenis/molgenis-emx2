package org.molgenis.emx2.cafevariome.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.molgenis.emx2.Table;
import spark.Request;

import java.util.List;

public class CafeVariomeIndexService {

  private static ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  public static String query(Request request, List<Table> tables) throws Exception {

    return "";

  }
}
