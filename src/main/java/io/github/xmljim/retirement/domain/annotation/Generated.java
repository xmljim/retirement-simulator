package io.github.xmljim.retirement.domain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks methods that should be excluded from code coverage analysis.
 *
 * <p>JaCoCo automatically excludes methods annotated with any annotation
 * whose simple name contains "Generated" from coverage analysis. This annotation
 * is intended for methods like {@code equals()}, {@code hashCode()}, and
 * {@code toString()} that follow standard patterns and don't require
 * explicit test coverage.
 *
 * <p>Usage:
 * <pre>
 * &#64;Generated
 * &#64;Override
 * public boolean equals(Object obj) {
 *     // ...
 * }
 * </pre>
 *
 * @see <a href="https://www.jacoco.org/jacoco/trunk/doc/faq.html">JaCoCo FAQ</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface Generated {

    /**
     * Optional description of why this element is excluded from coverage.
     *
     * @return the reason for exclusion
     */
    String value() default "";
}
