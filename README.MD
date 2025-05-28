# JLK-Job


## 概述
基于elastic-job，提供spring注解方式创建任务

### 1. 快速开始

#### 引入maven

```xml
<dependency>
    <groupId>cn.liujinnan.java.library.kit</groupId>
    <artifactId>jlk-job</artifactId>
    <version>${version}</version>
</dependency>
```
#### 创建任务
```java
/**
 * @see cn.liujinnan.jlk.job.annotation.JobAnnotation
 */
@Component
@JobAnnotation(jobName = "name", cron = "0/10 * * * * ?", shardingTotalCount = 1, description = "bbb", shardingTotalCount = 3, shardingItemParameters = "0=a,1=b,3=c")
public class Test2 implements AbstractSimpleJob {
    @Override
    public void executeJob(ShardingContext shardingContext) {
        switch (shardingContext.getShardingItem()) {
            case 0:
                // do something by sharding item 0
                //StringUtils.equals(shardingContext.getShardingParameter(), "a") == true
                break;
            case 1:
                // do something by sharding item 1
                //StringUtils.equals(shardingContext.getShardingParameter(), "b") == true
                break;
            case 2:
                // do something by sharding item 2
                //StringUtils.equals(shardingContext.getShardingParameter(), "b") == true
                break;
            // case n: ...
        }
    }
}

```
#### 配置
```properties
# 必要配置
# zookeeper 多个逗号分隔
elasticjob.reg-center.server-lists=ip1:port1,ip2:port2
elasticjob.reg-center.namespace=namespace
```

### 2. 错误处理

#### 钉钉预警
jobErrorHandlerType="DT"，发送钉钉预警
```java
@JobAnnotation(jobErrorHandlerType="DT")


// 可在job后台单独配置作业属性
```

```properties
# 需要配置钉钉url，或者作业后台配置作业属性
elasticjob.jobs.props.dingtalk.webhook=https://oapi.dingtalk.com/robot/send?access_token=xxx
```
作业属性配置
![job属性配置](static/img/job_1.png)
