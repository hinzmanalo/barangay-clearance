package com.barangay.clearance.shared.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Executor;

/**
 * Configures the dedicated async thread pool used by {@link AuditService}.
 *
 * <p>
 * Separating audit writes onto their own pool prevents audit latency from
 * bleeding into the main request thread pool and provides clear tuning knobs
 * for the audit workload independently of other async tasks.
 * </p>
 *
 * <p>
 * Pool characteristics:
 * </p>
 * <ul>
 * <li>Core pool size: 2 — handles normal steady-state audit traffic.</li>
 * <li>Max pool size: 5 — absorbs brief traffic spikes.</li>
 * <li>Queue capacity: 500 — buffers bursts before rejecting tasks.</li>
 * <li>Caller-runs policy (handled by {@link AuditService#log} catch block) —
 * if the pool is saturated, {@code RejectedExecutionException} is caught
 * and the error is logged rather than propagated.</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AuditAsyncConfig {

    /**
     * Named task executor injected via {@code @Async("auditTaskExecutor")}.
     *
     * @return configured thread pool executor for audit writes
     */
    @Bean(name = "auditTaskExecutor")
    public Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-pool-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // Propagate the HTTP request context so that AuditService.resolveIpAddress()
        // can read X-Forwarded-For / remoteAddr from the originating request.
        executor.setTaskDecorator(runnable -> {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                return runnable;
            }
            return () -> {
                try {
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                    runnable.run();
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            };
        });

        executor.initialize();
        return executor;
    }
}
