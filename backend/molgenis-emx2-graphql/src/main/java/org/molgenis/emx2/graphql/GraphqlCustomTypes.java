package org.molgenis.emx2.graphql;

import graphql.schema.*;
import java.io.IOException;
import javax.servlet.http.Part;
import org.molgenis.emx2.BinaryFileWrapper;

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
                  if (input instanceof Part) {
                    Part part = (Part) input;
                    try {
                      String contentType = part.getContentType();
                      byte[] content = new byte[part.getInputStream().available()];
                      String fileName = part.getSubmittedFileName();
                      part.delete();
                      return new BinaryFileWrapper(contentType, fileName, content);
                    } catch (IOException e) {
                      throw new CoercingParseValueException(
                          "Couldn't read content of the uploaded file");
                    }
                  } else if (null == input) {
                    return null;
                  } else {
                    throw new CoercingParseValueException(
                        "Expected type "
                            + Part.class.getName()
                            + " but was "
                            + input.getClass().getName());
                  }
                }

                @Override
                public BinaryFileWrapper parseLiteral(Object input) {
                  throw new CoercingParseLiteralException(
                      "Must use variables to specify Upload values");
                }
              })
          .build();
}
