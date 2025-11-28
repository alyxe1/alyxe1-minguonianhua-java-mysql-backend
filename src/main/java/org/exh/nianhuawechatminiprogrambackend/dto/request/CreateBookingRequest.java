package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建预订请求 DTO
 */
@Data
public class CreateBookingRequest {

    /**
     * 场次类型（lunch/dinner）
     */
    @NotBlank(message = "场次类型不能为空")
    private String sessionType;

    /**
     * 日期 (YYYY-MM-DD格式)
     */
    @NotBlank(message = "日期不能为空")
    private String date;

    /**
     * 用户ID（从JWT token中获取，也可以从前端传入）
     */
    private Long userId;

    /**
     * 选择的商品列表（写真、化妆、摄影、座位商品包）
     */
    @NotEmpty(message = "商品列表不能为空")
    private List<SelectedGood> selectedGoodList;

    /**
     * 选择的座位列表
     */
    @NotEmpty(message = "座位列表不能为空")
    private List<SelectedSeatForBooking> selectedSeatList;

    /**
     * 选择的座位详情（用于预订时使用）
     */
    @Data
    public static class SelectedSeatForBooking {
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
