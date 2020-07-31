// package org.molgenis.emx2.sql;
//
// import org.jooq.*;
// import org.jooq.Table;
// import org.jooq.conf.ParamType;
// import org.jooq.exception.DataAccessException;
// import org.jooq.impl.DSL;
// import org.jooq.util.postgres.PostgresDSL;
// import org.molgenis.emx2.*;
// import org.molgenis.emx2.Row;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// import java.sql.SQLException;
// import java.util.ArrayList;
// import java.util.Collection;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;
//
// import static org.jooq.impl.DSL.*;
// import static org.jooq.impl.DSL.name;
// import static org.molgenis.emx2.ColumnType.*;
// import static org.molgenis.emx2.Order.ASC;
// import static org.molgenis.emx2.sql.SqlColumnExecutor.getJoinTableName;
// import static org.molgenis.emx2.sql.SqlQueryFilterUtils.*;
// import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.searchColumnName;
//
// public class SqlQueryGraphExecutor extends QueryBean {}
