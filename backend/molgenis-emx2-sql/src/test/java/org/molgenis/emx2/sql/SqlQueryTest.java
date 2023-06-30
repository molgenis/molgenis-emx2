package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.molgenis.emx2.Order.ASC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.SelectJoinStep;
import org.jooq.SelectSeekStep1;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.TableLike;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;

class SqlQueryTest {
  @Test
  void orderByTest() {

    SqlSchemaMetadata schemaMock = Mockito.mock(SqlSchemaMetadata.class);
    SqlTableMetadata tableMock = Mockito.mock(SqlTableMetadata.class);
    when(schemaMock.getTableMetadata("name")).thenReturn(tableMock);

    Column column = new Column("name");
    when(tableMock.getColumn("name")).thenReturn(column);
    Table<org.jooq.Record> jooqTableMock = Mockito.mock(Table.class);

    when(tableMock.getJooqTable()).thenReturn(jooqTableMock);
    when(jooqTableMock.as(anyString())).thenReturn(null);

    DSLContext jooqMock = Mockito.mock(DSLContext.class);

    when(tableMock.getJooq()).thenReturn(jooqMock);

    SelectSelectStep recordMock = Mockito.mock(SelectSelectStep.class);
    when(jooqMock.select((Collection<? extends SelectFieldOrAsterisk>) any()))
        .thenReturn(recordMock);
    SelectJoinStep<org.jooq.Record> fromMock = Mockito.mock(SelectJoinStep.class);

    when(recordMock.from((TableLike<?>) any())).thenReturn(fromMock);

    SelectConditionStep<Record> whereMock = Mockito.mock(SelectConditionStep.class);
    when(fromMock.where((Condition) any())).thenReturn(whereMock);

    SelectSeekStep1<Record, Object> resultQueryMock =
        Mockito.mock(SelectSeekStep1.class, withSettings().extraInterfaces(SelectJoinStep.class));
    when(whereMock.orderBy((OrderField<Object>) any())).thenReturn(resultQueryMock);

    Result<Record> fetchResult = Mockito.mock(Result.class);
    when(resultQueryMock.fetch()).thenReturn(fetchResult);

    when(fetchResult.iterator()).thenReturn(new ArrayList<Record>().iterator());

    String flied = "name";
    SqlQuery sqlQuery = new SqlQuery(schemaMock, flied);
    sqlQuery.orderBy("name", ASC);
    final List<Row> result = sqlQuery.retrieveRows();
    assertEquals(Collections.emptyList(), result);
  }
}
