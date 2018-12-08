package org.molgenis.sql.psql;

import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;
import org.molgenis.sql.SqlType;

import java.sql.Types;

public class TypeUtils {

    private TypeUtils() {
        //to hide the public constructor
    }

    public static DataType typeOf(SqlType sqlType) {
        switch (sqlType) {
            case UUID:
                return SQLDataType.UUID;
            case STRING:
                return SQLDataType.VARCHAR(255);
            case INT:
                return SQLDataType.INTEGER;
            case BOOL:
                return SQLDataType.BOOLEAN;
            case DECIMAL:
                return SQLDataType.DOUBLE;
            case TEXT:
                return SQLDataType.LONGVARCHAR;
            case DATE:
               return SQLDataType.DATE;
            case DATETIME:
                return SQLDataType.TIMESTAMPWITHTIMEZONE;
            case REF:
                return SQLDataType.UUID;
            default:
                throw new IllegalArgumentException("addColumn(name,type) : unsupported type " + sqlType);
        }
    }

    public static SqlType getSqlType(Field f) {
        switch(f.getDataType().getSQLType()) {
            case 1111:
                //here we cannot see if it is a REF or UUID key so that needs correcting elsewhere
                return SqlType.UUID;
            case Types.VARCHAR: return SqlType.STRING;
            case Types.BOOLEAN: return SqlType.BOOL;
            case Types.INTEGER: return SqlType.INT;
            case Types.DOUBLE: return SqlType.DECIMAL;
            case Types.LONGVARCHAR: return SqlType.TEXT;
            case Types.DATE: return SqlType.DATE;
            case Types.TIMESTAMP_WITH_TIMEZONE: return SqlType.DATETIME;
            default:
                throw new UnsupportedOperationException("Unsupported SQL type found:" + f.getDataType().getSQLType());
        }
    }
}
