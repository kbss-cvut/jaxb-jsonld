/**
 * Copyright (C) 2020 Czech Technical University in Prague
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.kbss.jsonld.deserialization.expanded;

import cz.cvut.kbss.jsonld.Configuration;
import cz.cvut.kbss.jsonld.deserialization.util.TargetClassResolver;
import cz.cvut.kbss.jsonld.deserialization.util.TypeMap;
import cz.cvut.kbss.jsonld.environment.TestUtil;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeserializerTest {

    @Test
    void resolveTargetClassReturnsExpectedClassWhenItIsPlainIdentifierType() throws Exception {
        final Deserializer<Map<?, ?>> deserializer =
                new ObjectDeserializer(null,
                        new DeserializerConfig(new Configuration(), new TargetClassResolver(new TypeMap())),
                        URI.class);
        assertEquals(URI.class,
                deserializer.resolveTargetClass(TestUtil.readAndExpand("objectWithDataProperties.json"), URI.class));
    }
}