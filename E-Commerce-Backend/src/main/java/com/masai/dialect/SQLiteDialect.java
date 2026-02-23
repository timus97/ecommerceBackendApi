package com.masai.dialect;

import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * Custom minimal SQLite Dialect for Hibernate 5.x (since native SQLiteDialect is HB6+ only).
 * Provides basic type mapping for the e-commerce project.
 * This should suffice for JPA operations with @GeneratedValue.AUTO etc.
 */
public class SQLiteDialect extends Dialect {

    public SQLiteDialect() {
        super();
        registerColumnType(Types.BIT, "integer");
        registerColumnType(Types.TINYINT, "tinyint");
        registerColumnType(Types.SMALLINT, "smallint");
        registerColumnType(Types.INTEGER, "integer");
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.FLOAT, "float");
        registerColumnType(Types.REAL, "real");
        registerColumnType(Types.DOUBLE, "double");
        registerColumnType(Types.NUMERIC, "numeric");
        registerColumnType(Types.DECIMAL, "decimal");
        registerColumnType(Types.CHAR, "char");
        registerColumnType(Types.VARCHAR, "varchar");
        registerColumnType(Types.LONGVARCHAR, "longvarchar");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "timestamp");
        registerColumnType(Types.BINARY, "blob");
        registerColumnType(Types.VARBINARY, "blob");
        registerColumnType(Types.LONGVARBINARY, "blob");
        registerColumnType(Types.BLOB, "blob");
        registerColumnType(Types.CLOB, "clob");
        registerColumnType(Types.BOOLEAN, "integer");
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl();
    }

    public boolean supportsIdentityColumns() {
        return true;
    }

    public boolean hasDataTypeInIdentityColumn() {
        return false;
    }

    public String getIdentitySelectString(String table, String column, int identityColumnType) {
        return "select last_insert_rowid()";
    }

    public String getIdentityColumnString(int type) {
        return "integer";
    }

    @Override
    public boolean supportsUnique() {
        return true;
    }

    @Override
    public boolean supportsUniqueConstraintInCreateAlterTable() {
        return false;  // SQLite limitations
    }

    @Override
    public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKey, String referencedTable, String[] primaryKey, boolean referencesPrimaryKey) {
        return "";  // SQLite FK limited by default
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }

    @Override
    public String getDropTableString(String tableName) {
        return "drop table if exists " + tableName;
    }

    // Support for ALTER TABLE ADD COLUMN (fixes UnsupportedOperation in schema migration)
    public String getAddColumnString(String columnName) {
        // SQLite syntax: ADD COLUMN col type
        return "add column " + columnName;
    }
}
