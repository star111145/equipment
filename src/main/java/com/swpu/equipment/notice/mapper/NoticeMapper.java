package com.swpu.equipment.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.notice.entity.Notice;
import com.swpu.equipment.notice.entity.NoticeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
    IPage<NoticeVO> getNoticeList(Page<NoticeVO> page, @Param("keyword") String keyword);
}
