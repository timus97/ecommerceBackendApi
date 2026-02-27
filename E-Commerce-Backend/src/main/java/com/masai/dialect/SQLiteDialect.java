package com.masai.dialect;

import org.hibernate.dialect.Dialect;

/**
 * Custom SQLite Dialect for Hibernate 6.x.
 * Note: Hibernate 6.x includes native SQLite support, so this is a minimal wrapper.
 */
public class SQLiteDialect extends Dialect {
    
    public SQLiteDialect() {
        super();
    }
}
