package com.swpu.equipment.lifecycle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.lifecycle.entity.EquipmentRepair;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EquipmentRepairMapper extends BaseMapper<EquipmentRepair> {
    /**
     * 获取设备的报修数量（报修中状态）
     */
    Long getRepairQuantity(@Param("equipmentId") Long equipmentId);
    
    /**
     * 获取设备的维修数量（已维修状态）
     */
    Long getMaintenanceQuantity(@Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备的已故障数量（已同意的报修）
     */
    Long selectSumFaulty(@Param("equipmentId") Long equipmentId);
    
    /**
     * 查询设备是否有待处理的报修
     */
    Boolean selectHasPendingRepair(@Param("equipmentId") Long equipmentId);
    
    int countPendingRepairs(@Param("userId") Long userId, @Param("equipmentId") Long equipmentId);

    /**
     * 分页查询报修列表（关联设备表）
     */
    IPage<EquipmentRepair> getPageListWithType(Page<EquipmentRepair> page, 
                                               @Param("userId") Long userId,
                                               @Param("repairStatus") Integer repairStatus,
                                               @Param("auditStatus") Integer auditStatus,
                                               @Param("keyword") String keyword);
}
