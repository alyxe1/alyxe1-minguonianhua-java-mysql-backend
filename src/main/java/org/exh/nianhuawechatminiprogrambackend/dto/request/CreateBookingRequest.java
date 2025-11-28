package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 创建预订请求 DTO
 */
@Data
public class CreateBookingRequest {

    /**
     * 场次类型 (lunch/dinner)
     */
    @NotBlank(message = "场次类型不能为空")
    private String sessionType;

    /**
     * 日期 (yyyy-mm-dd格式)
     */
    @NotBlank(message = "日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式错误，应为YYYY-MM-DD")
    private String date;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 选中的商品列表
     */
    @NotNull(message = "商品列表不能为空")
    @Size(min = 1, message = "至少选择一个商品")
    @Valid
    private List<SelectedGood> selectedGoodList;

    /**
     * 选中的座位列表
     */
    @NotNull(message = "座位列表不能为空")
    @Size(min = 1, message = "至少选择一个座位")
    @Valid
    private List<SelectedSeat> selectedSeatList;
}
