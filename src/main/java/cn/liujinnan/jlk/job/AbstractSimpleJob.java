package cn.liujinnan.jlk.job;


import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.apache.shardingsphere.elasticjob.simple.job.SimpleJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StopWatch;

import java.util.UUID;

/**
 * 任务抽象类。 继承该类，任务失败可发送通知。日志打印mdc记录
 *
 * @author liujinnan
 */
public abstract class AbstractSimpleJob implements SimpleJob {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Value("${vegetable.dog.job.mdc.key:TRACE_ID}")
    private String mdcKey;

    @Override
    public void execute(ShardingContext shardingContext) {
        MDC.put(mdcKey, UUID.randomUUID().toString());
        StopWatch stopWatch = new StopWatch();
        String className = this.getClass().getSimpleName();
        try {
            stopWatch.start();
            log.info("Task {} starts running", className);
            executeJob(shardingContext);
            stopWatch.stop();
            log.info("Task {} is finished, it takes {}ms", className, stopWatch.getTotalTimeMillis());
        } finally {
            MDC.remove(mdcKey);
        }
    }

    /**
     * Subclass implementation method to perform tasks.
     *
     * 子类实现方法执行任务
     * @param shardingContext Task context
     */
    protected abstract void executeJob(ShardingContext shardingContext);

}
