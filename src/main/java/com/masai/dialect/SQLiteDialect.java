package com.masai.dialect;

import java.sql.Types;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.dialect.unique.DefaultUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

/**
 * Custom minimal SQLite Dialect for Hibernate 5.x (since native SQLiteDialect is HB6+ only).
 * Provides basic type mapping for the e-commerce project.
 * Suppresses unsupported DDL (ALTER TABLE DROP/ADD CONSTRAINT) that SQLite cannot execute.
 */
public class SQLiteDialect extends Dialect {

    /**
     * SQLite-specific UniqueDelegate that:
     *  - Handles unique constraints inline in CREATE TABLE (via column definition)
     *  - Returns empty strings for all ALTER TABLE ADD/DROP CONSTRAINT calls
     *    so Hibernate never emits those unsupported statements.
     */
    private final UniqueDelegate uniqueDelegate = new DefaultUniqueDelegate(this) {
        @Override
        public String getColumnDefinitionUniquenessFragment(Column column) {
            // Inline UNIQUE on single-column definitions — SQLite supports this.
            return " unique";
        }

        @Override
        public String getTableCreationUniqueConstraintsFragment(Table table) {
            // Multi-column unique constraints declared in CREATE TABLE — SQLite supports this.
            return super.getTableCreationUniqueConstraintsFragment(table);
        }

        @Override
        public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
            // SQLite does NOT support ALTER TABLE … ADD CONSTRAINT … UNIQUE.
            // Returning null tells Hibernate's SchemaCreatorImpl to skip this statement entirely.
            return null;
        }

        @Override
        public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata) {
            // SQLite does NOT support ALTER TABLE … DROP CONSTRAINT.
            return null;
        }
    };

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
    public UniqueDelegate getUniqueDelegate() {
        return uniqueDelegate;
    }

    @Override
    public boolean supportsUnique() {
        return true;
    }

    @Override
    public boolean supportsUniqueConstraintInCreateAlterTable() {
        return false;  // SQLite cannot add unique constraints via ALTER TABLE
    }

    @Override
    public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKey, String referencedTable, String[] primaryKey, boolean referencesPrimaryKey) {
        // SQLite does not support ALTER TABLE … ADD CONSTRAINT … FOREIGN KEY.
        // Return null so Hibernate skips the entire ALTER TABLE statement.
        return null;
    }

    @Override
    public String getDropForeignKeyString() {
        // SQLite does not support ALTER TABLE … DROP CONSTRAINT.
        return null;
    }

    @Override
    public String getAddPrimaryKeyConstraintString(String constraintName) {
        return "";
    }

    @Override
    public boolean dropConstraints() {
        // Tell Hibernate NOT to emit "alter table … drop constraint …" DDL for SQLite.
        return false;
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
