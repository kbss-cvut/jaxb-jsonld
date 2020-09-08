/**
 * Copyright (C) 2020 Czech Technical University in Prague
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jsonld.serialization;

import cz.cvut.kbss.jopa.model.MultilingualString;
import cz.cvut.kbss.jsonld.common.BeanAnnotationProcessor;
import cz.cvut.kbss.jsonld.serialization.model.CollectionNode;
import cz.cvut.kbss.jsonld.serialization.model.JsonNode;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class LiteralFieldSerializer implements FieldSerializer {

    private final MultilingualStringSerializer multilingualStringSerializer;

    LiteralFieldSerializer(MultilingualStringSerializer multilingualStringSerializer) {
        this.multilingualStringSerializer = multilingualStringSerializer;
    }

    @Override
    public List<JsonNode> serializeField(Field field, Object value) {
        final String attName = BeanAnnotationProcessor.getAttributeIdentifier(field);
        final JsonNode result;
        if (value instanceof Collection) {
            final Collection<?> col = (Collection<?>) value;
            final CollectionNode node = JsonNodeFactory.createCollectionNode(attName, col);
            col.forEach(obj -> {
                final JsonNode serObj = obj instanceof MultilingualString ? multilingualStringSerializer.serialize(
                        (MultilingualString) obj) : JsonNodeFactory.createLiteralNode(obj);
                node.addItem(serObj);
            });
            result = node;
        } else if (value instanceof MultilingualString) {
            result = multilingualStringSerializer.serialize(attName, (MultilingualString) value);
        } else {
            result = JsonNodeFactory.createLiteralNode(attName, value);
        }
        return Collections.singletonList(result);
    }
}
