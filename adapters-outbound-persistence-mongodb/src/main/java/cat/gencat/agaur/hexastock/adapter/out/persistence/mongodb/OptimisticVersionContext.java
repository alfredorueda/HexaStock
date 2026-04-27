package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Transaction-scoped cache of MongoDB {@code @Version} values.
 *
 * <p>When a repository reads a document it calls {@link #remember} to store the
 * version in the active transaction context. On the subsequent save the repository
 * calls {@link #resolve} which returns the remembered version so that Spring Data
 * MongoDB can perform the optimistic-lock check. Outside of a transaction (e.g.
 * during a retry without an active unit-of-work) the fallback supplier is used
 * instead, which typically issues a fresh {@code findById}.</p>
 *
 * <p>Each repository should hold its own instance, constructed with its own class
 * as the owner so that the transaction resource keys never collide.</p>
 */
public final class OptimisticVersionContext {

    private final String resourceKey;

    public OptimisticVersionContext(Class<?> owner) {
        this.resourceKey = owner.getName() + ".version-context";
    }

    public void remember(String id, Long version) {
        if (!isTransactionActive()) {
            return;
        }
        getOrCreateMap().put(id, version);
    }

    /**
     * Returns the version captured at read time, or the result of {@code fallback}
     * if no transaction context is available.
     */
    public Long resolve(String id, Supplier<Optional<Long>> fallback) {
        return getFromMap(id).or(fallback).orElse(null);
    }

    private Optional<Long> getFromMap(String id) {
        if (!isTransactionActive()) {
            return Optional.empty();
        }
        return Optional.ofNullable(getOrCreateMap().get(id));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> getOrCreateMap() {
        Map<String, Long> map =
                (Map<String, Long>) TransactionSynchronizationManager.getResource(resourceKey);
        if (map != null) {
            return map;
        }
        Map<String, Long> newMap = new HashMap<>();
        TransactionSynchronizationManager.bindResource(resourceKey, newMap);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(resourceKey)) {
                    TransactionSynchronizationManager.unbindResource(resourceKey);
                }
            }
        });
        return newMap;
    }

    private boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();
    }
}