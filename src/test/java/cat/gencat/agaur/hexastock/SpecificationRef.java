package cat.gencat.agaur.hexastock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(SpecificationRefs.class)
public @interface SpecificationRef {
    String value();
    TestLevel level();
    String feature() default "";
}
