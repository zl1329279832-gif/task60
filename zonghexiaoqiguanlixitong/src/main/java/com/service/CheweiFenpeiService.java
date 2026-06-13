package com.service;

import com.baomidou.mybatisplus.service.IService;
import com.utils.PageUtils;
import com.entity.CheweiFenpeiEntity;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import java.util.List;

/**
 * 车位分配 服务类
 */
public interface CheweiFenpeiService extends IService<CheweiFenpeiEntity> {

    /**
    * @param params 查询参数
    * @return 带分页的查询出来的数据
    */
     PageUtils queryPage(Map<String, Object> params);

    /**
     * 分配车位（带并发控制）
     * 检查车位空闲后插入分配记录并将车位标记为已占用
     */
    void allocateChewei(CheweiFenpeiEntity fenpei);

    /**
     * 释放车位
     * 删除分配记录并将车位状态回滚为空闲
     */
    void releaseChewei(Integer[] ids);

}