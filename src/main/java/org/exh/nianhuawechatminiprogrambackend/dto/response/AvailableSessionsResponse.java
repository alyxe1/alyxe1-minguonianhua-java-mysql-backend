package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 可用场次响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSessionsResponse {
    private List<SessionItem> items;
}
