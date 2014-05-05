package org.projasource.dimport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author oleg
 */
@Mojo(
        name = "import",
        defaultPhase = LifecyclePhase.INSTALL,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresDependencyCollection = ResolutionScope.COMPILE,
        aggregator = true)
@Execute(phase = LifecyclePhase.PACKAGE)
public class DataImportMojo extends AbstractMojo {

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

    @Parameter(property = "storage", required = true)
    private String storage;

    @Parameter(property = "importDescriptor", required = true)
    private String importDescriptor;

    private BasicDataSource ds;
    private Connection conn;
    private File store;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initStorage();
            initDataSource();
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            final ScriptEngine js = new ScriptEngineManager().getEngineByName("javascript");
            final Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("stdout", System.out);
            bindings.put("stderr", System.err);
            bindings.put("sql", new SqlQueryExecutor(ds));
            final InputStream is = this.getClass().getClassLoader().getResourceAsStream("oquery.js");
            js.eval(new InputStreamReader(is), bindings);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtil.copy(new FileInputStream(store), baos);
            bindings.put("dbJSON", baos.toString());
            js.eval("var db = JSON.parse(dbJSON);");
            //Iterate
            final JSONParser parser = new JSONParser();
            final ContainerFactory orderedKeyFactory = new ContainerFactory() {

                public Map createObjectContainer() {
                  return new LinkedHashMap();
                }

                public List creatArrayContainer() {
                    return new LinkedList();
                }

            };
            final LinkedHashMap object = (LinkedHashMap) parser.parse(new FileReader(importDescriptor), orderedKeyFactory);
            for (final Object o : object.keySet()) {
                final String table = (String) o;
                final LinkedHashMap tImport = (LinkedHashMap) object.get(table);
                final Number it = (Number) js.eval(tImport.get("iterator").toString());
                final LinkedHashMap fields = (LinkedHashMap) tImport.get("fields");
                for (int i = 0; i < it.intValue(); i++) {
                    final Map<String, Object> row = new LinkedHashMap<String, Object>();
                    for (final Object o1 : fields.keySet()) {
                        final String fieldName = (String) o1;
                        bindings.put("iterator", i);
                        bindings.put("tableName", table);
                        bindings.put("fieldName", fieldName);
                        final Object fVal = js.eval(fields.get(fieldName).toString());
                        row.put(fieldName, fVal);
                    }
                    importRow(table, row);
                }
            }
            conn.commit();
        } catch (final Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new MojoExecutionException("Failed to rollback", ex);
                }
            }
            throw new MojoExecutionException("Execution failed", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    throw new MojoExecutionException("Failed to close connection", ex);
                }
            }
        }
    }

    protected void initStorage() throws IOException {
        store = new File(storage);
        if (!store.exists()) {
            store.createNewFile();
        }
    }

    protected void initDataSource() {
        ds = new BasicDataSource();
        ds.setUrl(url);
        ds.setDriverClassName(driverClass);
        ds.setUsername(user);
        ds.setPassword(password);
    }

    protected void importRow(final String table, final Map<String, Object> fields) throws SQLException {
        final StringWriter ps = new StringWriter();
        ps.append("inert into ");
        if (schema != null) {
            ps.append(schema).append(".");
        }
        ps.append(table).append(" (").append(StringUtils.join(fields.keySet().iterator(), ", ")).append(") ");
        ps.append(" values (");
        final String rep = StringUtils.repeat("?, ", fields.keySet().size());
        ps.append(rep.substring(0, rep.length() - 2));
        ps.append(")");
        final PreparedStatement st = conn.prepareStatement(ps.toString());
        for (int i = 1; i <= fields.size(); i++) {
            st.setObject(i, ((List) fields.values()).get(i - 1));
        }
    }
}
