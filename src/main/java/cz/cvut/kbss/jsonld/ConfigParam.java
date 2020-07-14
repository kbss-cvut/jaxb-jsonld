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
package cz.cvut.kbss.jsonld;

/**
 * Configuration parameters.
 */
public enum ConfigParam {

    /**
     * Whether to ignore unknown properties when deserializing JSON-LD.
     * <p>
     * If set to {@code false}, an exception will be thrown when unknown property is encountered.
     */
    IGNORE_UNKNOWN_PROPERTIES("ignoreUnknownProperties"),

    /**
     * Package in which to look for mapped classes.
     * <p>
     * The scan is important for support for polymorphism in object deserialization.
     */
    SCAN_PACKAGE("scanPackage"),

    /**
     * Whether to require an identifier when serializing an object.
     * <p>
     * If set to {@code true} and no identifier is found (either there is no identifier field or its value is {@code
     * null}), an exception will be thrown. If configured to {@code false}, a blank node identifier is generated if no
     * id is present.
     */
    REQUIRE_ID("requireId"),

    /**
     * Allows assuming target type from the provided Java type when no types are specified in JSON-LD.
     * <p>
     * If set to {@code true}, JB4JSON-LD will attempt to use the provided Java type as the target type when
     * deserializing a JSON-LD object which has no types declared.
     * <p>
     * Defaults to {@code false}, in which case an exception is thrown for a typeless JSON-LD object.
     */
    ASSUME_TARGET_TYPE("assumeTargetType");

    private final String name;

    ConfigParam(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
