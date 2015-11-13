package org.nakedpojo.utils;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.nakedpojo.utils.Commons.equalsEither;
import static org.nakedpojo.utils.Commons.isGetterName;
import static org.nakedpojo.utils.Commons.isSetterName;

public class TypeMirrorUtils {

    private final Types types;
    private final Elements elements;
    private final Messager messager;

    private final TypeMirror STRING_TYPE;

    public final Comparator<Element> TYPE_NAME_COMPARATOR = new Comparator<Element>() {
        @Override
        public int compare(Element o1, Element o2) {
            return typeName(o1).compareTo(typeName(o2));
        }
    };

    public TypeMirrorUtils(Types types, Elements elements, Messager messager) {
        this.types = types;
        this.elements = elements;
        this.messager = messager;

        this.STRING_TYPE = elements.getTypeElement(java.lang.String.class.getCanonicalName()).asType();
    }

    public  boolean isNumeric(Element element) {
        return equalsEither(element.asType().getKind(),
                TypeKind.SHORT,
                TypeKind.INT,
                TypeKind.LONG,
                TypeKind.FLOAT,
                TypeKind.DOUBLE);
    }

    public  boolean isPublicField(Element e) {
        return e.getKind().isField()
                && e.getModifiers().contains(Modifier.PUBLIC);
    }

    public boolean isPrimitive(Element element) {
        return element.asType().getKind().isPrimitive()
                || types.isSameType(element.asType(), STRING_TYPE);
    }

    public boolean isString(Element element) {
        return element.asType().getKind().equals(TypeKind.CHAR)
                || types.isSameType(element.asType(), STRING_TYPE);
    }

    public boolean isBoolean(Element element) {
        return element.asType().getKind().equals(TypeKind.BOOLEAN);
    }

    public boolean isByte(Element element) {
        return element.asType().getKind().equals(TypeKind.BYTE);
    }

    public boolean isEnum(Element element)
    {
        return element.getKind().equals(ElementKind.ENUM);
    }

    public boolean isIterable(Element element) {
        TypeElement list = elements.getTypeElement(java.lang.Iterable.class.getCanonicalName());
        return element.asType().getKind().equals(TypeKind.ARRAY)
                || types.isSubtype(element.asType(), types.getDeclaredType(list));
    }

    public boolean isClass(Element element) {
        return element.getKind().isClass();
    }

    public String fieldName(Element element) {
        return element.getSimpleName().toString();
    }

    public String typeName(Element element) {
        return element.asType().getKind().equals(TypeKind.EXECUTABLE) ?
                ((ExecutableElement)element).getReturnType().toString()
                : element.toString();
    }

    public String simpleName(Element element) {
        return element.asType().getKind().equals(TypeKind.EXECUTABLE) ?
                ((ExecutableElement)element).getSimpleName().toString()
                : element.getSimpleName().toString();
    }

    public List<Element> publicFields(Element element) {
        List<Element> publicFields = new ArrayList<Element>();
        for(Element enclosed: element.getEnclosedElements()) {
            if(isPublicField(enclosed)) {
                publicFields.add(enclosed);
            }
        }
        return publicFields;
    }

    public List<Element> nestedClasses(Element element) {
        List<Element> nestedClasses = new ArrayList<Element>();
        for(Element enclosed: element.getEnclosedElements()) {
            if(enclosed.getKind().isClass() || enclosed.getKind().isInterface()) {
                nestedClasses.add(enclosed);
            }
        }
        return nestedClasses;
    }

    public boolean isGetter(Element element) {
        return isGetterName(fieldName(element))
                && element.asType().getKind().equals(TypeKind.EXECUTABLE)
                && element.getModifiers().contains(Modifier.PUBLIC)
                && !((ExecutableElement) element).getReturnType().getKind().equals(TypeKind.VOID)
                && ((ExecutableElement) element).getParameters().isEmpty();
    }

    public boolean isSetter(Element element) {
        return isGetterName(fieldName(element))
                && element.asType().getKind().equals(TypeKind.EXECUTABLE)
                && element.getModifiers().contains(Modifier.PUBLIC)
                && ((ExecutableElement) element).getReturnType().getKind().equals(TypeKind.VOID)
                && ((ExecutableElement) element).getParameters().size() == 1;
    }

    public List<ExecutableElement> getters(Element element) {
        List<ExecutableElement> getters = new ArrayList<>();
        for(Element enclosed: element.getEnclosedElements()) {
            if(isGetter(enclosed)) getters.add((ExecutableElement)enclosed);
        }
        return getters;
    }

    public List<ExecutableElement> setters(Element element) {
        List<ExecutableElement> setters = new ArrayList<>();
        for(Element enclosed: element.getEnclosedElements()) {
            if(isSetter(enclosed)) setters.add((ExecutableElement)enclosed);
        }
        return setters;
    }
}
