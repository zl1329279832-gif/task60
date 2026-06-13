package com.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.CheweiEntity;
import com.entity.CheweiFenpeiEntity;
import com.entity.EIException;
import com.service.CheweiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 车位分配 Service 单元测试
 *
 * 验证场景：
 * 1. 正常分配空闲车位 → 成功（插入记录 + 车位变已占用）
 * 2. 车位不存在 → 抛异常
 * 3. 并发竞争同一车位（CAS 失败）→ 抛异常，不插入记录
 * 4. 重复分配（同用户同车位已存在）→ 抛异常，回滚车位状态
 * 5. 正常释放 → 删除记录 + 车位状态回滚为空闲
 * 6. 释放不存在记录 → 抛异常
 *
 * 运行方式：mvn test -Dtest=CheweiFenpeiServiceImplTest
 */
@ExtendWith(MockitoExtension.class)
class CheweiFenpeiServiceImplTest {

    @Spy
    @InjectMocks
    private CheweiFenpeiServiceImpl cheweiFenpeiService;

    @Mock
    private CheweiService cheweiService;

    // ========== allocate 测试 ==========

    @Test
    void testAllocate_success() {
        CheweiEntity chewei = new CheweiEntity();
        chewei.setId(1);
        chewei.setCheweiZhuangtaiTypes(1);
        when(cheweiService.selectById(1)).thenReturn(chewei);
        when(cheweiService.update(any(CheweiEntity.class), any(EntityWrapper.class))).thenReturn(true);
        doReturn(null).when(cheweiFenpeiService).selectOne(any(Wrapper.class));
        doReturn(true).when(cheweiFenpeiService).insert(any(CheweiFenpeiEntity.class));

        cheweiFenpeiService.allocate(1, 100);

        verify(cheweiService).update(any(CheweiEntity.class), any(EntityWrapper.class));
        verify(cheweiFenpeiService).insert(any(CheweiFenpeiEntity.class));
    }

    @Test
    void testAllocate_cheweiNotFound() {
        when(cheweiService.selectById(999)).thenReturn(null);
        assertThrows(EIException.class, () -> cheweiFenpeiService.allocate(999, 100));
    }

    @Test
    void testAllocate_concurrentContention() {
        CheweiEntity chewei = new CheweiEntity();
        chewei.setId(1);
        chewei.setCheweiZhuangtaiTypes(1);
        when(cheweiService.selectById(1)).thenReturn(chewei);
        // CAS 失败：另一线程已抢先
        when(cheweiService.update(any(CheweiEntity.class), any(EntityWrapper.class))).thenReturn(false);

        assertThrows(EIException.class, () -> cheweiFenpeiService.allocate(1, 100));
        // 不应有任何 insert
        verify(cheweiFenpeiService, never()).insert(any());
    }

    @Test
    void testAllocate_duplicateAllocation() {
        CheweiEntity chewei = new CheweiEntity();
        chewei.setId(1);
        chewei.setCheweiZhuangtaiTypes(1);
        when(cheweiService.selectById(1)).thenReturn(chewei);
        when(cheweiService.update(any(CheweiEntity.class), any(EntityWrapper.class))).thenReturn(true);
        // 已存在相同分配
        CheweiFenpeiEntity existing = new CheweiFenpeiEntity();
        existing.setCheweiId(1);
        existing.setYonghuId(100);
        doReturn(existing).when(cheweiFenpeiService).selectOne(any(Wrapper.class));

        assertThrows(EIException.class, () -> cheweiFenpeiService.allocate(1, 100));
        // 回滚：至少2次 update（1次 CAS 占用 + 1次回滚为空闲）
        verify(cheweiService, atLeast(2)).update(any(CheweiEntity.class), any(EntityWrapper.class));
        // 不应插入
        verify(cheweiFenpeiService, never()).insert(any());
    }

    @Test
    void testAllocate_nullParams() {
        assertThrows(EIException.class, () -> cheweiFenpeiService.allocate(null, 100));
        assertThrows(EIException.class, () -> cheweiFenpeiService.allocate(1, null));
    }

    // ========== release 测试 ==========

    @Test
    void testRelease_success() {
        CheweiFenpeiEntity fenpei = new CheweiFenpeiEntity();
        fenpei.setId(10);
        fenpei.setCheweiId(1);
        fenpei.setYonghuId(100);
        doReturn(fenpei).when(cheweiFenpeiService).selectById(10);
        doReturn(true).when(cheweiFenpeiService).deleteById(10);
        when(cheweiService.update(any(CheweiEntity.class), any(EntityWrapper.class))).thenReturn(true);

        cheweiFenpeiService.release(10);

        verify(cheweiFenpeiService).deleteById(10);
        verify(cheweiService).update(any(CheweiEntity.class), any(EntityWrapper.class));
    }

    @Test
    void testRelease_notFound() {
        doReturn(null).when(cheweiFenpeiService).selectById(999);
        assertThrows(EIException.class, () -> cheweiFenpeiService.release(999));
        verify(cheweiFenpeiService, never()).deleteById(any());
    }

    @Test
    void testRelease_nullId() {
        assertThrows(EIException.class, () -> cheweiFenpeiService.release(null));
    }

}
