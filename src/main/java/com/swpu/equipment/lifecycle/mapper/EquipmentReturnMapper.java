package com.swpu.equipment.lifecycle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.lifecycle.entity.EquipmentReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Mapper
public interface EquipmentReturnMapper extends BaseMapper<EquipmentReturn> {
    IPage<EquipmentReturn> getPageListWithType(Page<EquipmentReturn> page, @Param("userId") Long userId, 
            @Param("status") Integer status, @Param("auditStatus") Integer auditStatus, @Param("keyword") String keyword);
    
    int countPendingReturns(@Param("userId") Long userId, @Param("equipmentId") Long equipmentId);
    
    List<EquipmentReturn> selectListWithDetails(@Param("equipmentIds") Set<Long> equipmentIds, @Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}
