[1mdiff --git a/backend/build.gradle b/backend/build.gradle[m
[1mindex 84baff3d..184d6567 100644[m
[1m--- a/backend/build.gradle[m
[1m+++ b/backend/build.gradle[m
[36m@@ -54,6 +54,7 @@[m [msubprojects {[m
 [m
         //also used outside test[m
         implementation 'junit:junit:4.13.2'[m
[32m+[m[32m        implementation 'org.mockito:mockito-core:4.3.1'[m
     }[m
 [m
     jacocoTestReport {[m
[1mdiff --git a/backend/molgenis-emx2-exampledata/src/main/java/org/molgenis/emx2/examples/PetStoreExample.java b/backend/molgenis-emx2-exampledata/src/main/java/org/molgenis/emx2/examples/PetStoreExample.java[m
[1mindex 075e4f9d..ff9873b7 100644[m
[1m--- a/backend/molgenis-emx2-exampledata/src/main/java/org/molgenis/emx2/examples/PetStoreExample.java[m
[1m+++ b/backend/molgenis-emx2-exampledata/src/main/java/org/molgenis/emx2/examples/PetStoreExample.java[m
[36m@@ -1,13 +1,13 @@[m
 package org.molgenis.emx2.examples;[m
 [m
[31m-import org.molgenis.emx2.Row;[m
[31m-import org.molgenis.emx2.Schema;[m
[31m-import org.molgenis.emx2.SchemaMetadata;[m
[31m-[m
 import static org.molgenis.emx2.Column.column;[m
 import static org.molgenis.emx2.ColumnType.*;[m
 import static org.molgenis.emx2.TableMetadata.table;[m
 [m
[32m+[m[32mimport org.molgenis.emx2.Row;[m
[32m+[m[32mimport org.molgenis.emx2.Schema;[m
[32m+[m[32mimport org.molgenis.emx2.SchemaMetadata;[m
[32m+[m
 public class PetStoreExample {[m
 [m
   public static final String CATEGORY = "Category";[m
[1mdiff --git a/backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/EvaluateExpressions.java b/backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/EvaluateExpressions.java[m
[1mindex aab5c8d1..6b9104ee 100644[m
[1m--- a/backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/EvaluateExpressions.java[m
[1m+++ b/backend/molgenis-emx2-sql/src/main/java/org/molgenis/emx2/sql/EvaluateExpressions.java[m
[36m@@ -1,5 +1,7 @@[m
 package org.molgenis.emx2.sql;[m
 [m
[32m+[m[32mimport java.util.*;[m
[32m+[m[32mimport java.util.stream.Collectors;[m
 import org.molgenis.emx2.Column;[m
 import org.molgenis.emx2.MolgenisException;[m
 import org.molgenis.emx2.Row;[m
[36m@@ -7,12 +9,9 @@[m [mimport org.molgenis.emx2.utils.TypeUtils;[m
 import org.molgenis.expression.Expressions;[m
 import scala.util.Try;[m
 [m
[31m-import java.util.*;[m
[31m-import java.util.stream.Collectors;[m
[31m-[m
 public class EvaluateExpressions {[m
 [m
[31m-  private static final Expressions evaluator = new Expressions(10000);[m
[32m+[m[32m  private static Expressions evaluator = new Expressions(10000);[m
 [m
   /**[m
    * validate if the expression is valid, given the metadata. Typically, done at beginning of a[m
[36m@@ -23,30 +22,38 @@[m [mpublic class EvaluateExpressions {[m
     Set<String> variableNames = evaluator.getAllVariableNames(expressionList);[m
     Set<String> columnNames = getColumnNames(columns);[m
 [m
[31m-    // check if any variable is missing in column list[m
[31m-    Set<String> missingVariables =[m
[31m-        variableNames.stream().filter(v -> !columnNames.contains(v)).collect(Collectors.toSet());[m
[32m+[m[32m    Set<String> missingVariables = getMissingVariableList(variableNames, columnNames);[m
     if (!missingVariables.isEmpty()) {[m
       throw new MolgenisException([m
           "Validation failed: columns " + missingVariables + " not provided");[m
     }[m
   }[m
 [m
[32m+[m[32m  private static List<String> getExpressionList(Collection<Column> columns) {[m
[32m+[m[32m    return columns.stream().map(Column::getValidation).filter(Objects::nonNull).toList();[m
[32m+[m[32m  }[m
[32m+[m
   private static Set<String> getColumnNames(Collection<Column> columns) {[m
     return columns.stream().map(Column::getName).collect(Collectors.toSet());[m
   }[m
 [m
[31m-  private static List<String> getExpressionList(Collection<Column> columns) {[m
[31m-    return columns.stream().map(Column::getValidation).filter(Objects::nonNull).toList();[m
[32m+[m[32m  private static Set<String> getMissingVariableList([m
[32m+[m[32m      Set<String> variableNames, Set<String> columnNames) {[m
[32m+[m[32m    return variableNames.stream()[m
[32m+[m[32m        .filter(name -> !columnNames.contains(name))[m
[32m+[m[32m        .collect(Collectors.toSet());[m
   }[m
 [m
[31m-  /** validate an expression given a row. True means it is valid. */[m
[31m-  public static boolean check(String expression, Row row) {[m
[31m-    return TypeUtils.toBool(compute(expression, row));[m
[32m+[m[32m  /**[m
[32m+[m[32m   * validate an expression given a row. True means it is valid. Why is this needed?[m
[32m+[m[32m   * calculateComputedExpression can already return booleans[m
[32m+[m[32m   */[m
[32m+[m[32m  public static boolean evaluateValidationExpression(String expression, Row row) {[m
[32m+[m[32m    return TypeUtils.toBool(calculateComputedExpression(expression, row));[m
   }[m
 [m
[31m-  /** use expression to compute value and return value of the expression */[m
[31m-  public static Object compute(String expression, Row row) {[m
[32m+[m[32m  /** use expression to compute value and return value of the expression, not used yet */[m
[32m+[m[32m  public static Object calculateComputedExpression(String expression, Row row) {[m
     Try<Object> result = evaluator.parseAndEvaluate(List.of(expression), row.getValueMap()).get(0);[m
     if (result.isFailure()) {[m
       throw new MolgenisException("Failed to execute expression: " + expression);[m
[36m@@ -55,16 +62,17 @@[m [mpublic class EvaluateExpressions {[m
   }[m
 [m
   public static void checkValidation(Map<String, Object> values, Collection<Column> columns) {[m
[31m-    for (Column c : columns) {[m
[31m-      if (c.getValidation() != null) {[m
[31m-        Try<Object> result = evaluator.parseAndEvaluate(List.of(c.getValidation()), values).get(0);[m
[32m+[m[32m    for (Column column : columns) {[m
[32m+[m[32m      if (column.getValidation() != null) {[m
[32m+[m[32m        Try<Object> result =[m
[32m+[m[32m            evaluator.parseAndEvaluate(List.of(column.getValidation()), values).get(0);[m
         if (result.isFailure()) {[m
           throw new MolgenisException([m
[31m-              String.format("Cannot execute expression: %s", c.getValidation()));[m
[32m+[m[32m              String.format("Cannot execute expression: %s", column.getValidation()));[m
         }[m
         if (!TypeUtils.toBool(result.get())) {[m
           throw new MolgenisException([m
[31m-              String.format("%s. Values provided: %s", c.getValidation(), values));[m
[32m+[m[32m              String.format("%s. Values provided: %s", column.getValidation(), values));[m
         }[m
       }[m
     }[m
[1mdiff --git a/backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/EvaluateExpressionsTest.java b/backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/EvaluateExpressionsTest.java[m
[1mindex 147fcd77..1bcbaf35 100644[m
[1m--- a/backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/EvaluateExpressionsTest.java[m
[1m+++ b/backend/molgenis-emx2-sql/src/test/java/org/molgenis/emx2/sql/EvaluateExpressionsTest.java[m
[36m@@ -1,4 +1,116 @@[m
[32m+[m[32mpackage org.molgenis.emx2.sql;[m
[32m+[m
[32m+[m[32mimport static org.mockito.Mockito.mock;[m
[32m+[m[32mimport static org.mockito.Mockito.when;[m
[32m+[m[32mimport static org.molgenis.emx2.sql.EvaluateExpressions.*;[m
[32m+[m
[32m+[m[32mimport java.util.ArrayList;[m
[32m+[m[32mimport java.util.Collection;[m
[32m+[m[32mimport java.util.HashMap;[m
[32m+[m[32mimport java.util.Map;[m
 import junit.framework.TestCase;[m
[32m+[m[32mimport org.junit.Test;[m
[32m+[m[32mimport org.junit.runner.RunWith;[m
[32m+[m[32mimport org.mockito.junit.MockitoJUnitRunner;[m
[32m+[m[32mimport org.molgenis.emx2.Column;[m
[32m+[m[32mimport org.molgenis.emx2.MolgenisException;[m
[32m+[m[32mimport org.molgenis.emx2.Row;[m
[32m+[m
[32m+[m[32m@RunWith(MockitoJUnitRunner.class)[m
 public class EvaluateExpressionsTest extends TestCase {[m
[31m-  [m
[31m-}[m
\ No newline at end of file[m
[32m+[m
[32m+[m[32m  @Test[m
[32m+[m[32m  public void testCheckValidationColumnsSuccess() {[m
[32m+[m[32m    Collection<Column> columns = new ArrayList<>();[m
[32m+[m[32m    Column column1 = new Column("columnName1");[m
[32m+[m[32m    column1.setValidation("{columnName2}");[m
[32m+[m[32m    Column column2 = new Column("columnName2");[m
[32m+[m
[32m+[m[32m    columns.add(column1);[m
[32m+[m[32m    columns.add(column2);[m
[32m+[m
[32m+[m[32m    checkValidationColumns(columns);[m
[32m+[m[32m  }[m
[32m+[m
[32m+[m[32m  @Test[m
[32m+[m[32m  public void testCheckValidationColumnsFailure() {[m
[32m+[m[32m    Collection<Column> columns = new ArrayList<>();[m
[32m+[m[32m    Column column1 = new Column("columnName1");[m
[32m+[m[32m    column1.setValidation("{columnName2}");[m
[32m+[m[32m    columns.add(column1);[m
[32m+[m[32m    try {[m
[32m+[m[32m      checkValidationColumns(columns);[m
[32m+[m[32m    } catch (MolgenisException exception) {[m
[32m+[m[32m      String expectedError = "Validation failed: columns [columnName2] not provided";[m
[32m+[m[32m      assertEquals(expectedError, exception.getMessage());[m
[32m+[m[32m    }[m
[32m+[m[32m  }[m
[32m+[m
[32m+[m[32m  @Test[m
[32m+[m[32m  public void evaluateValidationExpressionTest() {[m
[32m+[m[32m    String expression = "false || true";[m
[32m+[m[32m    Row row = new Row();[m
[32m+[m[32m    assertTrue(evaluateValidationExpression(expression, row));[m
[32m+[m[32m  }[m
[32m+[m
[32m+[m[32m  @Test[m
[32m+[m[32m  public void testCalculateComputedExpression() {[m
[32m+[m[32m    String expression = "5 + 7";[m
[32m+[m[32m    Row row = new Row();[m
[32m+[m[32m    Object outcome = calculateComputedExpression(expression, row);[m
[32m+[m[32m    assertEquals(12.0, outcome);[m
[32m+[m[32m  }[m
[32m+[m
[32m+[m[32m  @Test[m
[32m+[m[32m  public void testCalculateComputedExpressionFailure() {[m
[32m+[m[32m    String expression = "5 + YAAARGH";[m
[32m+[m[32m    Row row = new Row();[m
[32m+[m[32m    try {[m
[32m+[m[32m      calculateComputedExpression(expression, row);[m
[32m+[m
[32m+[m[32m    } catch (MolgenisException exception) {[m
[32m+[m[32m      assertEquals("Failed to execute expression: " + expression, exception.getMessage());[m
[32m+[m[32m    }[m
[32m+[m[32m  }[m
[32m+[m
[32m+[m[32m  @Test[m
[32m+[m[32m  public void testCheckValidationSuccess() {[m
[32m+[m[32m    Map<String, Object> values = new HashMap<>();[m
[32m+[m[32m    Collection<Column> columns = new ArrayList<>();[m
[32m+[m[32m    Column column = mock(Column.class);[m
[32m+[m[32m    String validation = "true && true";[m
[32m+[m[32m    when(column.getValidation()).thenReturn(validation);[m
[32m+[m[32m    columns.add(column);[m
[32m+[m[32m    checkValidation(values, columns);[m
[32m+[m[32m  }[m
[32m+[m
[32m+[m[32m  @Test[m
[32m+[m[32m  public void testCheckValidationInvalidExpression() {[m
[32m+[m[32m    Map<String, Object> values = new HashMap<>();[m
[32m+[m[32m    Collection<Column> columns = new ArrayList<>();[m
[32m+[m[32m    Column column = mock(Column.class);[m
[32m+[m[32m    String validation = "this is very invalid";[m
[32m+[m[32m    when(column.getValidation()).thenReturn(validation);[m
[32m+[m[32m    columns.add(column);[m
[32m+[m[32m    try {[m
[32m+[m[32m      checkValidation(values, columns);[m
[32m+[m[32m    } catch (MolgenisException exception) {[m
[32m+[m[32m      assertEquals("Cannot execute expression: this is very invalid", exception.getMessage());[m
[32m+[m[32m    }[m
[32m+[m[32m  }[m
[32m+[m
[32m+[m[32m  @Test[m
[32m+[m[32m  public void testCheckValidationTurnToBoolIsFalse() {[m
[32m+[m[32m    Map<String, Object> values = new HashMap<>();[m
[32m+[m[32m    Collection<Column> columns = new ArrayList<>();[m
[32m+[m[32m    Column column = mock(Column.class);[m
[32m+[m[32m    String validation = "false";[m
[32m+[m[32m    when(column.getValidation()).thenReturn(validation);[m
[32m+[m[32m    columns.add(column);[m
[32m+[m[32m    try {[m
[32m+[m[32m      checkValidation(values, columns);[m
[32m+[m[32m    } catch (MolgenisException exception) {[m
[32m+[m[32m      assertEquals("false. Values provided: {}", exception.getMessage());[m
[32m+[m[32m    }[m
[32m+[m[32m  }[m
[32m+[m[32m}[m
[1mdiff --git a/backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Column.java b/backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Column.java[m
[1mindex 602bf0a5..b429081d 100644[m
[1m--- a/backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Column.java[m
[1m+++ b/backend/molgenis-emx2/src/main/java/org/molgenis/emx2/Column.java[m
[36m@@ -70,20 +70,6 @@[m [mpublic class Column implements Comparable<Column> {[m
     this.columnName = validateName(columnName, skipValidation);[m
   }[m
 [m
[31m-  private String validateName(String columnName, boolean skipValidation) {[m
[31m-    if (!skipValidation && !columnName.matches("[a-zA-Z][a-zA-Z0-9_ ]*")) {[m
[31m-      throw new MolgenisException([m
[31m-          "Invalid column name '"[m
[31m-              + columnName[m
[31m-              + "': Column must start with a letter, followed by letters, underscores, a space or numbers, i.e. [a-zA-Z][a-zA-Z0-9_]*");[m
[31m-    }[m
[31m-    if (!skipValidation && (columnName.contains("_ ") || columnName.contains(" _"))) {[m
[31m-      throw new MolgenisException([m
[31m-          "Invalid column name '" + columnName + "': column names cannot contain '_ ' or '_ '");[m
[31m-    }[m
[31m-    return columnName.trim();[m
[31m-  }[m
[31m-[m
   public Column(TableMetadata table, String columnName) {[m
     this(columnName);[m
     this.table = table;[m
[36m@@ -102,6 +88,20 @@[m [mpublic class Column implements Comparable<Column> {[m
     return new Column(name).setType(type);[m
   }[m
 [m
[32m+[m[32m  private String validateName(String columnName, boolean skipValidation) {[m
[32m+[m[32m    if (!skipValidation && !columnName.matches("[a-zA-Z][a-zA-Z0-9_ ]*")) {[m
[32m+[m[32m      throw new MolgenisException([m
[32m+[m[32m          "Invalid column name '"[m
[32m+[m[32m              + columnName[m
[32m+[m[32m              + "': Column must start with a letter, followed by letters, underscores, a space or numbers, i.e. [a-zA-Z][a-zA-Z0-9_]*");[m
[32m+[m[32m    }[m
[32m+[m[32m    if (!skipValidation && (columnName.contains("_ ") || columnName.contains(" _"))) {[m
[32m+[m[32m      throw new MolgenisException([m
[32m+[m[32m          "Invalid column name '" + columnName + "': column names cannot contain '_ ' or '_ '");[m
[32m+[m[32m    }[m
[32m+[m[32m    return columnName.trim();[m
[32m+[m[32m  }[m
[32m+[m
   public String[] getSemantics() {[m
     return semantics;[m
   }[m
[36m@@ -150,15 +150,15 @@[m [mpublic class Column implements Comparable<Column> {[m
     return columnName;[m
   }[m
 [m
[31m-  public String getQualifiedName() {[m
[31m-    return getTableName() + "." + getName();[m
[31m-  }[m
[31m-[m
   public Column setName(String columnName) {[m
     this.columnName = columnName;[m
     return this;[m
   }[m
 [m
[32m+[m[32m  public String getQualifiedName() {[m
[32m+[m[32m    return getTableName() + "." + getName();[m
[32m+[m[32m  }[m
[32m+[m
   public ColumnType getColumnType() {[m
     return columnType;[m
   }[m
[36m@@ -585,6 +585,10 @@[m [mpublic class Column implements Comparable<Column> {[m
     return refLink;[m
   }[m
 [m
[32m+[m[32m  public void setRefLink(String refLink) {[m
[32m+[m[32m    this.refLink = refLink;[m
[32m+[m[32m  }[m
[32m+[m
   public Column getRefLinkColumn() {[m
     if (refLink != null) {[m
       return getTable().getColumn(refLink);[m
[36m@@ -592,10 +596,6 @@[m [mpublic class Column implements Comparable<Column> {[m
     return null;[m
   }[m
 [m
[31m-  public void setRefLink(String refLink) {[m
[31m-    this.refLink = refLink;[m
[31m-  }[m
[31m-[m
   public boolean isPrimaryKey() {[m
     return getKey() == 1;[m
   }[m
[1mdiff --git a/backend/molgenis-emx2/src/test/java/org/molgenis/emx2/TestTypeUtils.java b/backend/molgenis-emx2/src/test/java/org/molgenis/emx2/TestTypeUtils.java[m
[1mindex b853d22c..49b3002b 100644[m
[1m--- a/backend/molgenis-emx2/src/test/java/org/molgenis/emx2/TestTypeUtils.java[m
[1m+++ b/backend/molgenis-emx2/src/test/java/org/molgenis/emx2/TestTypeUtils.java[m
[36m@@ -1,16 +1,15 @@[m
 package org.molgenis.emx2;[m
 [m
[31m-import org.jooq.JSONB;[m
[31m-import org.junit.Test;[m
[31m-import org.molgenis.emx2.utils.TypeUtils;[m
[32m+[m[32mimport static org.junit.Assert.*;[m
 [m
 import java.time.LocalDate;[m
 import java.time.LocalDateTime;[m
 import java.util.Arrays;[m
 import java.util.List;[m
 import java.util.UUID;[m
[31m-[m
[31m-import static org.junit.Assert.*;[m
[32m+[m[32mimport org.jooq.JSONB;[m
[32m+[m[32mimport org.junit.Test;[m
[32m+[m[32mimport org.molgenis.emx2.utils.TypeUtils;[m
 [m
 public class TestTypeUtils {[m
 [m
