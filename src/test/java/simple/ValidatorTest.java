package simple;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import io.micronaut.validation.validator.constraints.ConstraintValidator;
import io.micronaut.validation.validator.constraints.ConstraintValidatorContext;
import jakarta.inject.Inject;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.stream.Collectors;

@MicronautTest(startApplication = false)
class ValidatorTest {
	@Inject Validator validator;

    /**
	 * Expectation here is that we have an object, with three properties, all set to "null"
	 * we have the standard @NotNull annotation set on all fields, so we expect the standard violation message returned for each field
	 * "car" and "truck" also have a custom annotation added with a message dynamically set in the Validator
	 * and "bike" has a custom annotation added with a message read from the annotation
	 *
	 * We expect 6 constraint violations to be returned, however, due to disableDefaultConstraintViolation not being reset between
	 * each test, we actually only end up with 3: [car: FromValidator, truck: FromValidator, car: must not be null].
	 */
	@Test
	void testAllNullValues() {
		
		var results = validator.validate(new ValueObject(null, null, null));
		var violations =  results.stream().map(x -> x.getPropertyPath() + ": " + x.getMessage()).collect(Collectors.toSet());
		var expected = Set.of("bike: must not be null", "car: must not be null", "truck: must not be null",
		        "bike: FromAnnotation", "car: FromValidator", "truck: FromValidator");
		assertEquals(expected, Set.copyOf(violations));
	}

    @Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)	
	@Constraint(validatedBy = NotNullMessageFromAnnotationValidator.class)
	public @interface NotNullMessageFromAnnotation {
		String message() default "FromAnnotation";
		Class<?>[] groups() default {};
		Class<? extends Payload>[] payload() default {};
	}

    @Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)	
	@Constraint(validatedBy = NotNullMessageFromValidatorValidator.class)
	public @interface NotNullMessageFromValidator {
		String message() default "ignored";
		Class<?>[] groups() default {};
		Class<? extends Payload>[] payload() default {};
	}

    @Introspected
	public static class NotNullMessageFromAnnotationValidator implements ConstraintValidator<NotNullMessageFromAnnotation, String> {
		@Override
		public boolean isValid(String value,
							   @NonNull AnnotationValue<NotNullMessageFromAnnotation> annotationMetadata,
							   @NonNull ConstraintValidatorContext context) {
			return value != null;
	    }
	}

    @Introspected
	public static class NotNullMessageFromValidatorValidator implements ConstraintValidator<NotNullMessageFromValidator, String> {
		@Override
		public boolean isValid(String value,
							   @NonNull AnnotationValue<NotNullMessageFromValidator> annotationMetadata,
							   @NonNull ConstraintValidatorContext context) {
		    if (value == null) {
				context.disableDefaultConstraintViolation();
				context.buildConstraintViolationWithTemplate("FromValidator").addConstraintViolation();
				return false;
			}
			return true;
	    }
	}

    @Introspected
	public record ValueObject(@NotNullMessageFromValidator @NotNull String car,
							  @NotNullMessageFromAnnotation @NotNull String bike,
							  @NotNullMessageFromValidator @NotNull String truck) {}	
}
