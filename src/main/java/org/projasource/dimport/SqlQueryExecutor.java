package org.projasource.dimport;

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
 * @author oleg
 */
public class SqlQueryExecutor {

    private final BasicDataSource ds;

    public SqlQueryExecutor(final BasicDataSource ds) {
        this.ds = ds;
    }

    public String list(final String sql, final Object[] params) throws SQLException {
        final JSONArray result = new JSONArray();
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
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
            return result.toJSONString();
        } finally {
            conn.close();
        }
    }

    public String list(final String sql) throws SQLException {
        return list(sql, null);
    }

    public String unique(final String sql, final Object[] params) throws SQLException {
        final JSONObject o = new JSONObject();
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
                } finally {
                    rs.close();
                }
            } finally {
                ps.close();
            }
        } finally {
            conn.close();
        }
        return o.toJSONString();
    }

    public String unique(final String sql) throws SQLException {
        return unique(sql, null);
    }

    public String single(final String sql, final Object[] params) throws SQLException {
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
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                    return null;
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

    public String single(final String sql) throws SQLException {
        return single(sql, null);
    }
}
