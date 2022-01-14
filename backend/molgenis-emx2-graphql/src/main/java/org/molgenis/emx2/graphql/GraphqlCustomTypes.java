package org.molgenis.emx2.graphql;

import graphql.schema.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.Part;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;

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
                  if (input instanceof String) {
                    return new BinaryFileWrapper("text/html", "text", ((String) input).getBytes());
                  } else if (input instanceof Part) {
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
                    throw new MolgenisException(
                        "Unknown data type given to graphql file input: "
                            + input.getClass().getSimpleName());
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
