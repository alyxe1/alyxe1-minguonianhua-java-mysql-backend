package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建预订请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    /**
     * 场次ID
     */
    @NotBlank(message = "场次ID不能为空")
    private String sessionType;

    /**
     * 日期 (YYYY-MM-DD格式)
     */
    @NotBlank(message = "日期不能为空")
    private String date;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /**
     * 选择的商品列表
     */
    @Valid
    @NotEmpty(message = "商品列表不能为空")
    private List<SelectedGood> selectedGoodList;

    /**
     * 选择的座位列表
     */
    @Valid
    @NotEmpty(message = "座位列表不能为空")
    private List<SelectedSeat> selectedSeatList;

    /**
     * 选择的座位 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedSeat {
        /**
         * 座位ID
         */
        @NotBlank(message = "座位ID不能为空")
        private String seatId;

        /**
         * 座位名称
         */
        @NotBlank(message = "座位名称不能为空")
        private String seatName;
    }
}
