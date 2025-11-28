package org.exh.nianhuawechatminiprogrambackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exh.nianhuawechatminiprogrambackend.dto.request.SelectedGood;

import java.util.List;

/**
 * 校验座位响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckSeatResponse {

    /**
     * 选择的商品列表（原样返回）
     */
    private List<SelectedGood> selectedGoodList;
}
