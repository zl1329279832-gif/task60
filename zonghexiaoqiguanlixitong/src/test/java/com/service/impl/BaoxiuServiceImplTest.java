package com.service.impl;

import com.dao.BaoxiuDao;
import com.entity.BaoxiuEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 报修工单状态推进测试
 *
 * 验证点：
 * 1. 物业人员可以按合法顺序推进状态：已提交(1)→已接单(2)→处理中(3)→已完结(4)
 * 2. 不能跳跃状态（如1→3）
 * 3. 不能倒退状态（如3→2）
 * 4. 用户角色不能推进状态
 * 5. 已完结的工单不能继续推进
 * 6. 不存在的工单报错
 */
@ExtendWith(MockitoExtension.class)
public class BaoxiuServiceImplTest {

    @InjectMocks
    private BaoxiuServiceImpl baoxiuService;

    @Mock
    private BaoxiuDao baoxiuDao;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(baoxiuService, "baseMapper", baoxiuDao);
    }

    private BaoxiuEntity createBaoxiu(int id, int status) {
        BaoxiuEntity baoxiu = new BaoxiuEntity();
        baoxiu.setId(id);
        baoxiu.setBaoxiuZhuangtaiTypes(status);
        return baoxiu;
    }

    // ==================== 合法状态转换 ====================

    @Test
    public void testAdvance_submitted_to_accepted() {
        // 已提交(1) → 已接单(2)
        when(baoxiuDao.selectById(1)).thenReturn(createBaoxiu(1, 1));
        when(baoxiuDao.updateById(any())).thenReturn(1);

        baoxiuService.advanceStatus(1, 2, "物业人员");

        ArgumentCaptor<BaoxiuEntity> captor = ArgumentCaptor.forClass(BaoxiuEntity.class);
        verify(baoxiuDao).updateById(captor.capture());
        assertEquals(Integer.valueOf(2), captor.getValue().getBaoxiuZhuangtaiTypes());
    }

    @Test
    public void testAdvance_accepted_to_processing() {
        // 已接单(2) → 处理中(3)
        when(baoxiuDao.selectById(1)).thenReturn(createBaoxiu(1, 2));
        when(baoxiuDao.updateById(any())).thenReturn(1);

        baoxiuService.advanceStatus(1, 3, "物业人员");

        ArgumentCaptor<BaoxiuEntity> captor = ArgumentCaptor.forClass(BaoxiuEntity.class);
        verify(baoxiuDao).updateById(captor.capture());
        assertEquals(Integer.valueOf(3), captor.getValue().getBaoxiuZhuangtaiTypes());
    }

    @Test
    public void testAdvance_processing_to_completed() {
        // 处理中(3) → 已完结(4)
        when(baoxiuDao.selectById(1)).thenReturn(createBaoxiu(1, 3));
        when(baoxiuDao.updateById(any())).thenReturn(1);

        baoxiuService.advanceStatus(1, 4, "物业人员");

        ArgumentCaptor<BaoxiuEntity> captor = ArgumentCaptor.forClass(BaoxiuEntity.class);
        verify(baoxiuDao).updateById(captor.capture());
        assertEquals(Integer.valueOf(4), captor.getValue().getBaoxiuZhuangtaiTypes());
    }

    // ==================== 非法状态转换 ====================

    @Test
    public void testAdvance_skipState_throws() {
        // 已提交(1) → 处理中(3)：跳跃，应该失败
        when(baoxiuDao.selectById(1)).thenReturn(createBaoxiu(1, 1));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> baoxiuService.advanceStatus(1, 3, "物业人员"));
        assertTrue(ex.getMessage().contains("非法状态转换"));
        verify(baoxiuDao, never()).updateById(any());
    }

    @Test
    public void testAdvance_reverseState_throws() {
        // 处理中(3) → 已接单(2)：倒退，应该失败
        when(baoxiuDao.selectById(1)).thenReturn(createBaoxiu(1, 3));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> baoxiuService.advanceStatus(1, 2, "物业人员"));
        assertTrue(ex.getMessage().contains("非法状态转换"));
        verify(baoxiuDao, never()).updateById(any());
    }

    @Test
    public void testAdvance_completedOrder_throws() {
        // 已完结(4) → 任何状态：不允许
        when(baoxiuDao.selectById(1)).thenReturn(createBaoxiu(1, 4));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> baoxiuService.advanceStatus(1, 5, "物业人员"));
        assertEquals("当前工单状态不允许继续推进", ex.getMessage());
        verify(baoxiuDao, never()).updateById(any());
    }

    // ==================== 角色控制 ====================

    @Test
    public void testAdvance_userRole_throws() {
        // 用户角色不允许推进状态
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> baoxiuService.advanceStatus(1, 2, "用户"));
        assertEquals("只有物业人员可以推进工单状态", ex.getMessage());
        // 验证：甚至没有查询数据库
        verify(baoxiuDao, never()).selectById(any());
    }

    @Test
    public void testAdvance_adminRole_throws() {
        // 管理员角色也不允许推进状态（只有物业人员可以）
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> baoxiuService.advanceStatus(1, 2, "管理员"));
        assertEquals("只有物业人员可以推进工单状态", ex.getMessage());
    }

    // ==================== 边界情况 ====================

    @Test
    public void testAdvance_nonExistentOrder_throws() {
        when(baoxiuDao.selectById(999)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> baoxiuService.advanceStatus(999, 2, "物业人员"));
        assertEquals("报修工单不存在", ex.getMessage());
    }
}
