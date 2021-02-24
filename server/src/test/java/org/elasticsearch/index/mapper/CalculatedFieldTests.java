/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.index.IndexableField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.runtimefields.mapper.DoubleFieldScript;
import org.elasticsearch.runtimefields.mapper.LongFieldScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CalculatedFieldTests extends MapperServiceTestCase {

    @Override
    protected Collection<? extends Plugin> getPlugins() {
        return Set.of(new TestScriptPlugin());
    }

    public void testCalculatedFieldLength() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("message").field("type", "text").endObject();
            b.startObject("message_length");
            b.field("type", "long");
            b.field("script", "length");
            b.endObject();
        }));

        ParsedDocument doc = mapper.parse(source(b -> b.field("message", "this is some text")));
        IndexableField[] lengthFields = doc.rootDoc().getFields("message_length");
        assertEquals(2, lengthFields.length);
        assertEquals("LongPoint <message_length:17>", lengthFields[0].toString());
        assertEquals("docValuesType=SORTED_NUMERIC<message_length:17>", lengthFields[1].toString());
    }

    public void testDocAccess() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("long_field").field("type", "long").endObject();
            b.startObject("long_field_plus_two");
            b.field("type", "long");
            b.field("script", "plus_two");
            b.endObject();
        }));

        ParsedDocument doc = mapper.parse(source(b -> b.field("long_field", 4)));
        assertEquals(doc.rootDoc().getField("long_field_plus_two").numericValue(), 6L);
    }

    public void testDoublesAccess() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("double_field").field("type", "double").endObject();
            b.startObject("double_field_plus_two");
            b.field("type", "double");
            b.field("script", "plus_two");
            b.endObject();
        }));

        ParsedDocument doc = mapper.parse(source(b -> b.field("double_field", 4.5)));
        assertEquals(doc.rootDoc().getField("double_field_plus_two").numericValue(), 6.5);
    }

    public void testSerialization() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("message").field("type", "text").endObject();
            b.startObject("message_length");
            b.field("type", "long");
            b.field("script", "length");
            b.endObject();
        }));
        assertEquals(
            "{\"_doc\":{\"properties\":{\"message\":{\"type\":\"text\"}," +
                "\"message_length\":{\"type\":\"long\",\"script\":{\"source\":\"length\",\"lang\":\"painless\"}}}}}",
            Strings.toString(mapper.mapping()));
    }

    public static class TestScriptPlugin extends Plugin implements ScriptPlugin {

        @Override
        public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
            return new ScriptEngine() {
                @Override
                public String getType() {
                    return "painless";
                }

                @Override
                @SuppressWarnings("unchecked")
                public <FactoryType> FactoryType compile(
                    String name,
                    String code,
                    ScriptContext<FactoryType> context,
                    Map<String, String> params
                ) {
                    if (context.factoryClazz == LongFieldScript.Factory.class) {
                        switch (code) {
                            case "length":
                                return (FactoryType) (LongFieldScript.Factory) (n, p, lookup) -> ctx -> new LongFieldScript(n, p, lookup, ctx) {
                                    @Override
                                    public void execute() {
                                        for (Object v : extractFromSource("message")) {
                                            emit(Objects.toString(v).length());
                                        }
                                    }
                                };
                            case "plus_two":
                                return (FactoryType) (LongFieldScript.Factory) (n, p, lookup) -> ctx -> new LongFieldScript(n, p, lookup, ctx) {
                                    @Override
                                    public void execute() {
                                        long input = (long) getDoc().get("long_field").get(0);
                                        emit(input + 2);
                                    }
                                };
                        }
                    }
                    if (context.factoryClazz == DoubleFieldScript.Factory.class) {
                        if (code.equals("plus_two")) {
                            return (FactoryType) (DoubleFieldScript.Factory) (n, p, lookup) -> ctx -> new DoubleFieldScript(n, p, lookup, ctx) {
                                @Override
                                public void execute() {
                                    double input = (double) getDoc().get("double_field").get(0);
                                    emit(input + 2);
                                }
                            };
                        }
                    }
                    throw new IllegalArgumentException("Unknown factory type " + context.factoryClazz + " for code " + code);
                }

                @Override
                public Set<ScriptContext<?>> getSupportedContexts() {
                    return Set.of(LongFieldScript.CONTEXT, DoubleFieldScript.CONTEXT);
                }
            };
        }
    }

}
