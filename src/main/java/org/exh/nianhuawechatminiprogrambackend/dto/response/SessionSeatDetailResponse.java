package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 场次座位详情响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionSeatDetailResponse {

    /**
     * 座位详情列表
     */
    @NotNull(message = "座位详情列表不能为空")
    private List<SeatDetail> seatDetailList;
}
