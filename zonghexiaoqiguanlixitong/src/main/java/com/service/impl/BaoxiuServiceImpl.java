package com.service.impl;

import com.utils.StringUtil;
import com.service.DictionaryService;
import com.utils.ClazzDiff;
import com.entity.EIException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.lang.reflect.Field;
import java.util.*;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.utils.PageUtils;
import com.utils.Query;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import com.dao.BaoxiuDao;
import com.entity.BaoxiuEntity;
import com.service.BaoxiuService;
import com.entity.view.BaoxiuView;

/**
 * 报修 服务实现类
 */
@Service("baoxiuService")
@Transactional
public class BaoxiuServiceImpl extends ServiceImpl<BaoxiuDao, BaoxiuEntity> implements BaoxiuService {

    @Override
    public PageUtils queryPage(Map<String,Object> params) {
        Page<BaoxiuView> page =new Query<BaoxiuView>(params).getPage();
        page.setRecords(baseMapper.selectListView(page,params));
        return new PageUtils(page);
    }

    /**
     * 合法状态迁移表（当前状态 → 允许的目标状态集合）
     * 1-待处理 → 2-已接单
     * 2-已接单 → 3-处理中
     * 3-处理中 → 4-已完结
     */
    private static final Map<Integer, Set<Integer>> VALID_TRANSITIONS = new HashMap<>();
    static {
        VALID_TRANSITIONS.put(STATUS_PENDING,    new HashSet<>(Arrays.asList(STATUS_ACCEPTED)));
        VALID_TRANSITIONS.put(STATUS_ACCEPTED,   new HashSet<>(Arrays.asList(STATUS_PROCESSING)));
        VALID_TRANSITIONS.put(STATUS_PROCESSING, new HashSet<>(Arrays.asList(STATUS_COMPLETED)));
    }

    /**
     * 各步推进所允许的角色
     * 接单/处理/完结 均由物业人员操作
     */
    private static final Map<Integer, Set<String>> ALLOWED_ROLES = new HashMap<>();
    static {
        Set<String> wuyeOnly = new HashSet<>(Arrays.asList("物业人员"));
        ALLOWED_ROLES.put(STATUS_ACCEPTED,   wuyeOnly);
        ALLOWED_ROLES.put(STATUS_PROCESSING, wuyeOnly);
        ALLOWED_ROLES.put(STATUS_COMPLETED,  wuyeOnly);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceStatus(Integer baoxiuId, Integer targetStatus, String role) {
        if (baoxiuId == null || targetStatus == null) {
            throw new EIException("工单id和目标状态不能为空");
        }

        BaoxiuEntity baoxiu = selectById(baoxiuId);
        if (baoxiu == null) {
            throw new EIException("工单不存在");
        }

        Integer currentStatus = baoxiu.getBaoxiuZhuangtaiTypes();
        if (currentStatus == null) {
            throw new EIException("工单状态异常");
        }

        // 校验是否为合法的状态迁移
        Set<Integer> allowed = VALID_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new EIException("不允许从状态[" + currentStatus + "]推进到[" + targetStatus + "]");
        }

        // 校验操作角色
        Set<String> roles = ALLOWED_ROLES.get(targetStatus);
        if (roles != null && !roles.contains(role)) {
            throw new EIException("当前角色[" + role + "]无权执行此操作");
        }

        // 更新状态
        baoxiu.setBaoxiuZhuangtaiTypes(targetStatus);
        updateById(baoxiu);
    }

}
