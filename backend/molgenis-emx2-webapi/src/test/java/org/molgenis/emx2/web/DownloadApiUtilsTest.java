package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.web.Constants.INCLUDE_SYSTEM_COLUMNS;

import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DownloadApiUtilsTest {

  @Test
  void includeSystemColumnsTest() {
    Context ctx = Mockito.mock(Context.class);
    when(ctx.queryParam(INCLUDE_SYSTEM_COLUMNS)).thenReturn("true");
    assertTrue(DownloadApiUtils.includeSystemColumns(ctx));
  }
}
