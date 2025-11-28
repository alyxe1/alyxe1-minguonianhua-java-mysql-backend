package org.exh.nianhuawechatminiprogrambackend.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 选中的座位 DTO
 */
@Data
public class SelectedSeat {

    /**
     * 座位ID
     */
    @NotBlank(message = "座位ID不能为空")
    private String seatId;

    /**
     * 座位名称
     */
    private String seatName;
}
