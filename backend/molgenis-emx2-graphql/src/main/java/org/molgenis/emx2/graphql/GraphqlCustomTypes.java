package org.molgenis.emx2.graphql;

import static graphql.scalars.util.Kit.typeName;

import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.servlet.http.Part;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.utils.TypeUtils;

public class GraphqlCustomTypes {

  private GraphqlCustomTypes() {
    // hide constructor
  }

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

  public static final GraphQLScalarType GraphQLDuration =
      GraphQLScalarType.newScalar()
          .name("Duration")
          .description("A ISO 8601 duration scalar with year, month, week and day components.")
          .coercing(
              new Coercing<Integer, String>() {
                @Override
                public String serialize(Object input) throws CoercingSerializeException {
                  return TypeUtils.intToDuration(input);
                }

                @Override
                public Integer parseValue(Object input) throws CoercingParseValueException {
                  return TypeUtils.durationToInt(input);
                }

                @Override
                public Integer parseLiteral(Object input) throws CoercingParseLiteralException {
                  if (!(input instanceof StringValue)) {
                    throw new CoercingParseLiteralException(
                        "Expected AST type 'StringValue' but was '" + typeName(input) + "'.");
                  }
                  return TypeUtils.durationToInt(((StringValue) input).getValue());
                }

                @Override
                public Value<?> valueToLiteral(Object input) {
                  String s = serialize(input);
                  return StringValue.newStringValue(s).build();
                }
              })
          .build();
}
