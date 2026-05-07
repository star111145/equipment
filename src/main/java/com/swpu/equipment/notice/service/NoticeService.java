package com.swpu.equipment.notice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swpu.equipment.notice.entity.Notice;
import com.swpu.equipment.notice.entity.NoticeVO;

public interface NoticeService extends IService<Notice> {
    IPage<NoticeVO> getNoticeList(Page<NoticeVO> page, String keyword);
}
