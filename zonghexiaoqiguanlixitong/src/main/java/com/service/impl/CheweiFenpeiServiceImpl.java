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
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
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
import com.entity.CheweiFenpeiEntity;
import com.entity.CheweiEntity;
import com.service.CheweiFenpeiService;
import com.service.CheweiService;
import com.entity.view.CheweiFenpeiView;

/**
 * 车位分配 服务实现类
 */
@Service("cheweiFenpeiService")
@Transactional
public class CheweiFenpeiServiceImpl extends ServiceImpl<CheweiFenpeiDao, CheweiFenpeiEntity> implements CheweiFenpeiService {

    @Autowired
    private CheweiService cheweiService;

    /**
     * 车位状态常量
     * 1=空闲  2=已占用
     */
    private static final int CHEWEI_STATUS_FREE = 1;
    private static final int CHEWEI_STATUS_OCCUPIED = 2;

    @Override
    public PageUtils queryPage(Map<String,Object> params) {
        Page<CheweiFenpeiView> page =new Query<CheweiFenpeiView>(params).getPage();
        page.setRecords(baseMapper.selectListView(page,params));
        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void allocate(Integer cheweiId, Integer yonghuId) {
        if (cheweiId == null || yonghuId == null) {
            throw new EIException("车位id和用户id不能为空");
        }

        // 1. 查询车位是否存在
        CheweiEntity chewei = cheweiService.selectById(cheweiId);
        if (chewei == null) {
            throw new EIException("车位不存在");
        }

        // 2. 乐观锁：仅当车位状态为空闲时才更新为已占用，防并发分配同一车位
        CheweiEntity lockEntity = new CheweiEntity();
        lockEntity.setCheweiZhuangtaiTypes(CHEWEI_STATUS_OCCUPIED);
        boolean casOk = cheweiService.update(lockEntity,
                new EntityWrapper<CheweiEntity>()
                        .eq("id", cheweiId)
                        .eq("chewei_zhuangtai_types", CHEWEI_STATUS_FREE)
        );
        if (!casOk) {
            throw new EIException("该车位已被占用或不存在，分配失败");
        }

        // 3. 检查是否已存在相同分配记录（防止重复插入）
        Wrapper<CheweiFenpeiEntity> dupWrapper = new EntityWrapper<CheweiFenpeiEntity>()
                .eq("chewei_id", cheweiId)
                .eq("yonghu_id", yonghuId);
        CheweiFenpeiEntity existing = selectOne(dupWrapper);
        if (existing != null) {
            // 回滚车位状态
            rollbackCheweiStatus(cheweiId);
            throw new EIException("该用户已分配过此车位");
        }

        // 4. 插入分配记录
        CheweiFenpeiEntity fenpei = new CheweiFenpeiEntity();
        fenpei.setCheweiId(cheweiId);
        fenpei.setYonghuId(yonghuId);
        fenpei.setFenpeiTime(new Date());
        fenpei.setCreateTime(new Date());
        insert(fenpei);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Integer fenpeiId) {
        if (fenpeiId == null) {
            throw new EIException("分配记录id不能为空");
        }

        CheweiFenpeiEntity fenpei = selectById(fenpeiId);
        if (fenpei == null) {
            throw new EIException("分配记录不存在");
        }

        Integer cheweiId = fenpei.getCheweiId();

        // 1. 删除分配记录
        deleteById(fenpeiId);

        // 2. 回滚车位状态为空闲
        rollbackCheweiStatus(cheweiId);
    }

    /**
     * 将车位状态回滚为空闲
     */
    private void rollbackCheweiStatus(Integer cheweiId) {
        CheweiEntity freeEntity = new CheweiEntity();
        freeEntity.setCheweiZhuangtaiTypes(CHEWEI_STATUS_FREE);
        cheweiService.update(freeEntity,
                new EntityWrapper<CheweiEntity>()
                        .eq("id", cheweiId)
                        .eq("chewei_zhuangtai_types", CHEWEI_STATUS_OCCUPIED)
        );
    }

}
