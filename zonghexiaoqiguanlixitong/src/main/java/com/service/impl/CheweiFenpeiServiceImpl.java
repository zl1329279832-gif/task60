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
import com.dao.CheweiFenpeiDao;
import com.dao.CheweiDao;
import com.entity.CheweiFenpeiEntity;
import com.entity.CheweiEntity;
import com.service.CheweiFenpeiService;
import com.entity.view.CheweiFenpeiView;

/**
 * 车位分配 服务实现类
 */
@Service("cheweiFenpeiService")
@Transactional
public class CheweiFenpeiServiceImpl extends ServiceImpl<CheweiFenpeiDao, CheweiFenpeiEntity> implements CheweiFenpeiService {

    /** 车位状态：已占用 */
    private static final int CHEWEI_STATUS_OCCUPIED = 1;
    /** 车位状态：空闲 */
    private static final int CHEWEI_STATUS_FREE = 2;

    @Autowired
    private CheweiDao cheweiDao;

    @Override
    public PageUtils queryPage(Map<String,Object> params) {
        Page<CheweiFenpeiView> page =new Query<CheweiFenpeiView>(params).getPage();
        page.setRecords(baseMapper.selectListView(page,params));
        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void allocateChewei(CheweiFenpeiEntity fenpei) {
        // 悲观锁：锁定车位行，防止并发分配同一车位
        CheweiEntity chewei = cheweiDao.selectByIdForUpdate(fenpei.getCheweiId());
        if (chewei == null) {
            throw new RuntimeException("车位不存在");
        }
        if (chewei.getCheweiZhuangtaiTypes() != null && chewei.getCheweiZhuangtaiTypes() == CHEWEI_STATUS_OCCUPIED) {
            throw new RuntimeException("该车位已被分配，不能重复分配");
        }

        // 插入分配记录（只插入一次）
        fenpei.setCreateTime(new Date());
        baseMapper.insert(fenpei);

        // 更新车位状态为已占用
        CheweiEntity update = new CheweiEntity();
        update.setId(chewei.getId());
        update.setCheweiZhuangtaiTypes(CHEWEI_STATUS_OCCUPIED);
        cheweiDao.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseChewei(Integer[] ids) {
        List<CheweiFenpeiEntity> fenpeiList = baseMapper.selectBatchIds(Arrays.asList(ids));
        // 先收集需要释放的车位ID
        Set<Integer> cheweiIds = new HashSet<>();
        for (CheweiFenpeiEntity fenpei : fenpeiList) {
            if (fenpei.getCheweiId() != null) {
                cheweiIds.add(fenpei.getCheweiId());
            }
        }
        // 删除分配记录
        baseMapper.deleteBatchIds(Arrays.asList(ids));
        // 回滚车位状态为空闲
        for (Integer cheweiId : cheweiIds) {
            // 检查该车位是否还有其他分配记录（防止一个车位被多条记录关联时误释放）
            int remaining = baseMapper.selectCount(
                new com.baomidou.mybatisplus.mapper.EntityWrapper<CheweiFenpeiEntity>()
                    .eq("chewei_id", cheweiId)
            );
            if (remaining == 0) {
                CheweiEntity update = new CheweiEntity();
                update.setId(cheweiId);
                update.setCheweiZhuangtaiTypes(CHEWEI_STATUS_FREE);
                cheweiDao.updateById(update);
            }
        }
    }

}
