/**
 * Copyright (C) 2020 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jsonld.serialization;

import cz.cvut.kbss.jsonld.ConfigParam;
import cz.cvut.kbss.jsonld.Configuration;
import cz.cvut.kbss.jsonld.common.Configurable;
import cz.cvut.kbss.jsonld.serialization.model.JsonNode;
import cz.cvut.kbss.jsonld.serialization.traversal.ObjectGraphTraverser;

import java.util.Objects;

/**
 * Base class for all JSON-LD serializers.
 * <p>
 * The serializers will mostly differ in the form of the generated JSON. E.g. the output can be expanded, using contexts etc.
 */
public abstract class JsonLdSerializer implements Configurable {

    private final Configuration configuration;

    final JsonGenerator jsonGenerator;

    final ValueSerializers serializers = new CommonValueSerializers();

    protected JsonLdSerializer(JsonGenerator jsonGenerator) {
        this.jsonGenerator = Objects.requireNonNull(jsonGenerator);
        this.configuration = new Configuration();
    }

    public JsonLdSerializer(JsonGenerator jsonGenerator, Configuration configuration) {
        this.jsonGenerator = Objects.requireNonNull(jsonGenerator);
        this.configuration = Objects.requireNonNull(configuration);
    }

    @Override
    public Configuration configuration() {
        return configuration;
    }

    /**
     * Registers a custom serializer for the specified type.
     * <p>
     * If a serializer already existed for the type, it is replaced by the new one.
     *
     * @param type       Type to register the serializer for
     * @param serializer Serializer to register
     * @param <T>        Serialized type
     */
    public <T> void registerSerializer(Class<T> type, ValueSerializer<T> serializer) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(serializer);
        serializers.registerSerializer(type, serializer);
    }

    /**
     * Serializes object graph with the specified root.
     * <p>
     * The serialization builds a JSON-LD tree model and then writes it using a {@link JsonGenerator}, which was passed to this instance in
     * constructor.
     *
     * @param root Object graph root
     */
    public void serialize(Object root) {
        Objects.requireNonNull(root);
        final ObjectGraphTraverser traverser = new ObjectGraphTraverser();
        traverser.setRequireId(configuration.is(ConfigParam.REQUIRE_ID));
        final JsonNode jsonRoot = buildJsonTree(root, traverser);
        jsonRoot.write(jsonGenerator);
    }

    /**
     * Builds the JSON-LD tree model.
     *
     * @param root           Object graph root
     * @param graphTraverser Instance capable of traversing the object graph from the specified root
     * @return {@link JsonNode} corresponding to the JSON-LD's tree root
     */
    protected abstract JsonNode buildJsonTree(Object root, ObjectGraphTraverser graphTraverser);

    public static JsonLdSerializer createCompactedJsonLdSerializer(JsonGenerator jsonWriter) {
        return new CompactedJsonLdSerializer(jsonWriter);
    }

    public static JsonLdSerializer createCompactedJsonLdSerializer(JsonGenerator jsonWriter, Configuration configuration) {
        return new CompactedJsonLdSerializer(jsonWriter, configuration);
    }
}
