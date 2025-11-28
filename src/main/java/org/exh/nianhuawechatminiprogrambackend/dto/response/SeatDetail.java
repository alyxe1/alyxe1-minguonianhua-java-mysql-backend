package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 座位详情 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatDetail {

    /**
     * 座位ID
     */
    @NotBlank(message = "座位ID不能为空")
    private String seatId;

    /**
     * 是否已被选中（预订）
     */
    @NotNull(message = "isSelected不能为空")
    private Boolean isSelected;

    /**
     * 座位名称
     */
    private String seatName;

    /**
     * 座位类型：front/front-内场/前排, middle-中场/中排, back-外场/后排
     */
    @NotBlank(message = "座位类型不能为空")
    private String seatType;
}
