package com.swpu.equipment.notice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swpu.equipment.common.util.Result;
import com.swpu.equipment.notice.entity.Notice;
import com.swpu.equipment.notice.entity.NoticeVO;
import com.swpu.equipment.notice.service.NoticeService;
import com.swpu.equipment.user.entity.User;
import com.swpu.equipment.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notice")
public class NoticeController {
    
    @Autowired
    private NoticeService noticeService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取公告分页列表
     */
    @GetMapping("/list")
    public Result<IPage<NoticeVO>> getPageList(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "") String keyword) {
        try {
            Page<NoticeVO> page = new Page<>(currentPage, pageSize);
            IPage<NoticeVO> result = noticeService.getNoticeList(page, keyword);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取公告列表失败", e);
            return Result.error("获取公告列表失败");
        }
    }
    
    @GetMapping("/{id}")
    public Result<NoticeVO> getNoticeById(@PathVariable Long id) {
        try {
            Notice notice = noticeService.getById(id);
            if (notice == null) {
                return Result.error("公告不存在");
            }
            NoticeVO noticeVO = new NoticeVO();
            noticeVO.setId(notice.getId());
            noticeVO.setTitle(notice.getTitle());
            noticeVO.setContent(notice.getContent());
            noticeVO.setCreatorId(notice.getCreatorId());
            noticeVO.setCreateTime(notice.getCreateTime());
            noticeVO.setUpdateTime(notice.getUpdateTime());
            
            if (notice.getCreatorId() != null) {
                User user = userService.getById(notice.getCreatorId());
                if (user != null) {
                    noticeVO.setCreatorName(user.getRealName());
                }
            }
            
            return Result.success(noticeVO);
        } catch (Exception e) {
            log.error("获取公告详情失败", e);
            return Result.error("获取公告详情失败");
        }
    }
    
    @GetMapping("/adjacent/{id}")
    public Result<Map<String, NoticeVO>> getAdjacentNotices(@PathVariable Long id) {
        try {
            Map<String, NoticeVO> result = new HashMap<>();
            
            LambdaQueryWrapper<Notice> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(Notice::getCreateTime);
            List<Notice> notices = noticeService.list(queryWrapper);
            
            NoticeVO prevNotice = null;
            NoticeVO nextNotice = null;
            
            for (int i = 0; i < notices.size(); i++) {
                if (notices.get(i).getId().equals(id)) {
                    if (i < notices.size() - 1) {
                        nextNotice = convertToVO(notices.get(i + 1));
                    }
                    if (i > 0) {
                        prevNotice = convertToVO(notices.get(i - 1));
                    }
                    break;
                }
            }
            
            result.put("prev", prevNotice);
            result.put("next", nextNotice);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取相邻公告失败", e);
            return Result.error("获取相邻公告失败");
        }
    }
    
    private NoticeVO convertToVO(Notice notice) {
        NoticeVO noticeVO = new NoticeVO();
        noticeVO.setId(notice.getId());
        noticeVO.setTitle(notice.getTitle());
        noticeVO.setContent(notice.getContent());
        noticeVO.setCreatorId(notice.getCreatorId());
        noticeVO.setCreateTime(notice.getCreateTime());
        noticeVO.setUpdateTime(notice.getUpdateTime());
        
        if (notice.getCreatorId() != null) {
            User user = userService.getById(notice.getCreatorId());
            if (user != null) {
                noticeVO.setCreatorName(user.getRealName());
            }
        }
        
        return noticeVO;
    }
    
    /**
     * 创建公告
     */
    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public Result<String> createNotice(@RequestBody Notice notice) {
        try {
            notice.setCreateTime(LocalDateTime.now());
            notice.setUpdateTime(LocalDateTime.now());
            noticeService.save(notice);
            return Result.success("创建公告成功");
        } catch (Exception e) {
            log.error("创建公告失败", e);
            return Result.error("创建公告失败");
        }
    }
    
    /**
     * 更新公告
     */
    @PutMapping
    @PreAuthorize("hasAuthority('admin')")
    public Result<String> updateNotice(@RequestBody Notice notice) {
        try {
            notice.setUpdateTime(LocalDateTime.now());
            noticeService.updateById(notice);
            return Result.success("更新公告成功");
        } catch (Exception e) {
            log.error("更新公告失败", e);
            return Result.error("更新公告失败");
        }
    }
    
    /**
     * 删除公告
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteNotice(@PathVariable Long id) {
        try {
            noticeService.removeById(id);
            return Result.success("删除公告成功");
        } catch (Exception e) {
            log.error("删除公告失败", e);
            return Result.error("删除公告失败");
        }
    }
}
