package org.projasource.dimport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author oleg
 */
@Mojo(
        name = "export",
        defaultPhase = LifecyclePhase.INSTALL,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        aggregator = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class DataExportMojo extends AbstractMojo {

    @Parameter(property = "driverClass", required = true)
    private String driverClass;

    @Parameter(property = "url", required = true)
    private String url;

    @Parameter(property = "user", required = true)
    private String user;

    @Parameter(property = "password", required = true)
    private String password;

    @Parameter(property = "schema")
    private String schema;

    @Parameter(property = "tableNames", required = true)
    private List<String> tableNames;

    @Parameter(property = "storage", required = true)
    private String storage;

    private BasicDataSource ds;
    private File store;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initDataSource();
            initStorage();
            final JSONObject export = new JSONObject();
            for (final String table : tableNames) {
                export.put(table, readTable(table));
            }
            final Writer out = new FileWriter(store);
            export.writeJSONString(out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    protected void initDataSource() {
        ds = new BasicDataSource();
        ds.setUrl(url);
        ds.setDriverClassName(driverClass);
        ds.setUsername(user);
        ds.setPassword(password);
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
        final Connection c = ds.getConnection();
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
