package cn.liujinnan.jlk.job.handler.dingtalk;

import org.apache.shardingsphere.elasticjob.error.handler.JobErrorHandlerPropertiesValidator;
import org.apache.shardingsphere.elasticjob.infra.validator.JobPropertiesValidateRule;

import java.util.Properties;

/**
 * @author ljn
 * @version 1.0
 * @since  2025-05-06 17:02
 */
public class JobDingTalkErrorHandlerPropertiesValidator implements JobErrorHandlerPropertiesValidator {

    @Override
    public void validate(Properties props) {
        JobPropertiesValidateRule.validateIsRequired(props, JobDingtalkPropertiesConstants.WEBHOOK);
    }

    @Override
    public String getType() {
        return "DT";
    }
}
