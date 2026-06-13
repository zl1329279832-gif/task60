package com.service.impl;

import com.entity.BaoxiuEntity;
import com.entity.EIException;
import com.service.BaoxiuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 报修工单 Service 单元测试
 *
 * 验证场景：
 * 1. 合法状态推进链：待处理→已接单→处理中→已完结
 * 2. 非法跳转（跳级、回退、已完结再推）→ 拒绝
 * 3. 角色校验：用户不能执行物业操作
 * 4. 工单不存在 / 参数为空 → 抛异常
 *
 * 运行方式：mvn test -Dtest=BaoxiuServiceImplTest
 */
@ExtendWith(MockitoExtension.class)
class BaoxiuServiceImplTest {

    @Spy
    @InjectMocks
    private BaoxiuServiceImpl baoxiuService;

    private BaoxiuEntity makeBaoxiu(Integer status) {
        BaoxiuEntity b = new BaoxiuEntity();
        b.setId(1);
        b.setBaoxiuZhuangtaiTypes(status);
        b.setYonghuId(10);
        return b;
    }

    // ========== 合法状态推进 ==========

    @Test
    void testAdvance_pendingToAccepted() {
        doReturn(makeBaoxiu(BaoxiuService.STATUS_PENDING)).when(baoxiuService).selectById(1);
        doReturn(true).when(baoxiuService).updateById(any(BaoxiuEntity.class));

        baoxiuService.advanceStatus(1, BaoxiuService.STATUS_ACCEPTED, "物业人员");

        verify(baoxiuService).updateById(argThat(b ->
                b.getBaoxiuZhuangtaiTypes() == BaoxiuService.STATUS_ACCEPTED));
    }

    @Test
    void testAdvance_acceptedToProcessing() {
        doReturn(makeBaoxiu(BaoxiuService.STATUS_ACCEPTED)).when(baoxiuService).selectById(1);
        doReturn(true).when(baoxiuService).updateById(any(BaoxiuEntity.class));

        baoxiuService.advanceStatus(1, BaoxiuService.STATUS_PROCESSING, "物业人员");

        verify(baoxiuService).updateById(argThat(b ->
                b.getBaoxiuZhuangtaiTypes() == BaoxiuService.STATUS_PROCESSING));
    }

    @Test
    void testAdvance_processingToCompleted() {
        doReturn(makeBaoxiu(BaoxiuService.STATUS_PROCESSING)).when(baoxiuService).selectById(1);
        doReturn(true).when(baoxiuService).updateById(any(BaoxiuEntity.class));

        baoxiuService.advanceStatus(1, BaoxiuService.STATUS_COMPLETED, "物业人员");

        verify(baoxiuService).updateById(argThat(b ->
                b.getBaoxiuZhuangtaiTypes() == BaoxiuService.STATUS_COMPLETED));
    }

    // ========== 非法状态跳转 ==========

    @Test
    void testAdvance_illegalSkipTransition() {
        doReturn(makeBaoxiu(BaoxiuService.STATUS_PENDING)).when(baoxiuService).selectById(1);
        assertThrows(EIException.class, () ->
                baoxiuService.advanceStatus(1, BaoxiuService.STATUS_COMPLETED, "物业人员"));
    }

    @Test
    void testAdvance_completedCannotAdvance() {
        doReturn(makeBaoxiu(BaoxiuService.STATUS_COMPLETED)).when(baoxiuService).selectById(1);
        assertThrows(EIException.class, () ->
                baoxiuService.advanceStatus(1, BaoxiuService.STATUS_PENDING, "物业人员"));
    }

    @Test
    void testAdvance_reverseTransition() {
        doReturn(makeBaoxiu(BaoxiuService.STATUS_PROCESSING)).when(baoxiuService).selectById(1);
        assertThrows(EIException.class, () ->
                baoxiuService.advanceStatus(1, BaoxiuService.STATUS_ACCEPTED, "物业人员"));
    }

    // ========== 角色校验 ==========

    @Test
    void testAdvance_userCannotAccept() {
        doReturn(makeBaoxiu(BaoxiuService.STATUS_PENDING)).when(baoxiuService).selectById(1);
        assertThrows(EIException.class, () ->
                baoxiuService.advanceStatus(1, BaoxiuService.STATUS_ACCEPTED, "用户"));
    }

    @Test
    void testAdvance_userCannotProcess() {
        doReturn(makeBaoxiu(BaoxiuService.STATUS_ACCEPTED)).when(baoxiuService).selectById(1);
        assertThrows(EIException.class, () ->
                baoxiuService.advanceStatus(1, BaoxiuService.STATUS_PROCESSING, "用户"));
    }

    // ========== 边界 ==========

    @Test
    void testAdvance_baoxiuNotFound() {
        doReturn(null).when(baoxiuService).selectById(999);
        assertThrows(EIException.class, () ->
                baoxiuService.advanceStatus(999, BaoxiuService.STATUS_ACCEPTED, "物业人员"));
    }

    @Test
    void testAdvance_nullId() {
        assertThrows(EIException.class, () ->
                baoxiuService.advanceStatus(null, BaoxiuService.STATUS_ACCEPTED, "物业人员"));
    }

    @Test
    void testAdvance_nullTargetStatus() {
        assertThrows(EIException.class, () ->
                baoxiuService.advanceStatus(1, null, "物业人员"));
    }

}
