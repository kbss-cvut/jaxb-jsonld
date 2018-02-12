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
package cz.cvut.kbss.jsonld.deserialization;

import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import cz.cvut.kbss.jopa.model.annotations.Id;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.Properties;
import cz.cvut.kbss.jsonld.ConfigParam;
import cz.cvut.kbss.jsonld.environment.Vocabulary;
import cz.cvut.kbss.jsonld.environment.model.*;
import cz.cvut.kbss.jsonld.exception.JsonLdDeserializationException;
import cz.cvut.kbss.jsonld.exception.TargetTypeException;
import cz.cvut.kbss.jsonld.exception.UnknownPropertyException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class ExpandedJsonLdDeserializerTest {

    private static final URI HALSEY_URI = URI
            .create("http://krizik.felk.cvut.cz/ontologies/jb4jsonld#Catherine+Halsey");
    private static final URI LASKY_URI = URI
            .create("http://krizik.felk.cvut.cz/ontologies/jb4jsonld#Thomas+Lasky");
    private static final URI PALMER_URI = URI
            .create("http://krizik.felk.cvut.cz/ontologies/jb4jsonld#Sarah+Palmer");

    private static final Map<URI, User> USERS = initUsers();

    private static final URI ORG_URI = URI.create("http://krizik.felk.cvut.cz/ontologies/jb4jsonld#UNSC");
    private static final String ORG_NAME = "UNSC";
    private static final String[] ORG_BRANDS = {"Spartan-II", "Mjolnir IV"};

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private JsonLdDeserializer deserializer;

    private static Map<URI, User> initUsers() {
        final Map<URI, User> map = new HashMap<>();
        map.put(HALSEY_URI, new User(HALSEY_URI, "Catherine", "Halsey", "halsey@unsc.org", true));
        map.put(LASKY_URI, new User(LASKY_URI, "Thomas", "Lasky", "lasky@unsc.org", false));
        map.put(PALMER_URI, new User(PALMER_URI, "Sarah", "Palmer", "palmer@unsc.org", false));
        return map;
    }

    @Before
    public void setUp() {
        this.deserializer = JsonLdDeserializer.createExpandedDeserializer();
    }

    @Test
    public void testDeserializeInstanceWithDataProperties() throws Exception {
        final Object input = readAndExpand("objectWithDataProperties.json");
        final User result = deserializer.deserialize(input, User.class);
        verifyUserAttributes(USERS.get(HALSEY_URI), result);
    }

    private void verifyUserAttributes(User expected, User actual) {
        assertEquals(expected.getUri(), actual.getUri());
        assertEquals(expected.getAdmin(), actual.getAdmin());
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getUsername(), actual.getUsername());
    }

    private Object readAndExpand(String fileName) throws Exception {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        final Object jsonObject = JsonUtils.fromInputStream(is);
        return JsonLdProcessor.expand(jsonObject);
    }

    @Test
    public void testDeserializeInstanceWithSingularObjectProperty() throws Exception {
        final Object input = readAndExpand("objectWithSingularReference.json");
        final Employee result = deserializer.deserialize(input, Employee.class);
        verifyUserAttributes(USERS.get(HALSEY_URI), result);
        assertNotNull(result.getEmployer());
        verifyOrganizationAttributes(result.getEmployer());
    }

    private void verifyOrganizationAttributes(Organization result) {
        assertEquals(ORG_URI, result.getUri());
        assertEquals(ORG_NAME, result.getName());
        assertNotNull(result.getDateCreated());
        for (String brand : ORG_BRANDS) {
            assertTrue(result.getBrands().contains(brand));
        }
    }

    @Test
    public void testDeserializeInstanceWithPluralObjectProperty() throws Exception {
        final Object input = readAndExpand("objectWithPluralReference.json");
        final Organization result = deserializer.deserialize(input, Organization.class);
        verifyOrganizationAttributes(result);
        assertEquals(3, result.getEmployees().size());
        for (Employee e : result.getEmployees()) {
            assertTrue(USERS.containsKey(e.getUri()));
            verifyUserAttributes(USERS.get(e.getUri()), e);
        }
    }

    @Test
    public void testDeserializeInstanceWithPluralObjectPropertyWithBackwardReferencesToOriginalInstance()
            throws Exception {
        final Object input = readAndExpand("objectWithPluralObjectPropertyWithBackwardReferences.json");
        final Organization result = deserializer.deserialize(input, Organization.class);
        verifyOrganizationAttributes(result);
        assertEquals(3, result.getEmployees().size());
        for (Employee e : result.getEmployees()) {
            assertTrue(USERS.containsKey(e.getUri()));
            verifyUserAttributes(USERS.get(e.getUri()), e);
            assertNotNull(e.getEmployer());
            assertSame(result, e.getEmployer());
        }
    }

    @Test
    public void testDeserializeInstanceWithSingularObjectPropertyWithBackwardReference() throws Exception {
        final Object input = readAndExpand("objectWithSingularObjectPropertyWithBackwardReference.json");
        final Employee result = deserializer.deserialize(input, Employee.class);
        verifyUserAttributes(USERS.get(HALSEY_URI), result);
        final Organization org = result.getEmployer();
        assertNotNull(org);
        verifyOrganizationAttributes(org);
        assertEquals(1, org.getEmployees().size());
        assertSame(result, org.getEmployees().iterator().next());
    }

    @Test
    public void deserializationOfArrayWithOneInstanceReturnsProperCollection() throws Exception {
        final Object input = readAndExpand("objectWithPluralReferenceContainingOneValue.json");
        final Organization result = deserializer.deserialize(input, Organization.class);
        verifyOrganizationAttributes(result);
        assertEquals(1, result.getEmployees().size());
        final Employee e = result.getEmployees().iterator().next();
        verifyUserAttributes(USERS.get(e.getUri()), e);
    }

    @Test
    public void throwsUnknownPropertyExceptionWhenIgnoreIsNotConfiguredAndUnmappedPropertyIsEncountered()
            throws Exception {
        final Object input = readAndExpand("objectWithUnknownProperty.json");
        thrown.expect(UnknownPropertyException.class);
        final String property = "http://purl.org/dc/terms/description";
        thrown.expectMessage("No field matching property " + property + " was found in " + Organization.class +
                " or its ancestors.");

        deserializer.deserialize(input, Organization.class);
    }

    @Test
    public void skipsUnknownPropertyWhenIgnoreIsConfiguredAndUnmappedPropertyIsEncountered() throws Exception {
        final Object input = readAndExpand("objectWithUnknownProperty.json");
        deserializer.configure().set(ConfigParam.IGNORE_UNKNOWN_PROPERTIES, Boolean.TRUE.toString());
        final Organization result = deserializer.deserialize(input, Organization.class);
        verifyOrganizationAttributes(result);
    }

    @Test
    public void deserializationResolvesReferenceInPluralPropertyWrappedInAnotherObject() throws Exception {
        final Object input = readAndExpand("objectWithPluralReferenceSharingObject.json");
        final Study result = deserializer.deserialize(input, Study.class);

        assertNotNull(result.getName());
        Organization org = null;
        assertFalse(result.getParticipants().isEmpty());
        for (Employee e : result.getParticipants()) {
            if (org == null) {
                org = e.getEmployer();
            } else {
                assertSame(org, e.getEmployer());
            }
        }
        assertFalse(result.getMembers().isEmpty());
        for (Employee e : result.getMembers()) {
            assertSame(org, e.getEmployer());
        }
    }

    @Test
    public void deserializationSetsValueOfTypesSpecification() throws Exception {
        final Object input = readAndExpand("objectWithDataProperties.json");
        final User result = deserializer.deserialize(input, User.class);
        assertTrue(result.getTypes().contains(Vocabulary.AGENT));
        assertFalse(result.getTypes().contains(Vocabulary.USER));   // Type of the class should not be in @Types
    }

    @Test
    public void deserializationThrowsExceptionWhenTypesAttributeDoesNotContainTargetClassType() throws Exception {
        final Object input = readAndExpand("objectWithDataProperties.json");
        thrown.expect(TargetTypeException.class);
        thrown.expectMessage(
                "Type <" + Vocabulary.EMPLOYEE + "> mapped by the target Java class " + Employee.class +
                        " not found in input JSON-LD object.");
        deserializer.deserialize(input, Employee.class);
    }

    @Test
    public void deserializationPopulatesPropertiesFieldWithUnmappedPropertiesFoundInInput() throws Exception {
        final Object input = readAndExpand("objectWithUnmappedProperties.json");
        final Person result = deserializer.deserialize(input, Person.class);
        final User model = USERS.get(HALSEY_URI);
        assertEquals(model.getUri(), result.getUri());
        assertEquals(model.getFirstName(), result.getFirstName());
        assertEquals(model.getLastName(), result.getLastName());
        assertFalse(result.getProperties().isEmpty());
        assertTrue(result.getProperties().containsKey(Vocabulary.USERNAME));
        assertEquals(1, result.getProperties().get(Vocabulary.USERNAME).size());
        assertEquals(model.getUsername(), result.getProperties().get(Vocabulary.USERNAME).iterator().next());
        assertTrue(result.getProperties().containsKey(Vocabulary.IS_ADMIN));
        assertEquals(1, result.getProperties().get(Vocabulary.IS_ADMIN).size());
        assertEquals(Boolean.TRUE.toString(), result.getProperties().get(Vocabulary.IS_ADMIN).iterator().next());
    }

    @Test
    public void deserializationPopulatesTypedProperties() throws Exception {
        final Object input = readAndExpand("objectWithUnmappedProperties.json");
        final ClassWithProperties result = deserializer.deserialize(input, ClassWithProperties.class);
        assertNotNull(result);
        final User model = USERS.get(HALSEY_URI);
        assertEquals(model.getUri(), result.uri);
        assertTrue(result.properties.containsKey(URI.create(Vocabulary.FIRST_NAME)));
        assertEquals(model.getFirstName(), result.properties.get(URI.create(Vocabulary.FIRST_NAME)).iterator().next());
        assertTrue((Boolean) result.properties.get(URI.create(Vocabulary.IS_ADMIN)).iterator().next());
    }

    @OWLClass(iri = Vocabulary.PERSON)
    public static class ClassWithProperties {
        @Id
        private URI uri;

        @Properties
        private Map<URI, Set<?>> properties;

        public ClassWithProperties() {
        }
    }

    @Test
    public void deserializationThrowsExceptionWhenMultipleValuesForSingularFieldAreEncountered() throws Exception {
        thrown.expect(JsonLdDeserializationException.class);
        thrown.expectMessage(
                containsString("Encountered multiple values of property " + Vocabulary.FIRST_NAME));
        final Object input = readAndExpand("objectWithAttributeCardinalityViolation.json");
        deserializer.deserialize(input, Person.class);
    }

    @Test
    public void deserializationThrowsExceptionWhenMultipleValuesOfUnmappedPropertyForPropertiesWithSingularValuesAreEncountered()
            throws Exception {
        thrown.expect(JsonLdDeserializationException.class);
        thrown.expectMessage(
                containsString("Encountered multiple values of property " + Vocabulary.FIRST_NAME));
        final Object input = readAndExpand("objectWithAttributeCardinalityViolation.json");
        deserializer.deserialize(input, ClassWithSingularProperties.class);
    }

    @OWLClass(iri = Vocabulary.PERSON)
    public static class ClassWithSingularProperties {
        @Id
        private URI uri;

        @Properties
        private Map<String, String> properties;

        public ClassWithSingularProperties() {
        }
    }

    @Test
    public void deserializationSupportsPlainIdentifierObjectPropertyValues() throws Exception {
        final Object input = readAndExpand("objectWithPlainIdentifierObjectPropertyValue.json");
        final Organization result = deserializer.deserialize(input, Organization.class);
        assertNotNull(result);
        assertEquals(URI.create("http://dbpedia.org/resource/Czech_Republic"), result.getCountry());
    }

    @Test
    public void deserializationSupportsObjectsWithBlankNodeIds() throws Exception {
        final Object input = readAndExpand("objectWithBlankNodeIdentifier.json");
        final User result = deserializer.deserialize(input, User.class);
        assertNotNull(result);
        assertNull(result.getUri());
    }
}
