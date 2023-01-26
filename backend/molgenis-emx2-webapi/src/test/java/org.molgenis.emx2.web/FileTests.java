package org.molgenis.emx2.web;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTests {

  private static final Logger logger = LoggerFactory.getLogger(FileTests.class);

  @Test
  public void testFile() throws IOException, InvalidFormatException {
    logger.info("Hello test");
    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("pet.xlsx").getFile());
    List<String> rows = ExelTestUtils.readExcelSheet(file);
    logger.info(rows.get(0));
  }
}
