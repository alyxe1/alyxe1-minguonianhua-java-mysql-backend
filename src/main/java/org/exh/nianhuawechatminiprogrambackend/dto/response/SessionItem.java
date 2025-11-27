package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场次信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionItem {
    private String title;
    private List<String> descList;
}
