package cn.liujinnan.jlk.job.register;


import cn.liujinnan.jlk.job.annotation.JobAnnotation;
import cn.liujinnan.jlk.job.annotation.JobProp;
import cn.liujinnan.jlk.job.handler.JobPropErrorHandler;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.elasticjob.api.ElasticJob;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.error.handler.JobErrorHandler;
import org.apache.shardingsphere.elasticjob.infra.spi.ElasticJobServiceLoader;
import org.apache.shardingsphere.elasticjob.lite.api.bootstrap.impl.ScheduleJobBootstrap;
import org.apache.shardingsphere.elasticjob.reg.zookeeper.ZookeeperRegistryCenter;
import org.apache.shardingsphere.elasticjob.tracing.api.TracingConfiguration;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 任务注册
 *
 * @author liujinan
 */
@Configuration
@ConditionalOnExpression("'${elasticjob.reg-center.server-lists}'.length() > 0 && '${elasticjob.reg-center.namespace}'.length()>0")
public class JobRegister implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Environment environment;

    @Override
    public void run(String... args) throws Exception {
        ZookeeperRegistryCenter zookeeperRegistryCenter = applicationContext.getBean(ZookeeperRegistryCenter.class);
        Map<String, ElasticJob> map = applicationContext.getBeansOfType(ElasticJob.class);
        Map<String, Object> commonJobProp = getPropStartWith("elasticjob.jobs.props.");
        // 后面 ElasticJobServiceLoader.getCachedTypedServiceInstance 需要用到
        ElasticJobServiceLoader.registerTypedService(JobErrorHandler.class);

        //数据源配置
        TracingConfiguration<?> tracingConfiguration = getTracingConfiguration();
        for (Map.Entry<String, ElasticJob> entry : map.entrySet()) {
            ElasticJob elasticJob = entry.getValue();
            if (AopUtils.isAopProxy(elasticJob)) {
                try {
                    elasticJob = (ElasticJob) ((Advised) elasticJob).getTargetSource().getTarget();
                } catch (Exception ignored) {
                }
            }
            if (Objects.isNull(elasticJob)) {
                continue;
            }
            // @ElasticJobConfiguration
            JobAnnotation jobAnnotation = elasticJob.getClass().getAnnotation(JobAnnotation.class);
            if (Objects.isNull(jobAnnotation)) {
                continue;
            }

            String jobName = StringUtils.isBlank(jobAnnotation.jobName()) ? elasticJob.getClass().getSimpleName() : jobAnnotation.jobName();
            //job任务配置

            JobConfiguration.Builder builder = JobConfiguration.newBuilder(jobName, jobAnnotation.shardingTotalCount())
                    .shardingItemParameters(jobAnnotation.shardingItemParameters())
                    .cron(Strings.isNullOrEmpty(jobAnnotation.cron()) ? null : jobAnnotation.cron())
                    .timeZone(Strings.isNullOrEmpty(jobAnnotation.timeZone()) ? null : jobAnnotation.timeZone())
                    .jobParameter(jobAnnotation.jobParameter())
                    .monitorExecution(jobAnnotation.monitorExecution())
                    .failover(jobAnnotation.failover())
                    .misfire(jobAnnotation.misfire())
                    .maxTimeDiffSeconds(jobAnnotation.maxTimeDiffSeconds())
                    .reconcileIntervalMinutes(jobAnnotation.reconcileIntervalMinutes())
                    .jobShardingStrategyType(Strings.isNullOrEmpty(jobAnnotation.jobShardingStrategyType()) ? null : jobAnnotation.jobShardingStrategyType())
                    .jobExecutorServiceHandlerType(Strings.isNullOrEmpty(jobAnnotation.jobExecutorServiceHandlerType()) ? null : jobAnnotation.jobExecutorServiceHandlerType())
                    .jobErrorHandlerType(Strings.isNullOrEmpty(jobAnnotation.jobErrorHandlerType()) ? null : jobAnnotation.jobErrorHandlerType())
                    .jobListenerTypes(jobAnnotation.jobListenerTypes())
                    .description(jobAnnotation.description())
                    .disabled(jobAnnotation.disabled())
                    .overwrite(jobAnnotation.overwrite());

            // job properties
            Map<String, String> jobProp = getJobProp(jobAnnotation, commonJobProp);
            jobProp.forEach(builder::setProperty);

            JobConfiguration jobConfiguration = builder.build();
            if (Objects.nonNull(tracingConfiguration)) {
                // 数据源。存储执行记录
                jobConfiguration.getExtraConfigurations().add(tracingConfiguration);
            }
            // 创建任务
            ScheduleJobBootstrap scheduleJobBootstrap =
                    new ScheduleJobBootstrap(zookeeperRegistryCenter, elasticJob, jobConfiguration);
            scheduleJobBootstrap.schedule();
        }
    }

    private TracingConfiguration<?> getTracingConfiguration() {
        Map<String, TracingConfiguration> dataSourceConfig = applicationContext.getBeansOfType(TracingConfiguration.class);
        if (!CollectionUtils.isEmpty(dataSourceConfig) && dataSourceConfig.values().size() == 1) {
            return dataSourceConfig.values().stream().findFirst().get();
        }
        return null;
    }

    /**
     * Loading task common configuration.
     * All configurations starting with 'elasticjob.jobs.props.'
     * 加载公共配置。'elasticjob.jobs.props.' 开头
     * 返回
     * @return map
     */
    private Map<String, Object> getPropStartWith(String startWith) {
        // 加载配置。'elasticjob.jobs.props.' 或 'elasticjob.jobs.${jobName}.props.'开头
        Map<String, Object> properties = new HashMap<>();
        if (environment instanceof ConfigurableEnvironment) {
            for (PropertySource<?> propertySource : ((ConfigurableEnvironment) environment).getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    for (String name : ((EnumerablePropertySource<?>) propertySource).getPropertyNames()) {
                        if (Objects.nonNull(name) && name.startsWith("elasticjob.jobs.props.")) {
                            properties.put(name.replace("elasticjob.jobs.props.", ""), propertySource.getProperty(name));
                        }
                    }
                }
            }
        }
        return properties;
    }

    private Map<String, String> getJobProp(JobAnnotation jobAnnotation, Map<String, Object> commonJobProp) {

        // 优先级  jobName属性配置(elasticjob.jobs.${jobName}.props.) > 注解配置 > 全局job属性配置(elasticjob.jobs.props.)

        // 当前任务的属性前缀
        List<String> propsPrefixes = new ArrayList<>();

        // 加载错误处理类需要的属性前缀。 全局job属性配置(elasticjob.jobs.props.)，error typ 全局配置
        String errorType = jobAnnotation.jobErrorHandlerType();
        if (StringUtils.isNotBlank(errorType)) {
            // 获取配置的告警策略需要的配置前缀
            ElasticJobServiceLoader.getCachedTypedServiceInstance(JobErrorHandler.class, errorType).ifPresent(jobErrorHandler -> {
                if (jobErrorHandler instanceof JobPropErrorHandler) {
                    JobPropErrorHandler jobPropErrorHandler = (JobPropErrorHandler)jobErrorHandler;
                    List<String> prefixes = jobPropErrorHandler.propsPrefixes();
                    if (!CollectionUtils.isEmpty(prefixes)){
                        propsPrefixes.addAll(prefixes);
                    }
                }
            });
        }

        // 当前任务的prop配置。 根据优先级倒序加载，优先级低的将被覆盖
        Map<String, String> jobPropMap = new HashMap<>();
        // 1. 全局job属性配置(elasticjob.jobs.props.)
        commonJobProp.forEach((key, val) -> {
            if (propsPrefixes.stream().anyMatch(key::startsWith)) {
                jobPropMap.put(key, (String) val);
            }
        });

       // 2. 注解配置
        JobProp[] props = jobAnnotation.props();
        if (props != null) {
            for (JobProp prop : props) {
                jobPropMap.put(prop.key(), prop.value());
            }
        }
        // 3. jobName属性配置(elasticjob.jobs.${jobName}.props.)
        String jobProps = String.format("elasticjob.jobs.%s.props.", jobAnnotation.jobName());
        Map<String, Object> propStartWith = getPropStartWith(jobProps);
        propStartWith.forEach((key, val) -> {
            jobPropMap.put(key, (String) val);
        });

        return jobPropMap;
    }
}
