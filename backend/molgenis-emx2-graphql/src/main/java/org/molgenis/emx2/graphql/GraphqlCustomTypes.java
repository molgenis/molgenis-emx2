package org.molgenis.emx2.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.language.StringValue;
import graphql.schema.*;
import jakarta.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.Profile;
import org.molgenis.emx2.utils.MolgenisObjectMapper;

public class GraphqlCustomTypes {
  private static final MolgenisObjectMapper objectMapper = MolgenisObjectMapper.INTERNAL;

  private GraphqlCustomTypes() {
    // hide constructor
  }

  public static final GraphQLScalarType GraphQLJsonAsString =
      GraphQLScalarType.newScalar()
          .name("JsonString")
          .description("A JSON represented as string")
          .coercing(
              new Coercing<String, String>() {

                @Override
                public String serialize(Object dataFetcherResult) {
                  // Convert Java object to JSON string
                  try {
                    return objectMapper.getWriter().writeValueAsString(dataFetcherResult);
                  } catch (JsonProcessingException e) {
                    throw new CoercingSerializeException(
                        "Unable to serialize to JSON string: " + e.getMessage());
                  }
                }

                @Override
                public String parseValue(Object input) {
                  // Pass-through parsing (only used for input values)
                  return input.toString();
                }

                @Override
                public String parseLiteral(Object input) {
                  // Pass-through literal parsing
                  if (input instanceof graphql.language.StringValue) {
                    return ((graphql.language.StringValue) input).getValue();
                  }
                  throw new CoercingParseLiteralException("Value is not a valid JSON string");
                }
              })
          .build();

  // thanks to https://stackoverflow.com/questions/57372259/how-to-upload-files-with-graphql-java
  public static final GraphQLScalarType GraphQLFileUpload =
      GraphQLScalarType.newScalar()
          .name("FileUpload")
          .description("A file part in a multipart request")
          .coercing(
              new Coercing<BinaryFileWrapper, Void>() {

                @Override
                public Void serialize(Object dataFetcherResult) {
                  throw new CoercingSerializeException("Upload is an input-only type");
                }

                @Override
                public BinaryFileWrapper parseValue(Object input) {
                  if (input instanceof Map) {
                    // when user re-submitted the file metadata we skip update (skip flag)
                    return new BinaryFileWrapper(true);
                  } else if (input instanceof Part) {
                    // when submitted new file instance we will apply insert/update
                    Part part = (Part) input;
                    try (InputStream is = part.getInputStream(); ) {
                      String contentType = part.getContentType();

                      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                      int nRead;
                      byte[] data = new byte[1024];
                      while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                      }
                      buffer.flush();
                      byte[] content = buffer.toByteArray();
                      String fileName = part.getSubmittedFileName();
                      part.delete();
                      return new BinaryFileWrapper(contentType, fileName, content);
                    } catch (IOException e) {
                      throw new CoercingParseValueException(
                          "Couldn't read content of the uploaded file");
                    }
                  } else {
                    return null;
                  }
                }

                @Override
                public BinaryFileWrapper parseLiteral(Object input) {
                  throw new CoercingParseLiteralException(
                      "Must use variables to specify Upload values");
                }
              })
          .build();

  public static final GraphQLScalarType GraphQLProfileType =
      GraphQLScalarType.newScalar()
          .name("Profile")
          .description(
              "A string representing a profile, one of the following: "
                  + Arrays.stream(Profile.values())
                      .map(Profile::name)
                      .collect(Collectors.joining(", ")))
          .coercing(
              new Coercing<Profile, String>() {
                @Override
                public String serialize(Object dataFetcherResult) {
                  return ((Profile) dataFetcherResult).name();
                }

                @Override
                public Profile parseValue(Object input) {
                  return (Profile) input;
                }

                @Override
                public Profile parseLiteral(Object input) {
                  try {
                    StringValue stringValue = (StringValue) input;
                    String value = stringValue.getValue();
                    return Profile.valueOf(value);
                  } catch (Exception e) {
                    throw new CoercingSerializeException("Invalid profile", e);
                  }
                }
              })
          .build();
}
