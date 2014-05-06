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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.projasource.dbimport.bindings.SqlQueryExecutor;
import org.projasource.dbimport.bindings.JSEngine;
import org.projasource.dbimport.config.ImportConfiguration;
import org.projasource.dbimport.config.ImportConfiguration.Table;

/**
 *
 * @author Oleg Kasian
 */
@Mojo(
        name = "import",
        defaultPhase = LifecyclePhase.INSTALL,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        aggregator = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class DataImportMojo extends DBImportMojo {

    @Parameter(property = "storage")
    private String storage;

    @Parameter(property = "importDescriptor", required = true)
    private String importDescriptor;

    private JSEngine jSEngine;
    private ImportConfiguration config;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            //Init everything
            initDataSource();
            jSEngine = new JSEngine();
            jSEngine.bind("sql", new SqlQueryExecutor(getManagedConnection()));
            jSEngine.bind("project", getPluginContext().get("project"));
            config = ImportConfiguration.parse(importDescriptor);

            //include dbStore if exists
            if (storage != null) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtil.copy(new FileInputStream(storage), baos);

                jSEngine.bind("dbJSON", baos.toString());
                jSEngine.eval("var db = JSON.parse(dbJSON);");
            }

            //execute preconditions
            for (final String s : config.getPreConditions()) {
                jSEngine.eval(s);
            }
            
            //do import
            for (final Table t : config.getTables()) {
                jSEngine.bind("tableName", t.getName());
                final Number iterator = (Number) jSEngine.eval(t.getIterator());
                System.out.println("Processing " + iterator + " rows for table: " + t.getName());
                for (int i = 0; i < iterator.intValue(); i++) {
                    jSEngine.bind("iterator", i);
                    final Map<String, Object> row = new LinkedHashMap<String, Object>();
                    for (final Entry<String, String> e : t.entrySet()) {
                        jSEngine.bind("fieldName", e.getKey());
                        final Object fVal = jSEngine.eval(e.getValue());
                        row.put(e.getKey(), fVal);
                    }
                    importRow(t.getName(), row);
                }
            }

            //execute postconditions
            for (final String s : config.getPostConditions()) {
                jSEngine.eval(s);
            }

            //commit transaction
            if (transactionExists()) {
                commit();
            }
        } catch (final Exception e) {
            if (transactionExists()) {
                rollback();
            }
            throw new MojoExecutionException("Execution failed", e);
        } finally {
            if (transactionExists()) {
                close();
            }
        }
    }

    protected void importRow(final String table, final Map<String, Object> fields) throws SQLException {
        final StringWriter ps = new StringWriter();
        ps.append("insert into ");
        if (schema != null) {
            ps.append(schema).append(".");
        }
        ps.append(table).append(" (").append(StringUtils.join(fields.keySet().iterator(), ", ")).append(") ");
        ps.append(" values (");
        final String rep = StringUtils.repeat("?, ", fields.keySet().size());
        ps.append(rep.substring(0, rep.length() - 2));
        ps.append(")");
        final PreparedStatement st = getManagedConnection().prepareStatement(ps.toString());
        for (int i = 1; i <= fields.size(); i++) {
            st.setObject(i, new ArrayList(fields.values()).get(i - 1));
        }
        st.execute();
    }
}
