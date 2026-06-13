package com.service;

import com.baomidou.mybatisplus.service.IService;
import com.utils.PageUtils;
import com.entity.BaoxiuEntity;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import java.util.List;

/**
 * 报修 服务类
 */
public interface BaoxiuService extends IService<BaoxiuEntity> {

    /**
    * @param params 查询参数
    * @return 带分页的查询出来的数据
    */
     PageUtils queryPage(Map<String, Object> params);

    /**
     * 推进报修工单状态
     * 状态链：1-待处理 → 2-已接单 → 3-处理中 → 4-已完结
     * @param baoxiuId    工单id
     * @param targetStatus 目标状态
     * @param role         操作人角色
     */
    void advanceStatus(Integer baoxiuId, Integer targetStatus, String role);

    /** 状态常量 */
    int STATUS_PENDING    = 1;  // 待处理
    int STATUS_ACCEPTED   = 2;  // 已接单
    int STATUS_PROCESSING = 3;  // 处理中
    int STATUS_COMPLETED  = 4;  // 已完结

}