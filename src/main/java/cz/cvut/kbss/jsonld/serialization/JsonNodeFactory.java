package cz.cvut.kbss.jsonld.serialization;

import cz.cvut.kbss.jsonld.common.CollectionType;
import cz.cvut.kbss.jsonld.serialization.model.*;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Factory for constructing {@link JsonNode} instances.
 */
public class JsonNodeFactory {

    private JsonNodeFactory() {
        throw new AssertionError();
    }

    private enum LiteralType {
        BOOLEAN, NUMBER, STRING
    }

    public static LiteralNode createLiteralNode(Object value) {
        return createLiteralNode(null, value);
    }

    public static LiteralNode createLiteralNode(String name, Object value) {
        final LiteralType type = determineLiteralType(value);
        LiteralNode node = null;
        switch (type) {
            case BOOLEAN:
                node = name != null ? new BooleanLiteralNode(name, (Boolean) value) : new BooleanLiteralNode(
                        (Boolean) value);
                break;
            case NUMBER:
                node = name != null ? new NumericLiteralNode<>(name, (Number) value) :
                       new NumericLiteralNode<>((Number) value);
                break;
            case STRING:
                node = name != null ? new StringLiteralNode(name, value.toString()) :
                       new StringLiteralNode(value.toString());
                break;
        }
        return node;
    }

    private static LiteralType determineLiteralType(Object value) {
        if (value instanceof Boolean) {
            return LiteralType.BOOLEAN;
        } else if (value instanceof Number) {
            return LiteralType.NUMBER;
        }
        return LiteralType.STRING;
    }

    public static CollectionNode createCollectionNode(Collection<?> value) {
        return createCollectionNode(null, value);
    }

    public static CollectionNode createCollectionNode(String name, Collection<?> value) {
        final CollectionType type = determineCollectionType(value);
        CollectionNode n = null;
        switch (type) {
            case LIST:
                n = name != null ? new ListNode(name) : new ListNode();
                break;
            case SET:
                n = name != null ? new SetNode(name) : new SetNode();
                break;
        }
        return n;
    }

    private static CollectionType determineCollectionType(Collection<?> collection) {
        if (collection instanceof List) {
            return CollectionType.LIST;
        } else if (collection instanceof Set) {
            return CollectionType.SET;
        } else {
            throw new IllegalArgumentException("Unsupported collection type " + collection.getClass());
        }
    }

    public static <T> CollectionNode createCollectionNodeFromArray() {
        return new SetNode();
    }

    public static <T> CollectionNode createCollectionNodeFromArray(String name) {
        return new SetNode(name);
    }

    public static ObjectNode createObjectNode() {
        return new ObjectNode();
    }

    public static ObjectNode createObjectNode(String name) {
        return new ObjectNode(name);
    }

    public static ObjectIdNode createObjectIdNode(Object id) {
        return new ObjectIdNode(URI.create(id.toString()));
    }

    public static ObjectIdNode createObjectIdNode(String name, Object id) {
        return new ObjectIdNode(name, URI.create(id.toString()));
    }
}
