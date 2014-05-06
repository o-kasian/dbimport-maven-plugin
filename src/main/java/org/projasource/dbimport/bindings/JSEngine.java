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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *
 * @author Oleg Kasian
 */
public class JSEngine {

    final ScriptEngine js;
    final Bindings bindings;

    public JSEngine() throws ScriptException, IOException {
        js = new ScriptEngineManager().getEngineByName("javascript");
        bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
        bindDefaultEnv();
        loadDefaultScripts();
    }

    public void importResource(final String resource) throws IOException, ScriptException {
        js.eval(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(resource)), bindings);
    }

    public void importURL(final String urlSpec) throws IOException, ScriptException {
        final URL url = new URL(urlSpec);
        final URLConnection conn = url.openConnection();
        js.eval(new InputStreamReader(conn.getInputStream()), bindings);
    }

    public void bind(final String name, final Object o) {
        bindings.put(name, o);
    }

    public Object eval(final String script) throws ScriptException {
        return js.eval(script, bindings);
    }

    private void bindDefaultEnv() throws IOException {
        bind("stdout", System.out);
        bind("stderr", System.err);
        bind("env", System.getProperties());
        bind("jSEngine", this);
    }

    private void loadDefaultScripts() throws IOException, ScriptException {
        importResource("mock.js");
        importResource("oquery.js");
        importResource("application.js");
    }

}
