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
     * 合法转换：已提交(1)→已接单(2)→处理中(3)→已完结(4)
     * @param baoxiuId 报修工单ID
     * @param newStatus 目标状态
     * @param role 当前操作角色
     */
    void advanceStatus(Integer baoxiuId, Integer newStatus, String role);

}