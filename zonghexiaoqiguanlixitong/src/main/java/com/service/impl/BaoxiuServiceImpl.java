package com.service.impl;

import com.utils.StringUtil;
import com.service.DictionaryService;
import com.utils.ClazzDiff;
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

    /** 报修状态：已提交 */
    public static final int STATUS_SUBMITTED = 1;
    /** 报修状态：已接单 */
    public static final int STATUS_ACCEPTED = 2;
    /** 报修状态：处理中 */
    public static final int STATUS_PROCESSING = 3;
    /** 报修状态：已完结 */
    public static final int STATUS_COMPLETED = 4;

    /**
     * 合法的状态转换表：key=当前状态，value=允许的下一状态
     */
    private static final Map<Integer, Integer> VALID_TRANSITIONS = new HashMap<>();
    static {
        VALID_TRANSITIONS.put(STATUS_SUBMITTED, STATUS_ACCEPTED);    // 已提交 → 已接单
        VALID_TRANSITIONS.put(STATUS_ACCEPTED, STATUS_PROCESSING);   // 已接单 → 处理中
        VALID_TRANSITIONS.put(STATUS_PROCESSING, STATUS_COMPLETED);  // 处理中 → 已完结
    }

    @Override
    public PageUtils queryPage(Map<String,Object> params) {
        Page<BaoxiuView> page =new Query<BaoxiuView>(params).getPage();
        page.setRecords(baseMapper.selectListView(page,params));
        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceStatus(Integer baoxiuId, Integer newStatus, String role) {
        // 只有物业人员可以推进工单状态
        if (!"物业人员".equals(role)) {
            throw new RuntimeException("只有物业人员可以推进工单状态");
        }

        BaoxiuEntity baoxiu = baseMapper.selectById(baoxiuId);
        if (baoxiu == null) {
            throw new RuntimeException("报修工单不存在");
        }

        Integer currentStatus = baoxiu.getBaoxiuZhuangtaiTypes();
        Integer allowedNext = VALID_TRANSITIONS.get(currentStatus);

        if (allowedNext == null) {
            throw new RuntimeException("当前工单状态不允许继续推进");
        }
        if (!allowedNext.equals(newStatus)) {
            throw new RuntimeException("非法状态转换：当前状态" + currentStatus + "只能转到" + allowedNext + "，不能转到" + newStatus);
        }

        BaoxiuEntity update = new BaoxiuEntity();
        update.setId(baoxiuId);
        update.setBaoxiuZhuangtaiTypes(newStatus);
        baseMapper.updateById(update);
    }

}
