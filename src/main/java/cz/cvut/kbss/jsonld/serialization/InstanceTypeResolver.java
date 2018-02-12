/**
 * Copyright (C) 2017 Czech Technical University in Prague
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
package cz.cvut.kbss.jsonld.serialization;

import cz.cvut.kbss.jsonld.common.BeanAnnotationProcessor;
import cz.cvut.kbss.jsonld.common.BeanClassProcessor;
import cz.cvut.kbss.jsonld.exception.BeanProcessingException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Determines the set of types an instance possesses.
 */
class InstanceTypeResolver {

    /**
     * Resolves all the types the instance belongs to.
     * <p>
     * This includes:
     * <ul>
     * <li>{@link cz.cvut.kbss.jopa.model.annotations.OWLClass} values declared on the argument's class
     * and any of its ancestors.</li>
     * <li>Value of types field in the instance.</li>
     * </ul>
     *
     * @param instance The instance whose types should be resolved
     * @return Set of types of the instance
     */
    Set<String> resolveTypes(Object instance) {
        assert instance != null;
        final Set<String> declaredTypes = BeanAnnotationProcessor.getOwlClasses(instance);
        final Optional<Field> typesField = BeanAnnotationProcessor.getTypesField(instance.getClass());
        typesField.ifPresent(f -> {
            if (!Collection.class.isAssignableFrom(f.getType())) {
                throw new BeanProcessingException("@Types field in object " + instance + " must be a collection.");
            }
            final Collection<?> runtimeTypes = (Collection<?>) BeanClassProcessor.getFieldValue(f, instance);
            if (runtimeTypes != null) {
                runtimeTypes.forEach(t -> declaredTypes.add(t.toString()));
            }
        });
        return declaredTypes;
    }
}
