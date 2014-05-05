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
package org.projasource.dbimport.bindings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Oleg Kasian
 */
public class SqlQueryExecutor {

    private final BasicDataSource ds;

    public SqlQueryExecutor(final BasicDataSource ds) {
        this.ds = ds;
    }

    public String list(final String sql, final Object[] params) throws SQLException {
        return execute(sql, params, new RSCallback() {
            public String fetchResultSet(ResultSet rs) throws SQLException {
                final JSONArray result = new JSONArray();
                final List<String> fields = new ArrayList<String>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    fields.add(rs.getMetaData().getColumnName(i));
                }
                while (rs.next()) {
                    final JSONObject row = new JSONObject();
                    for (final String nm : fields) {
                        row.put(nm, rs.getString(nm));
                    }
                    result.add(row);
                }
                return result.toJSONString();
            }
        });
    }

    public String list(final String sql) throws SQLException {
        return list(sql, null);
    }

    public String unique(final String sql, final Object[] params) throws SQLException {
        return execute(sql, params, new RSCallback() {
            public String fetchResultSet(ResultSet rs) throws SQLException {
                final List<String> fields = new ArrayList<String>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    fields.add(rs.getMetaData().getColumnName(i));
                }
                if (rs.next()) {
                    final JSONObject row = new JSONObject();
                    for (final String nm : fields) {
                        row.put(nm, rs.getString(nm));
                    }
                    return row.toJSONString();
                }
                return null;
            }
        });
    }

    public String unique(final String sql) throws SQLException {
        return unique(sql, null);
    }

    public String single(final String sql, final Object[] params) throws SQLException {
        return execute(sql, params, new RSCallback() {
            public String fetchResultSet(ResultSet rs) throws SQLException {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        });
    }

    public String single(final String sql) throws SQLException {
        return single(sql, null);
    }

    private String execute(final String sql, final Object[] params, final RSCallback callback) throws SQLException {
        final Connection conn = ds.getConnection();
        try {
            final PreparedStatement ps = conn.prepareStatement(sql);
            try {
                if (params != null) {
                    for (int i = 1; i <= params.length; i++) {
                        ps.setObject(i, params[i]);
                    }
                }
                final ResultSet rs = ps.executeQuery();
                try {
                    return callback.fetchResultSet(rs);
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } finally {
            conn.close();
        }
    }

    private interface RSCallback {

        String fetchResultSet(final ResultSet rs) throws SQLException;
    }
}
