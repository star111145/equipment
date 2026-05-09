package com.swpu.equipment.lifecycle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.lifecycle.entity.EquipmentBorrow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Mapper
public interface EquipmentBorrowMapper extends BaseMapper<EquipmentBorrow> {
    /**
     * 获取设备的借用数量（已借出状态）
     */
    Long getBorrowQuantity(@Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备的已借出数量（已同意的借用）
     */
    Long selectSumBorrowed(@Param("equipmentId") Long equipmentId);
    
    /**
     * 统计用户对设备的未归还借用数量
     */
    Long countUnreturnedBorrows(@Param("userId") Long userId, @Param("equipmentId") Long equipmentId);
    
    int countPendingBorrows(@Param("userId") Long userId, @Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备的已归还数量
     */
    Long selectSumReturned(@Param("equipmentId") Long equipmentId);
    
    /**
     * 获取设备的报修数量（报修中状态）
     */
    Long getRepairQuantity(@Param("equipmentId") Long equipmentId);
    
    /**
     * 获取设备的报修数量（报修中状态，指定用户）
     */
    Long getRepairQuantityByUser(@Param("equipmentId") Long equipmentId, @Param("userId") Long userId);
    
    /**
     * 获取当前用户对设备的未归还借用数量
     */
    Long getUserUnreturnedBorrowQuantity(@Param("equipmentId") Long equipmentId, @Param("userId") Long userId);

    /**
     * 检查用户是否有逾期的未归还借用
     */
    int countOverdueBorrows(@Param("userId") Long userId);
    
    /**
     * 检查用户是否有该设备的已借出记录
     */
    Long countUserBorrowedEquipment(@Param("equipmentId") Long equipmentId, @Param("userId") Long userId);
    
    /**
     * 查询用户对设备的未归还借用记录
     */
    List<EquipmentBorrow> findUnreturnedBorrowsByEquipmentAndUser(@Param("equipmentId") Long equipmentId, @Param("userId") Long userId);
    
    /**
     * 查询设备的未归还借用记录
     */
    List<EquipmentBorrow> findUnreturnedBorrowsByEquipment(@Param("equipmentId") Long equipmentId);
    
    /**
     * 分页查询借用列表（带设备类型）
     */
    IPage<EquipmentBorrow> getPageListWithType(Page<EquipmentBorrow> page, @Param("userId") Long userId,
            @Param("status") Integer status, @Param("auditStatus") Integer auditStatus, @Param("keyword") String keyword);
    
    List<EquipmentBorrow> selectListWithDetails(@Param("equipmentIds") Set<Long> equipmentIds, @Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}
