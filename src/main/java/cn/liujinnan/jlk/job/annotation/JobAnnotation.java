package cn.liujinnan.jlk.job.annotation;

import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

/**
 * job annotation
 *
 * @author liujinnan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface JobAnnotation {

    /**
     * The default value is the simple class name. For example, class TestJob.java, jobName="TestJob"
     * 默认类名. 例如： TestJob.java , 则 jobName = "TestJob"
     * @return jobName
     */
    String jobName() default "";

    /**
     * Cron expression.
     * cron表达式
     * @return Cron expression.
     */
    String cron();

    String timeZone() default "";


    /**
     * Job parameter.
     * 作业自定义参数
     * @return Job parameter
     */
    String jobParameter() default "";

    /**
     * Job description.
     * 任务描述
     *
     * @return Job description.
     */
    String description() default "";

    /**
     * Job properties.
     * 独属于当前任务的属性
     *
     * @return Job properties
     */
    JobProp[] props() default {};

    /**
     * Sharding total count.
     * Returns: sharding total count
     * 默认分片数量 1
     *
     * @return Sharding total count
     */
    int shardingTotalCount() default 1;

    /**
     * Set mapper of sharding items and sharding parameters.
     * Sharding item start from zero, cannot equal to great than sharding total count. For example: 0=a,1=b,2=c
     * 下标从0开始
     * 分片样例："0=a,1=b,2=c"
     *
     * @return shardingItemParameters
     */
    String shardingItemParameters() default "";

    /**
     * Set job error handler type.
     * 任务失败预警通知
     * @return handler type
     */
    String jobErrorHandlerType() default "";

    /**
     * Set whether disabled job when start
     * 启动后是否禁用任务
     * @return disabled
     */
    boolean disabled() default false;

    /**
     * Set whether overwrite local configuration to registry center when job startup.
     * 作业启动时是否覆盖本地配置到注册中心
     * overwrite=true, 以代码中的任务配置为准，每次重启都会重写注册中心
     * overwrite=false, 以注册中心的任务配置为准，即代码中调整cron等项不生效，任务以注册中心中的cron执行
     *
     * @return boolean
     */
    boolean overwrite() default false;

    boolean monitorExecution() default false;

    /**
     * 失效转移
     *
     * @return boolean
     */
    boolean failover() default false;

    /**
     * 是否开启错过任务重新执行
     *
     * @return misfire
     */
    boolean misfire() default false;


    /**
     * The maximum value for time difference between server and registry center in seconds.
     *
     * @return max time diff seconds
     */
    int maxTimeDiffSeconds() default -1;

    /**
     * Service scheduling interval in minutes for repairing job server inconsistent state.
     *
     * @return reconcile interval minutes
     */
    int reconcileIntervalMinutes() default 10;

    /**
     * Job sharding strategy type.
     *
     * @return job sharding strategy type
     */
    String jobShardingStrategyType() default "";

    /**
     * Job thread pool handler type.
     *
     * @return job executor service handler type
     */
    String jobExecutorServiceHandlerType() default "";

    /**
     * Job listener types.
     *
     * @return job listener types
     */
    String[] jobListenerTypes() default {};
}
