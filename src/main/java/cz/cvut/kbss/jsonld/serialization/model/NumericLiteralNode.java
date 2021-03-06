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
package cz.cvut.kbss.jsonld.serialization.model;

import cz.cvut.kbss.jsonld.serialization.JsonGenerator;

import java.io.IOException;

/**
 * Represents a field value that should be serialized as a JSON numeric literal value.
 */
public class NumericLiteralNode<T extends Number> extends LiteralNode<T> {

    public NumericLiteralNode(T value) {
        super(value);
    }

    public NumericLiteralNode(String name, T value) {
        super(name, value);
    }

    @Override
    void writeValue(JsonGenerator writer) throws IOException {
        writer.writeNumber(value);
    }
}
