package org.molgenis.emx2.web;

import io.javalin.http.*;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.javalin.JavalinWebContext;

public class JavalinCustomHttpActionAdapter implements HttpActionAdapter {
  public static final JavalinCustomHttpActionAdapter INSTANCE =
      new JavalinCustomHttpActionAdapter();

  private JavalinCustomHttpActionAdapter() {}

  public Void adapt(HttpAction action, WebContext webContext) {
    CommonHelper.assertNotNull("action", action);
    CommonHelper.assertNotNull("context", webContext);
    if (!(webContext instanceof JavalinWebContext)) {
      throw new RuntimeException(
          "not a Javalin web context, but " + webContext.getClass().getName());
    } else {
      JavalinWebContext context = (JavalinWebContext) webContext;
      int code = action.getCode();
      if (code == 401) {
        throw new UnauthorizedResponse();
      } else if (code == 403) {
        throw new ForbiddenResponse();
      } else if (code == 400) {
        throw new BadRequestResponse();
      } else if (action instanceof WithContentAction) {
        context.getJavalinCtx().status(action.getCode());
        context.getJavalinCtx().result(((WithContentAction) action).getContent());
        return null;
      } else if (action instanceof WithLocationAction) {
        return null;
      } else {
        context.getJavalinCtx().status(action.getCode());
        return null;
      }
    }
  }
}
