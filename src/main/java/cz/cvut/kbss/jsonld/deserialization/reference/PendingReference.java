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
package cz.cvut.kbss.jsonld.deserialization.reference;

/**
 * Represents a pending reference.
 * <p>
 * A pending reference represents a situation when the JSON-LD contains just an object with and {@code @id}, while the
 * mapped attribute expects a full-blown object. In this case, it is expected that somewhere in the JSON-LD, there is
 * the object's full serialization and this is only a reference to it.
 */
public interface PendingReference {

    /**
     * Applies the specified referenced object to this pending reference, resolving it.
     * <p>
     * Resolving the reference basically means inserting the referenced object into the place represented by this
     * instance (e.g., field value, collection).
     *
     * @param referencedObject The object referenced by this pending reference
     */
    void apply(Object referencedObject);
}
