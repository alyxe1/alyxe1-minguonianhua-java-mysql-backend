package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 校验座位请求DTO
 */
@Data
public class CheckSeatRequest {

    /**
     * 日期 (YYYY-MM-DD格式)
     */
    @NotBlank(message = "日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式错误，应为YYYY-MM-DD")
    private String date;

    /**
     * 场次类型 (lunch/dinner)
     */
    @NotBlank(message = "场次类型不能为空")
    private String sessionType;

    /**
     * 选择的商品列表
     */
    @NotNull(message = "商品列表不能为空")
    @Size(min = 1, message = "至少选择一个商品")
    private List<SelectedGood> selectedGoodList;
}
