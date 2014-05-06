/*
 * The MIT License (MIT)
 * Copyright (c) 2014 projasource.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.projasource.dbimport.mojo;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * @author oleg
 */
public abstract class DBImportMojo extends AbstractMojo {

    @Parameter(property = "driverClass", required = true)
    protected String driverClass;

    @Parameter(property = "url", required = true)
    protected String url;

    @Parameter(property = "user", required = true)
    protected String user;

    @Parameter(property = "password", required = true)
    protected String password;

    @Parameter(property = "schema")
    protected String schema;

    private BasicDataSource ds;
    private Connection conn;

    protected void initDataSource() {
        ds = new BasicDataSource();
        ds.setUrl(url);
        ds.setDriverClassName(driverClass);
        ds.setUsername(user);
        ds.setPassword(password);
    }

    protected BasicDataSource getDataSource() {
        if (ds == null) {
            throw new IllegalStateException("DataSource is not initialized");
        }
        return ds;
    }

    protected Connection getManagedConnection() {
        try {
            if (conn == null) {
                conn = ds.getConnection();
                conn.setAutoCommit(false);
            }
            return conn;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not obtain connection", e);
        }
    }

    protected void rollback() {
        try {
            if (conn == null) {
                throw new IllegalStateException("No transaction is opened");
            }
            conn.rollback();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void commit() {
        try {
            if (conn == null) {
                throw new IllegalStateException("No transaction is opened");
            }
            conn.commit();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void close() {
        try {
            if (conn == null) {
                throw new IllegalStateException("No transaction is opened");
            }
            conn.close();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected boolean transactionExists() {
        return conn != null;
    }
}
