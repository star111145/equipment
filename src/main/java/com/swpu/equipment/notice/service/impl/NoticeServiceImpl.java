package com.swpu.equipment.notice.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swpu.equipment.notice.entity.Notice;
import com.swpu.equipment.notice.entity.NoticeVO;
import com.swpu.equipment.notice.mapper.NoticeMapper;
import com.swpu.equipment.notice.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {
    
    @Autowired
    private NoticeMapper noticeMapper;
    
    @Override
    public IPage<NoticeVO> getNoticeList(Page<NoticeVO> page, String keyword) {
        return noticeMapper.getNoticeList(page, keyword);
    }
}
