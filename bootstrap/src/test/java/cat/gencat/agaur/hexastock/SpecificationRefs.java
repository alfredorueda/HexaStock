package cat.gencat.agaur.hexastock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for {@link SpecificationRef @SpecificationRef}.
 * Required by Java's {@code @Repeatable} mechanism — allows a single test method
 * to be linked to multiple specification scenarios.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SpecificationRefs {
    SpecificationRef[] value();
}
