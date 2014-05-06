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
package org.projasource.dbimport.config;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.projasource.dbimport.util.JSONContainerFactory;

/**
 *
 * @author Oleg Kasian
 */
public class ImportConfiguration {

    private final List<String> preConditions = new LinkedList<String>();
    private final List<String> postConditions = new LinkedList<String>();
    private final List<Table> tables = new LinkedList<Table>();

    private ImportConfiguration() {
    }

    public List<String> getPreConditions() {
        return preConditions;
    }

    public List<String> getPostConditions() {
        return postConditions;
    }

    public List<Table> getTables() {
        return tables;
    }

    public static ImportConfiguration parse(final String path) throws IOException, ParseException {
        final ImportConfiguration config = new ImportConfiguration();
        final JSONParser parser = new JSONParser();
        final FileReader f = new FileReader(path);
        final LinkedHashMap map = (LinkedHashMap) parser.parse(f, new JSONContainerFactory());
        if (map.containsKey("preconditions")) {
            config.preConditions.addAll((List) map.get("preconditions"));
        }
        if (map.containsKey("postconditions")) {
            config.postConditions.addAll((List) map.get("postconditions"));
        }
        if (map.containsKey("tables")) {
            for (final Object o : ((Map) map.get("tables")).entrySet()) {
                final Entry<String, Map> e = (Entry) o;
                final Table t = new Table();
                t.name = e.getKey();
                t.iterator = (String) e.getValue().get("iterator");
                t.putAll((Map) e.getValue().get("fields"));
                config.tables.add(t);
            }
        }
        f.close();
        if (!config.isValid()) {
            throw new IllegalStateException("Configuration is invalid!");
        }
        return config;
    }

    protected boolean isValid() {
        return !tables.isEmpty();
    }

    public static class Table extends LinkedHashMap<String, String> {

        private String name;
        private String iterator;

        public String getName() {
            return name;
        }

        public String getIterator() {
            return iterator;
        }

        public List getValues() {
            return new ArrayList(this.values());
        }

        public List getFieldNames() {
            return new ArrayList(this.keySet());
        }

    }
}
