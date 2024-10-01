package org.molgenis.emx2.cafevariome.post.request.parameters;

import java.util.Arrays;
import java.util.List;

public class RequiredQueryParameters {
  public static final String META_SUBJECT_VARIANT_VERSION =
      "jsonAPI[meta][request][components][search][subjectVariant]";
  public static final String META_SUBJECT_EAV_VERSION =
      "jsonAPI[meta][request][components][search][eav]";
  public static final String META_PHENOTYPE_VERSION =
      "jsonAPI[meta][request][components][search][phenotype]";
  public static final String META_QUERY_ID_VERSION =
      "jsonAPI[meta][request][components][search][queryIdentification]";
  public static final String META_API_VERSION = "jsonAPI[meta][apiVersion]";
  public static final String META_QUERY_ID =
      "jsonAPI[meta][components][queryIdentification][queryID]";
  public static final String META_QUERY_LABEL =
      "jsonAPI[meta][components][queryIdentification][queryLabel]";
  public static final String REQ_EXISTS_VERSION =
      "jsonAPI[requires][response][components][collection][exists]";
  public static final String REQ_COUNT_VERSION =
      "jsonAPI[requires][response][components][collection][count]";
  public static final String NETWORK_KEY = "network_key";
  public static final String CSRF_TOKEN_NAME = "csrf_test_name";
  public static final List<String> reqParams =
      Arrays.asList(
          META_SUBJECT_VARIANT_VERSION,
          META_SUBJECT_EAV_VERSION,
          META_PHENOTYPE_VERSION,
          META_QUERY_ID_VERSION,
          META_API_VERSION,
          META_QUERY_ID,
          META_QUERY_LABEL,
          REQ_EXISTS_VERSION,
          REQ_COUNT_VERSION,
          NETWORK_KEY,
          CSRF_TOKEN_NAME);
}
