package org.molgenis.emx2.signin;

import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.graphql.GraphqlConstants.EMAIL;
import static org.molgenis.emx2.graphql.GraphqlConstants.PASSWORD;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.graphql.GraphqlApiMutationResult;
import org.molgenis.emx2.graphql.GraphqlApiMutationResultWithToken;
import org.molgenis.emx2.graphql.GraphqlSessionHandlerInterface;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;

public class SigninDataFetcher implements DataFetcher<Object> {

  private final RateLimit rateLimit = RateLimit.getInstance();
  private final Database database;

  public SigninDataFetcher(Database database) {
    this.database = database;
  }

  @Override
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    String userName = dataFetchingEnvironment.getArgument(EMAIL);
    String passWord = dataFetchingEnvironment.getArgument(PASSWORD);

    if (userName == null) {
      return new GraphqlApiMutationResult(FAILED, "Sign in failed: user or password not provided");
    }

    Optional<Instant> blockedUntil = rateLimit.getRateLimit(userName);
    if (blockedUntil.isPresent()) {
      rateLimit.onBlocked();
      return new GraphqlApiMutationResult(
          FAILED,
          "Sign in as '%s' failed: too many attempts, try again after %s",
          userName,
          blockedUntil.get().truncatedTo(ChronoUnit.SECONDS));
    }

    if (database.hasUser(userName) && database.checkUserPassword(userName, passWord)) {
      rateLimit.onSuccess(userName);
      if (database.getUser(userName).getEnabled()) {
        GraphqlSessionHandlerInterface sessionHandler =
            dataFetchingEnvironment.getGraphQlContext().get(GraphqlSessionHandlerInterface.class);
        sessionHandler.createSession(userName);
        // token can only be created as that user
        // to make sure we don't change database user we create new instance
        Database temp = new SqlDatabase(false);
        temp.setActiveUser(userName);
        return new GraphqlApiMutationResultWithToken(
            GraphqlApiMutationResult.Status.SUCCESS,
            JWTgenerator.createTemporaryToken(temp, userName),
            "Signed in as '%s'",
            userName);
      } else {
        return new GraphqlApiMutationResult(
            FAILED, "User '%s' disabled: check with your administrator", userName);
      }
    } else {
      rateLimit.onFailure(userName);
      return new GraphqlApiMutationResult(
          FAILED, "Sign in as '%s' failed: user or password unknown", userName);
    }
  }
}
