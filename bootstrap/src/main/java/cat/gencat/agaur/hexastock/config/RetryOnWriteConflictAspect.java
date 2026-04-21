package cat.gencat.agaur.hexastock.config;

import cat.gencat.agaur.hexastock.application.annotation.RetryOnWriteConflict;
import com.mongodb.MongoException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * Infrastructure-level implementation for {@link RetryOnWriteConflict}.
 *
 * <p>Retries are applied around the full use-case method, so each attempt reruns
 * the complete read-modify-write flow in a fresh transaction.</p>
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RetryOnWriteConflictAspect {

    @Around("@annotation(cat.gencat.agaur.hexastock.application.annotation.RetryOnWriteConflict)")
    public Object retry(ProceedingJoinPoint joinPoint) {
        int maxAttempts = Math.max(1, resolveRetryAnnotation(joinPoint).maxAttempts());

        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(maxAttempts)
                .retryOn(OptimisticLockingFailureException.class)
                .retryOn(ConcurrencyFailureException.class)
                .retryOn(MongoException.class)
                .traversingCauses()
                .fixedBackoff(50)
                .build();

        return retryTemplate.execute(context -> {
            try {
                return joinPoint.proceed();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Error error) {
                throw error;
            } catch (Throwable throwable) {
                throw new IllegalStateException(
                        "RetryOnWriteConflict methods must not throw checked exceptions", throwable);
            }
        });
    }

    private RetryOnWriteConflict resolveRetryAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
        RetryOnWriteConflict annotation = AnnotationUtils.findAnnotation(method, RetryOnWriteConflict.class);
        if (annotation == null) {
            throw new IllegalStateException("Missing @RetryOnWriteConflict on advised method: " + method);
        }
        return annotation;
    }
}
