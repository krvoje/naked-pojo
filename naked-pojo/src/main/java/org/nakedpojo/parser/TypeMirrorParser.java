package org.nakedpojo.parser;

import org.nakedpojo.Messages;
import org.nakedpojo.NakedParseException;
import org.nakedpojo.interfaces.Parser;
import org.nakedpojo.model.javascript.JSType;
import org.nakedpojo.model.javascript.Type;
import org.nakedpojo.utils.TypeMirrorUtils;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TypeMirrorParser implements Parser<Element, JSType>
{
    /* TODO: Package path. Currently classes with same simple name
        in different packages will overwrite each other. */
    // Note: Take generics into account?
    // Note: Take constructors into account

    private final Map<Element, JSType> prototypes;

    private final Types types;
    private final Elements elements;
    private final Messager messager;
    private final TypeMirrorUtils utils;

    public TypeMirrorParser(Types types, Elements elements, Messager messager) {
        this.types = types;
        this.elements = elements;
        this.messager = messager;
        this.utils = new TypeMirrorUtils(types, elements, messager);
        this.prototypes = new TreeMap<>(utils.TYPE_NAME_COMPARATOR);
    }

    public JSType convert(Element element) {
        return convert(element, utils.simpleName(element));
    }

    public JSType convert(Element element, String fieldName) {
        scan(element);
        return prototypes.get(element).withFieldName(fieldName);
    }

    public void scan(Element element) {
        JSType prototype = prototypeFor(element);

        if(utils.isPrimitive(element)) {
            prototypes.put(element, convertPrimitive(element));
        } else if(utils.isEnum(element)) {
            for(Element enumMeber : element.getEnclosedElements()) {
                if(enumMeber.getKind().equals(ElementKind.ENUM_CONSTANT)) {
                    prototype.getMembers().add(
                            new JSType(
                                utils.typeName(enumMeber),
                                utils.typeName(enumMeber),
                                utils.simpleName(enumMeber),
                                Type.ENUM_MEMBER));
                }
            }
            prototypes.put(element,
                    prototype
                        .withType(Type.ENUM));
        } else if(utils.isIterable(element)) {
            prototypes.put(element, prototype.withType(Type.ARRAY));
        } else { // Otherwise this is an object
            prototype = prototype.withType(Type.OBJECT);
            prototypes.put(element, prototype);

            processGetters(element);
            processSetters(element);
            processPublicFields(element);
            processNestedClasses(element);

            // Scan all supertype elements and add them to this class' prototype
            for(Element superTypeElement : utils.supertypeElements(element)) {
                JSType superType = convert(superTypeElement);
                prototype.getMembers().addAll(superType.getMembers());
            }

            prototypes.put(element, prototype);
        }
    }

    private void processGetters(Element element) {
        JSType prototype = prototypeFor(element);
        Set<JSType> members = prototype.getMembers();
        for(ExecutableElement getter: utils.getters(element)) {
            Element returnTypeClass = types.asElement(getter.getReturnType());
            members.add(convert(returnTypeClass, utils.fieldNameFromAccessor(getter)));
        }
    }

    private void processSetters(Element element) {
        JSType prototype = prototypeFor(element);
        Set<JSType> members = prototype.getMembers();
        for(ExecutableElement setter : utils.setters(element)) {
            Element returnTypeClass = setter.getParameters().get(0);
            members.add(convert(returnTypeClass, utils.fieldNameFromAccessor(setter)));
        }
    }

    private void processPublicFields(Element element) {
        JSType prototype = prototypeFor(element);
        Set<JSType> members = prototype.getMembers();
        for (Element field : utils.publicFields(element)) {
            members.add(convert(field, utils.simpleName(field)));
        }
    }

    private void processNestedClasses(Element element) {
        for (Element nestedClass : utils.nestedClasses(element)) {
            scan(nestedClass);
        }
    }

    private JSType convertPrimitive(Element element) {
        String fieldName = utils.simpleName(element);
        String typeName = utils.typeName(element);
        if(utils.isBoolean(element)) {
            return new JSType(typeName, typeName, fieldName, Type.BOOLEAN);
        }
        else if(utils.isString(element)) {
            return new JSType(typeName, typeName, fieldName, Type.STRING);
        }
        else if(utils.isByte(element)) {
            // TODO: implement
            return new JSType(typeName, typeName, fieldName, Type.UNDEFINED);
        }
        else if(utils.isNumeric(element)) {
            return new JSType(typeName, typeName, fieldName, Type.NUMBER);
        }
        else {
            return new JSType(typeName, typeName, fieldName, Type.UNDEFINED);
        }
    }

    public Map<Element, JSType> prototypes() {
        return this.prototypes;
    }


    private JSType prototypeFor(Element element) {
        if(!prototypes.containsKey(element))
            prototypes.put(element,
                    new JSType(
                            utils.typeName(element),
                            utils.typeName(element)
                            , utils.simpleName(element)
                            , Type.UNDEFINED));
        return prototypes.get(element);
    }
}
