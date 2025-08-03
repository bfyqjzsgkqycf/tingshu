package com.lsj.tingshu.dispatch.job;

import com.lsj.tingshu.dispatch.mapper.XxlJobLogMapper;
import com.lsj.tingshu.model.dispatch.XxlJobLog;
import com.lsj.tingshu.search.client.SearchFeignClient;
import com.lsj.tingshu.user.client.UserInfoFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchJobHandler {

    @Autowired
    private SearchFeignClient searchFeignClient;

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    /**
     * 1.同步es的排行榜数据到redis
     * 2.user_info表中  is_vip(用户的身份)  vip_expire_time
     */
    @XxlJob("updateRank")
    public void preElasticSearchAlbumToRedis() {

        System.out.println("更新排行榜");
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());   //xxl-job中的任务id

        Long startTime = System.currentTimeMillis();
        try {
            searchFeignClient.preElasticSearchAlbumToRedis();
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
        } finally {
            Long endTime = System.currentTimeMillis();
            xxlJobLog.setTimes((int) (endTime - startTime));
        }
        xxlJobLogMapper.insert(xxlJobLog);

    }


    @XxlJob("updateExpireTimeHandler")
    public void updateExpireTimeVip() {

        System.out.println("更新过期身份的vip");
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());   //xxl-job中的任务id

        Long startTime = System.currentTimeMillis();
        try {
            userInfoFeignClient.updateExpireTimeVip();
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
        } finally {
            Long endTime = System.currentTimeMillis();
            xxlJobLog.setTimes((int) (endTime - startTime));
        }

    }

}