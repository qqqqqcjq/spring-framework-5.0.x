package maintest.genericType;

import jdk.nashorn.internal.objects.annotations.Function;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * @date 2022/7/19 15:54
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class TypeVariableDemo {

    public static void main(String[] args) {
        TypeVariable<Class<TypeVariableSample>>[] typeParameters = TypeVariableSample.class.getTypeParameters();

        for (int i = 0; i < typeParameters.length; i++) {
            TypeVariable<Class<TypeVariableSample>> typeVariable = typeParameters[i];
            System.out.println(typeVariable.getName());
            System.out.println(typeVariable.getGenericDeclaration());
            System.out.println();

            Type[] bounds = typeVariable.getBounds();
            for (int j = 0; j < bounds.length; j++) {
                Type bound = bounds[j];
                System.out.println(bound.getTypeName());
            }
            System.out.println();

            AnnotatedType[] annotatedBounds = typeVariable.getAnnotatedBounds();
            for (int j = 0; j < annotatedBounds.length; j++) {
                StringBuilder sb = new StringBuilder();
                AnnotatedType annotatedType = annotatedBounds[j];
                System.out.println("AnnotatedType:" + annotatedType.getType());
                Annotation[] annotations = annotatedType.getDeclaredAnnotations();
                for (int k = 0; k < annotations.length; k++) {
                    Annotation annotation = annotations[k];
                    sb.append(annotation);
                }
                sb.append(" "+ annotatedType.getType().getTypeName());
                System.out.println(sb.toString());
            }
        }
    }
}

class TypeVariableSample<T extends @Location("Serializable") Serializable & @Location("Comparable<T>") Comparable<T>> {

}

