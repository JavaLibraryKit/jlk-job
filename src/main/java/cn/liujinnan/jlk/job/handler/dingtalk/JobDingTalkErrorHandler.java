package cn.liujinnan.jlk.job.handler.dingtalk;


import cn.liujinnan.jlk.job.factory.JobFactory;
import cn.liujinnan.jlk.job.handler.JobPropErrorHandler;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.shardingsphere.elasticjob.infra.json.GsonFactory;
import org.apache.shardingsphere.elasticjob.infra.pojo.JobConfigurationPOJO;
import org.apache.shardingsphere.elasticjob.lite.lifecycle.api.JobConfigurationAPI;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 钉钉预警
 *
 * @author liujinan
 */
@Slf4j
public class JobDingTalkErrorHandler implements JobPropErrorHandler {

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLISECONDS = 3000;

    private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = 5000;

    private String webhook;

    private String getErrorMessage(final Throwable cause, String jobName) {
        JobConfigurationAPI jobConfigurationApi = JobFactory.configurationApi();
        JobConfigurationPOJO jobConfiguration = jobConfigurationApi.getJobConfiguration(jobName);
        DingTalkMessage dingTalkMessage = new DingTalkMessage();
        String title = String.format("<font color=#DC143C>任务%s执行失败</font>", jobName);
        StringBuilder text = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        text.append("## ").append(title).append("  \n");
        text.append("**预警时间**: ").append(LocalDateTime.now().format(formatter)).append("  \n");
        text.append("**任务名称**: ").append(jobName).append("  \n");
        text.append("**任务描述**: ").append(jobConfiguration.getDescription()).append("  \n");
        String message = cause.getMessage().length() > 200 ? cause.getMessage().substring(200) : cause.getMessage();
        text.append("**异常信息**: ").append(message).append("  \n");
        MarkDownBody markDownBody = new MarkDownBody();
        markDownBody.setText(text.toString());
        markDownBody.setTitle(title);
        dingTalkMessage.setMarkdown(markDownBody);
        return GsonFactory.getGson().toJson(dingTalkMessage);
    }

    @Override
    public void handleException(String jobName, Throwable cause) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .build();

        String errorMessageJson = getErrorMessage(cause, jobName);
        log.info("任务名{}，发送钉钉预警报文{}", jobName, errorMessageJson);
        RequestBody body = RequestBody.create(errorMessageJson,
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(webhook)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JsonObject responseMessage = GsonFactory.getGson().fromJson(response.body().string(), JsonObject.class);
                if (!"0".equals(responseMessage.get("errcode").getAsString())) {
                    log.error("任务执行异常[{}], 发送钉钉预警失败，钉钉返回消息: {}", jobName,
                            responseMessage.get("errmsg").getAsString());
                    return;
                }
                log.info("任务执行异常[{}], 发送钉钉预警成功", jobName);
            } else {
                System.out.println("请求失败: " + response.code());
            }
        } catch (IOException e) {
            log.error("任务执行异常[{}], 调用钉钉预警失败", jobName, e);
        }
    }

    @Override
    public void init(Properties props) {
        this.webhook = props.getProperty(JobDingtalkPropertiesConstants.WEBHOOK);
    }

    @Override
    public String getType() {
        return "DT";
    }

    @Override
    public List<String> propsPrefixes() {
        return Collections.singletonList(JobDingtalkPropertiesConstants.PREFIX);
    }

    @Data
    static class DingTalkMessage {
        /**
         * 固定格式markdown
         */
        private String msgtype = "markdown";

        private String title;

        private MarkDownBody markdown;

        /**
         * 是否@所有人
         */
        private boolean isAtAll;
    }

    @Data
    static class MarkDownBody {
        private String title;
        private String text;
    }
}
