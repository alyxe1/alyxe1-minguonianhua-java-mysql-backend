package org.exh.nianhuawechatminiprogrambackend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.dto.response.ThemeDetailResponse;
import org.exh.nianhuawechatminiprogrambackend.entity.Theme;
import org.exh.nianhuawechatminiprogrambackend.enums.ResultCode;
import org.exh.nianhuawechatminiprogrambackend.exception.BusinessException;
import org.exh.nianhuawechatminiprogrambackend.mapper.ThemeMapper;
import org.exh.nianhuawechatminiprogrambackend.service.ThemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 主题服务实现类
 */
@Slf4j
@Service
public class ThemeServiceImpl implements ThemeService {

    @Autowired
    private ThemeMapper themeMapper;

    @Override
    public ThemeDetailResponse getThemeDetail(String themeType) {
        log.info("获取主题详情开始，themeType={}", themeType);

        // 1. 参数校验
        if (themeType == null || themeType.isEmpty()) {
            log.error("主题类型不能为空");
            throw new BusinessException(ResultCode.ERROR, "主题类型不能为空");
        }

        // 2. 将themeType转换为Long类型的ID
        Long themeId;
        try {
            themeId = Long.parseLong(themeType);
        } catch (NumberFormatException e) {
            log.error("主题类型格式错误，必须为数字: {}", themeType);
            throw new BusinessException(ResultCode.ERROR, "主题类型格式错误");
        }

        // 3. 查询主题信息
        Theme theme = themeMapper.selectById(themeId);
        if (theme == null) {
            log.error("主题不存在，themeId={}", themeId);
            throw new BusinessException(ResultCode.ERROR, "主题不存在");
        }

        // 4. 检查主题是否已删除
        if (theme.getIsDeleted() != null && theme.getIsDeleted() == 1) {
            log.error("主题已删除，themeId={}", themeId);
            throw new BusinessException(ResultCode.ERROR, "主题不存在");
        }

        // 5. 检查主题状态
        if (theme.getStatus() != null && theme.getStatus() == 0) {
            log.error("主题已下架，themeId={}", themeId);
            throw new BusinessException(ResultCode.ERROR, "主题已下架");
        }

        // 6. 组装响应数据
        ThemeDetailResponse response = new ThemeDetailResponse();
        response.setHeaderImage(theme.getCoverImage());
        response.setTitle(theme.getTitle());
        response.setAddress(theme.getAddress() != null ? theme.getAddress() : "");

        log.info("获取主题详情成功，themeId={}", themeId);
        return response;
    }
}
