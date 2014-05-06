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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.projasource.dbimport.bindings.SqlQueryExecutor;

/**
 *
 * @author Oleg Kasian
 */
@Mojo(
        name = "export",
        defaultPhase = LifecyclePhase.INSTALL,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        aggregator = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class DataExportMojo extends DBImportMojo {

    @Parameter(property = "tableNames", required = true)
    protected List<String> tableNames;

    @Parameter(property = "extraConditions")
    protected Map<String, String> extraConditions;

    @Parameter(property = "storage", required = true)
    protected String storage;

    private File store;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initDataSource();
            initStorage();
            if (extraConditions != null) {
                tableNames.removeAll(extraConditions.keySet());
            }
            final JSONObject export = new JSONObject();
            final SqlQueryExecutor exec = new SqlQueryExecutor(getManagedConnection());
            String select = "SELECT * FROM ";
            if (schema != null) {
                select += schema + ".";
            }
            if (extraConditions == null) {
                extraConditions = new HashMap<String, String>();
            }
            for (final String table : tableNames) {
                extraConditions.put(table, select + table);
            }
            for (final Entry<String, String> e : extraConditions.entrySet()) {
                final JSONArray t = new JSONArray();
                exec.execute(e.getValue(), null, new SqlQueryExecutor.RSCallback() {

                    public String fetchResultSet(ResultSet rs) throws SQLException {
                        final List<String> fields = new ArrayList<String>();
                        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                            fields.add(rs.getMetaData().getColumnName(i));
                        }
                        while (rs.next()) {
                            final JSONObject row = new JSONObject();
                            for (final String field : fields) {
                                row.put(field, rs.getString(field));
                            }
                            t.add(row);
                        }
                        return null;
                    }

                });
                export.put(e.getKey(), t);
            }
            final Writer out = new FileWriter(store);
            export.writeJSONString(out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            e.printStackTrace();
            throw new MojoFailureException("Could not execute due to error", e);
        }
    }

    protected void initStorage() throws IOException {
        store = new File(storage);
        if (!store.exists()) {
            store.createNewFile();
        }
    }

    protected JSONArray readTable(final String name) throws SQLException {
        String select = "SELECT * FROM ";
        if (schema != null) {
            select += schema + ".";
        }
        final Connection c = getDataSource().getConnection();
        try {
            final PreparedStatement ps = c.prepareStatement(select + name);
            final JSONArray table = new JSONArray();
            try {
                final ResultSet rs = ps.executeQuery();
                final List<String> fields = new ArrayList<String>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    fields.add(rs.getMetaData().getColumnName(i));
                }
                while (rs.next()) {
                    final JSONObject row = new JSONObject();
                    for (final String field : fields) {
                        row.put(field, rs.getString(field));
                    }
                    table.add(row);
                }
                rs.close();
            } finally {
                ps.close();
            }
            return table;
        } finally {
            c.close();
        }
    }

}
