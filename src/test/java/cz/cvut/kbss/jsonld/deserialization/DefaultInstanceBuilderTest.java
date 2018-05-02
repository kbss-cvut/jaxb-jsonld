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

import cz.cvut.kbss.jopa.model.annotations.Id;
import cz.cvut.kbss.jopa.model.annotations.OWLClass;
import cz.cvut.kbss.jopa.model.annotations.OWLObjectProperty;
import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.jsonld.common.CollectionType;
import cz.cvut.kbss.jsonld.deserialization.util.TargetClassResolver;
import cz.cvut.kbss.jsonld.environment.Generator;
import cz.cvut.kbss.jsonld.environment.TestUtil;
import cz.cvut.kbss.jsonld.environment.Vocabulary;
import cz.cvut.kbss.jsonld.environment.model.Employee;
import cz.cvut.kbss.jsonld.environment.model.Organization;
import cz.cvut.kbss.jsonld.environment.model.Person;
import cz.cvut.kbss.jsonld.environment.model.User;
import cz.cvut.kbss.jsonld.exception.JsonLdDeserializationException;
import cz.cvut.kbss.jsonld.exception.TargetTypeException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class DefaultInstanceBuilderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private InstanceBuilder deserializer =
            new DefaultInstanceBuilder(new TargetClassResolver(TestUtil.getDefaultTypeMap()));

    @Test
    public void getCurrentRootReturnsNullIfThereIsNoRoot() {
        assertNull(deserializer.getCurrentRoot());
    }

    @Test
    public void openObjectCreatesNewInstanceOfSpecifiedClass() {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        final Object res = deserializer.getCurrentRoot();
        assertNotNull(res);
        assertTrue(res instanceof Person);
    }

    @Test
    public void openCollectionCreatesSet() {
        deserializer.openCollection(CollectionType.SET);
        assertTrue(deserializer.getCurrentRoot() instanceof Set);
    }

    @Test
    public void openCollectionInCollectionPushesOriginalCollectionToStack() throws Exception {
        deserializer.openCollection(CollectionType.SET);
        deserializer.openCollection(CollectionType.SET);
        assertTrue(deserializer.getCurrentRoot() instanceof Set);
        assertFalse(getOpenInstances().isEmpty());
        assertTrue(getOpenInstances().peek().getInstance() instanceof Set);
        assertNotSame(deserializer.getCurrentRoot(), getOpenInstances().peek().getInstance());
    }

    @Test
    public void openObjectAddsObjectToCurrentlyOpenCollectionAndBecomesCurrentInstance() throws Exception {
        deserializer.openCollection(CollectionType.SET);
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Employee.class);
        final Object root = deserializer.getCurrentRoot();
        assertTrue(root instanceof Employee);
        assertFalse(getOpenInstances().isEmpty());
        final Object stackTop = getOpenInstances().peek().getInstance();
        assertTrue(stackTop instanceof Set);
        final Set<?> set = (Set<?>) stackTop;
        assertEquals(1, set.size());
        assertSame(root, set.iterator().next());
    }

    @SuppressWarnings("unchecked")
    private Stack<InstanceContext<?>> getOpenInstances() throws Exception {
        final Field stackField = DefaultInstanceBuilder.class.getDeclaredField("openInstances");
        stackField.setAccessible(true);
        return (Stack<InstanceContext<?>>) stackField.get(deserializer);
    }

    private InstanceContext<?> getCurrentInstance() throws Exception {
        final Field instField = DefaultInstanceBuilder.class.getDeclaredField("currentInstance");
        instField.setAccessible(true);
        return (InstanceContext<?>) instField.get(deserializer);
    }

    @Test
    public void closeObjectPopsPreviousInstanceFromTheStack() throws Exception {
        deserializer.openCollection(CollectionType.SET);
        final Object originalRoot = deserializer.getCurrentRoot();
        assertTrue(originalRoot instanceof Set);
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Employee.class);
        assertFalse(getOpenInstances().isEmpty());
        deserializer.closeObject();
        assertTrue(getOpenInstances().isEmpty());
        assertSame(originalRoot, deserializer.getCurrentRoot());
    }

    @Test
    public void closeObjectDoesNothingForRootObject() throws Exception {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        assertTrue(getOpenInstances().isEmpty());
        final Object root = deserializer.getCurrentRoot();
        deserializer.closeObject();
        assertTrue(getOpenInstances().isEmpty());
        assertSame(root, deserializer.getCurrentRoot());
    }

    @Test
    public void addValueAddsValuesToCurrentlyOpenCollection() {
        final List<Integer> items = new ArrayList<>();
        for (int i = 0; i < Generator.randomCount(10); i++) {
            items.add(Generator.randomCount(Integer.MAX_VALUE));
        }
        deserializer.openCollection(CollectionType.LIST);
        items.forEach(item -> deserializer.addValue(item));
        assertTrue(deserializer.getCurrentRoot() instanceof List);
        final List<?> result = (List<?>) deserializer.getCurrentRoot();
        for (int i = 0; i < items.size(); i++) {
            assertEquals(items.get(i), result.get(i));
        }
    }

    @Test
    public void openObjectByPropertyCreatesObjectOfCorrectType() {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Employee.class);
        deserializer.openObject(Generator.generateUri().toString(), Vocabulary.IS_MEMBER_OF,
                Collections.singletonList(Vocabulary.ORGANIZATION));
        assertTrue(deserializer.getCurrentRoot() instanceof Organization);
    }

    @Test
    public void openObjectByPropertySetsNewInstanceAsFieldValueAndReplacesCurrentContext() throws Exception {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Employee.class);
        deserializer.openObject(Generator.generateUri().toString(), Vocabulary.IS_MEMBER_OF,
                Collections.singletonList(Vocabulary.ORGANIZATION));
        assertFalse(getOpenInstances().isEmpty());
        final Object top = getOpenInstances().peek().getInstance();
        assertTrue(top instanceof Employee);
        assertNotNull(((Employee) top).getEmployer());
    }

    @Test
    public void addValueSetsPropertyValue() {
        deserializer.openObject(TestUtil.HALSEY_URI.toString(), User.class);
        final String firstName = "Catherine";
        final String lastName = "Halsey";
        deserializer.addValue(Vocabulary.FIRST_NAME, firstName);
        deserializer.addValue(Vocabulary.LAST_NAME, lastName);
        deserializer.addValue(Vocabulary.IS_ADMIN, true);
        final User res = (User) deserializer.getCurrentRoot();
        assertEquals(firstName, res.getFirstName());
        assertEquals(lastName, res.getLastName());
        assertTrue(res.getAdmin());
    }

    @Test
    public void openCollectionByPropertyCreatesCollectionOfCorrectType() throws Exception {
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        deserializer.openCollection(Vocabulary.HAS_MEMBER);
        assertFalse(getOpenInstances().isEmpty());
        assertTrue(deserializer.getCurrentRoot() instanceof Set);
    }

    @Test
    public void openCollectionByPropertySetsFieldValueAndPushesCurrentInstanceToStack() throws Exception {
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        deserializer.openCollection(Vocabulary.HAS_MEMBER);
        assertFalse(getOpenInstances().isEmpty());
        assertTrue(getOpenInstances().peek().getInstance() instanceof Organization);
        final Organization org = (Organization) getOpenInstances().peek().getInstance();
        assertSame(deserializer.getCurrentRoot(), org.getEmployees());
    }

    @Test
    public void closeCollectionPopsLastInstanceFromStack() throws Exception {
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        final Object originalRoot = deserializer.getCurrentRoot();
        deserializer.openCollection(Vocabulary.HAS_MEMBER);
        assertFalse(getOpenInstances().isEmpty());
        deserializer.closeCollection();
        assertTrue(getOpenInstances().isEmpty());
        assertSame(originalRoot, deserializer.getCurrentRoot());
    }

    @Test
    public void closeCollectionDoesNothingWhenItIsTheTopElement() throws Exception {
        deserializer.openCollection(CollectionType.SET);
        assertTrue(getOpenInstances().isEmpty());
        final Object root = deserializer.getCurrentRoot();
        deserializer.closeCollection();
        assertTrue(getOpenInstances().isEmpty());
        assertSame(root, deserializer.getCurrentRoot());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getKnownInstances() throws Exception {
        final Field field = DefaultInstanceBuilder.class.getDeclaredField("knownInstances");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(deserializer);
    }

    @Test
    public void openCollectionCreatesTypesContextForTypesField() throws Exception {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), User.class);
        deserializer.openCollection(JsonLd.TYPE);
        final InstanceContext<?> result = getCurrentInstance();
        assertTrue(result instanceof TypesContext);
        final TypesContext<?, ?> ctx = (TypesContext<?, ?>) result;
        assertEquals(String.class, ctx.getItemType());
    }

    @Test
    public void isPropertyMappedReturnsTrueForMappedProperty() {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        assertTrue(deserializer.isPropertyMapped(Vocabulary.FIRST_NAME));
    }

    @Test
    public void isPropertyMappedReturnsFalseForUnknownProperty() {
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        assertFalse(deserializer.isPropertyMapped(Vocabulary.IS_ADMIN));
    }

    @Test
    public void isPropertyMappedReturnsTrueForType() {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        assertTrue(deserializer.isPropertyMapped(JsonLd.TYPE));
    }

    @Test
    public void isPluralReturnsTrueForType() {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        assertTrue(deserializer.isPlural(JsonLd.TYPE));
    }

    @Test
    public void openCollectionOpensDummyCollectionContextForTypeInInstanceWithoutTypesField() throws Exception {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        assertTrue(getCurrentInstance() instanceof SingularObjectContext);
        deserializer.openCollection(JsonLd.TYPE);
        assertTrue(getCurrentInstance() instanceof DummyCollectionInstanceContext);
        deserializer.closeCollection();
        assertTrue(getCurrentInstance() instanceof SingularObjectContext);
    }

    @Test
    public void addingTypesToInstanceWithoutTypesFieldDoesNothing() {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        deserializer.openCollection(JsonLd.TYPE);
        deserializer.addValue(Vocabulary.EMPLOYEE);
        deserializer.addValue(Vocabulary.USER);
        deserializer.closeCollection();
        assertNotNull(deserializer.getCurrentRoot());
    }

    @Test
    public void addValueOpensCollectionAndAddsSingleItemToItWhenSingleValueForPluralAttributeIsSpecified() {
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        final String value = "Mjolnir-IV";
        deserializer.addValue(Vocabulary.BRAND, value);
        final Organization object = (Organization) deserializer.getCurrentRoot();
        assertEquals(1, object.getBrands().size());
        assertTrue(object.getBrands().contains(value));
    }

    @Test
    public void openCollectionCreatesPropertiesContextAndSetsNewPropertiesMapOnTargetInstanceWhenItDoesNotExist() {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        final Person instance = (Person) deserializer.getCurrentRoot();
        assertNull(instance.getProperties());
        deserializer.openCollection(Vocabulary.IS_ADMIN);
        assertNotNull(instance.getProperties());
    }

    @Test
    public void openCollectionReusesPropertiesInstanceWhenItAlreadyExistsOnTargetInstance() {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Person.class);
        final Person instance = (Person) deserializer.getCurrentRoot();
        assertNull(instance.getProperties());
        deserializer.openCollection(Vocabulary.IS_ADMIN);
        final Map<String, Set<String>> propsMap = instance.getProperties();
        deserializer.closeCollection();
        deserializer.openCollection(Vocabulary.USERNAME);
        assertSame(propsMap, instance.getProperties());
    }

    @Test
    public void openObjectCreatesIdentifierContextWhenTargetTypeIsIdentifier() throws Exception {
        deserializer.openObject(Generator.generateUri().toString(), Event.class);
        deserializer.openCollection(Vocabulary.HAS_EVENT_TYPE);
        deserializer.openObject(Generator.generateUri().toString(), URI.class);
        final InstanceContext<?> ctx = getCurrentInstance();
        assertTrue(ctx instanceof NodeReferenceContext);
    }

    @Test
    public void openObjectCreatesIdentifierContextForPropertyWithPlainIdentifierTargetFieldType() throws Exception {
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        deserializer.openObject(Generator.generateUri().toString(), Vocabulary.ORIGIN, Collections.emptyList());
        final InstanceContext<?> ctx = getCurrentInstance();
        assertTrue(ctx instanceof NodeReferenceContext);
    }

    @Test
    public void addNodeReferenceForPropertySetsPlainIdentifierObjectPropertyValue() {
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        final String uri = Generator.generateUri().toString();
        deserializer.addNodeReference(Vocabulary.ORIGIN, uri);
        final Organization object = (Organization) deserializer.getCurrentRoot();
        assertEquals(uri, object.getCountry().toString());
    }

    @Test
    public void addNodeReferenceForPropertySetsFieldValueToKnownInstance() throws Exception {
        final Organization org = Generator.generateOrganization();
        getKnownInstances().put(org.getUri().toString(), org);
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Employee.class);
        deserializer.addNodeReference(Vocabulary.IS_MEMBER_OF, org.getUri().toString());
        final Employee object = (Employee) deserializer.getCurrentRoot();
        assertEquals(org, object.getEmployer());
    }

    @Test
    public void addNodeReferenceForPropertyThrowsDeserializationExceptionWhenNoKnownInstanceIsFound() {
        final String nodeId = Generator.generateUri().toString();
        thrown.expect(JsonLdDeserializationException.class);
        thrown.expectMessage(
                "Node with IRI " + nodeId + " cannot be referenced, because it has not been encountered yet.");
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Employee.class);
        deserializer.addNodeReference(Vocabulary.IS_MEMBER_OF, nodeId);
    }

    @Test
    public void addNodeReferenceAddsPlainIdentifierObjectPropertyValueToCollection() {
        final URI nodeId = Generator.generateUri();
        deserializer.openObject(Generator.generateUri().toString(), Event.class);
        deserializer.openCollection(Vocabulary.HAS_EVENT_TYPE);
        deserializer.addNodeReference(nodeId.toString());
        deserializer.closeCollection();
        final Event object = (Event) deserializer.getCurrentRoot();
        assertTrue(object.eventTypes.contains(nodeId));
    }

    @OWLClass(iri = Generator.URI_BASE + "Event")
    public static class Event {
        @Id
        private URI uri;

        @OWLObjectProperty(iri = Vocabulary.HAS_EVENT_TYPE)
        private Set<URI> eventTypes;
    }

    @Test
    public void addNodeReferenceAddsKnownInstanceWithMatchingIdentifier() throws Exception {
        final Employee employee = Generator.generateEmployee();
        getKnownInstances().put(employee.getUri().toString(), employee);
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        deserializer.openCollection(Vocabulary.HAS_MEMBER);
        deserializer.addNodeReference(employee.getUri().toString());
        deserializer.closeCollection();
        final Organization object = (Organization) deserializer.getCurrentRoot();
        assertTrue(object.getEmployees().contains(employee));
    }

    @Test
    public void addNodeReferenceThrowsDeserializationExceptionWhenNoKnownInstanceIsFound() {
        final String nodeId = Generator.generateUri().toString();
        thrown.expect(JsonLdDeserializationException.class);
        thrown.expectMessage(
                "Node with IRI " + nodeId + " cannot be referenced, because it has not been encountered yet.");
        deserializer.openObject(Generator.generateUri().toString(), Organization.class);
        deserializer.openCollection(Vocabulary.HAS_MEMBER);
        deserializer.addNodeReference(nodeId);
    }

    @Test
    public void openObjectSetsInstanceIdentifier() {
        deserializer.openObject(TestUtil.HALSEY_URI.toString(), Employee.class);
        final Employee root = (Employee) deserializer.getCurrentRoot();
        assertEquals(TestUtil.HALSEY_URI, root.getUri());
    }

    @Test
    public void openObjectAsPropertyValueSetsInstanceIdentifier() {
        deserializer.openObject(TestUtil.HALSEY_URI.toString(), Employee.class);
        deserializer.openObject(TestUtil.UNSC_URI.toString(), Vocabulary.IS_MEMBER_OF,
                Collections.singletonList(Vocabulary.ORGANIZATION));
        final Organization root = (Organization) deserializer.getCurrentRoot();
        assertEquals(TestUtil.UNSC_URI, root.getUri());
    }

    @Test
    public void objectIsStoredInKnownInstancesWhenItIsOpen() throws Exception {
        deserializer.openObject(TestUtil.PALMER_URI.toString(), User.class);
        assertTrue(getKnownInstances().containsKey(TestUtil.PALMER_URI.toString()));
        assertTrue(getKnownInstances().get(TestUtil.PALMER_URI.toString()) instanceof User);
    }

    @Test
    public void openObjectReopensAlreadyKnownInstance() throws Exception {
        final Employee employee = Generator.generateEmployee();
        getKnownInstances().put(employee.getUri().toString(), employee);
        deserializer.openObject(employee.getUri().toString(), Employee.class);
        assertSame(employee, deserializer.getCurrentRoot());
    }

    @Test
    public void openObjectThrowsTargetTypeExceptionWhenReopenObjectHasIncompatibleTypeWithTarget() throws Exception {
        thrown.expect(TargetTypeException.class);
        thrown.expectMessage(containsString(
                "instance with id " + TestUtil.PALMER_URI + " already exists, but its type " + Employee.class + " is not compatible with target type " + Organization.class));
        final Employee employee = Generator.generateEmployee();
        employee.setUri(TestUtil.PALMER_URI);
        getKnownInstances().put(employee.getUri().toString(), employee);
        deserializer.openObject(employee.getUri().toString(), Organization.class);
    }

    @Test
    public void openObjectForPropertyReopensAlreadyKnownInstance() throws Exception {
        final Organization org = Generator.generateOrganization();
        getKnownInstances().put(org.getUri().toString(), org);
        deserializer.openObject(TestUtil.PALMER_URI.toString(), Employee.class);
        deserializer.openObject(org.getUri().toString(), Vocabulary.IS_MEMBER_OF,
                Collections.singletonList(Vocabulary.ORGANIZATION));
        assertSame(org, deserializer.getCurrentRoot());
    }

    @Test
    public void openCollectionForPropertyReusesExistingCollectionIfItWasSetOnReopenObject() {
        deserializer.openObject(TestUtil.UNSC_URI.toString(), Organization.class);
        deserializer.openCollection(Vocabulary.HAS_MEMBER);
        deserializer.openObject(TestUtil.HALSEY_URI.toString(), Employee.class);
        deserializer.closeObject();
        deserializer.closeCollection();
        deserializer.openCollection(Vocabulary.HAS_MEMBER);
        final Set<Employee> root = (Set<Employee>) deserializer.getCurrentRoot();
        assertFalse(root.isEmpty());
        assertEquals(TestUtil.HALSEY_URI, root.iterator().next().getUri());
    }

    @Test
    public void openObjectThrowsDeserializationExceptionWhenTryingToSetSingularAttributeValueForSecondTime() {
        deserializer.openObject(TestUtil.HALSEY_URI.toString(), Employee.class);
        deserializer.openObject(TestUtil.UNSC_URI.toString(), Vocabulary.IS_MEMBER_OF,
                Collections.singletonList(Vocabulary.ORGANIZATION));
        deserializer.closeObject();
        assertNotNull(((Employee) deserializer.getCurrentRoot()).getEmployer());
        thrown.expect(JsonLdDeserializationException.class);
        thrown.expectMessage(containsString("Encountered multiple values of property"));
        deserializer.openObject(Generator.generateUri().toString(), Vocabulary.IS_MEMBER_OF,
                Collections.singletonList(Vocabulary.ORGANIZATION));
    }
}
