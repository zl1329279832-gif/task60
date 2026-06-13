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
     * 分配车位：校验车位空闲 → 插入分配记录 → 更新车位状态为已占用
     * 含乐观锁防并发
     * @param cheweiId 车位id
     * @param yonghuId 用户id
     */
    void allocate(Integer cheweiId, Integer yonghuId);

    /**
     * 释放车位：删除分配记录 → 回滚车位状态为空闲
     * @param fenpeiId 分配记录id
     */
    void release(Integer fenpeiId);

}