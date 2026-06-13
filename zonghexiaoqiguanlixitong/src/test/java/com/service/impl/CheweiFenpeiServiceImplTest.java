package com.service.impl;

import com.dao.CheweiDao;
import com.dao.CheweiFenpeiDao;
import com.entity.CheweiEntity;
import com.entity.CheweiFenpeiEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 车位分配服务测试
 *
 * 验证点：
 * 1. 空闲车位可以正常分配，状态变为已占用
 * 2. 已占用车位不能重复分配（并发防护）
 * 3. 车位不存在时分配失败
 * 4. 删除分配记录后车位状态回滚为空闲
 * 5. 同一车位有多条分配记录时，只在全部删除后才释放
 */
@ExtendWith(MockitoExtension.class)
public class CheweiFenpeiServiceImplTest {

    @InjectMocks
    private CheweiFenpeiServiceImpl cheweiFenpeiService;

    @Mock
    private CheweiFenpeiDao cheweiFenpeiDao;

    @Mock
    private CheweiDao cheweiDao;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(cheweiFenpeiService, "baseMapper", cheweiFenpeiDao);
    }

    // ==================== allocateChewei 测试 ====================

    @Test
    public void testAllocate_freeSpot_success() {
        // 准备：车位状态为空闲(2)
        CheweiEntity chewei = new CheweiEntity();
        chewei.setId(1);
        chewei.setCheweiZhuangtaiTypes(2); // 空闲

        when(cheweiDao.selectByIdForUpdate(1)).thenReturn(chewei);
        when(cheweiFenpeiDao.insert(any(CheweiFenpeiEntity.class))).thenReturn(1);
        when(cheweiDao.updateById(any(CheweiEntity.class))).thenReturn(1);

        CheweiFenpeiEntity fenpei = new CheweiFenpeiEntity();
        fenpei.setCheweiId(1);
        fenpei.setYonghuId(100);

        // 执行
        cheweiFenpeiService.allocateChewei(fenpei);

        // 验证：插入了一条分配记录（不是两条）
        verify(cheweiFenpeiDao, times(1)).insert(any(CheweiFenpeiEntity.class));

        // 验证：车位状态被更新为已占用(1)
        ArgumentCaptor<CheweiEntity> captor = ArgumentCaptor.forClass(CheweiEntity.class);
        verify(cheweiDao).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getValue().getCheweiZhuangtaiTypes());

        // 验证：createTime 被设置
        assertNotNull(fenpei.getCreateTime());
    }

    @Test
    public void testAllocate_occupiedSpot_throwsException() {
        // 准备：车位状态为已占用(1)
        CheweiEntity chewei = new CheweiEntity();
        chewei.setId(1);
        chewei.setCheweiZhuangtaiTypes(1); // 已占用

        when(cheweiDao.selectByIdForUpdate(1)).thenReturn(chewei);

        CheweiFenpeiEntity fenpei = new CheweiFenpeiEntity();
        fenpei.setCheweiId(1);
        fenpei.setYonghuId(200);

        // 执行 & 验证：应该抛出异常，阻止重复分配
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> cheweiFenpeiService.allocateChewei(fenpei));
        assertEquals("该车位已被分配，不能重复分配", ex.getMessage());

        // 验证：没有插入任何记录
        verify(cheweiFenpeiDao, never()).insert(any());
        // 验证：没有更新车位状态
        verify(cheweiDao, never()).updateById(any());
    }

    @Test
    public void testAllocate_nonExistentSpot_throwsException() {
        // 准备：车位不存在
        when(cheweiDao.selectByIdForUpdate(999)).thenReturn(null);

        CheweiFenpeiEntity fenpei = new CheweiFenpeiEntity();
        fenpei.setCheweiId(999);
        fenpei.setYonghuId(100);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> cheweiFenpeiService.allocateChewei(fenpei));
        assertEquals("车位不存在", ex.getMessage());
    }

    @Test
    public void testAllocate_nullStatus_treatedAsFree() {
        // 准备：新录入的车位，状态为null（未设置）
        CheweiEntity chewei = new CheweiEntity();
        chewei.setId(1);
        chewei.setCheweiZhuangtaiTypes(null);

        when(cheweiDao.selectByIdForUpdate(1)).thenReturn(chewei);
        when(cheweiFenpeiDao.insert(any(CheweiFenpeiEntity.class))).thenReturn(1);
        when(cheweiDao.updateById(any(CheweiEntity.class))).thenReturn(1);

        CheweiFenpeiEntity fenpei = new CheweiFenpeiEntity();
        fenpei.setCheweiId(1);
        fenpei.setYonghuId(100);

        // 执行：状态为null应该视为可分配
        cheweiFenpeiService.allocateChewei(fenpei);

        // 验证：成功插入
        verify(cheweiFenpeiDao, times(1)).insert(any());
    }

    // ==================== releaseChewei 测试 ====================

    @Test
    public void testRelease_singleRecord_cheweiBecomeFree() {
        // 准备：一条分配记录
        CheweiFenpeiEntity fenpei = new CheweiFenpeiEntity();
        fenpei.setId(10);
        fenpei.setCheweiId(1);

        when(cheweiFenpeiDao.selectBatchIds(Arrays.asList(10)))
            .thenReturn(Collections.singletonList(fenpei));
        when(cheweiFenpeiDao.selectCount(any())).thenReturn(0); // 删除后无剩余记录

        // 执行
        cheweiFenpeiService.releaseChewei(new Integer[]{10});

        // 验证：删除了分配记录
        verify(cheweiFenpeiDao).deleteBatchIds(Arrays.asList(10));

        // 验证：车位状态回滚为空闲(2)
        ArgumentCaptor<CheweiEntity> captor = ArgumentCaptor.forClass(CheweiEntity.class);
        verify(cheweiDao).updateById(captor.capture());
        assertEquals(Integer.valueOf(2), captor.getValue().getCheweiZhuangtaiTypes());
        assertEquals(Integer.valueOf(1), captor.getValue().getId());
    }

    @Test
    public void testRelease_multipleRecordsSameChewei_onlyFreeWhenAllDeleted() {
        // 准备：同一车位有两条分配记录，只删除一条
        CheweiFenpeiEntity fenpei = new CheweiFenpeiEntity();
        fenpei.setId(10);
        fenpei.setCheweiId(1);

        when(cheweiFenpeiDao.selectBatchIds(Arrays.asList(10)))
            .thenReturn(Collections.singletonList(fenpei));
        when(cheweiFenpeiDao.selectCount(any())).thenReturn(1); // 还有1条剩余

        // 执行
        cheweiFenpeiService.releaseChewei(new Integer[]{10});

        // 验证：删除了分配记录
        verify(cheweiFenpeiDao).deleteBatchIds(Arrays.asList(10));

        // 验证：不会释放车位（因为还有其他分配记录）
        verify(cheweiDao, never()).updateById(any());
    }
}
