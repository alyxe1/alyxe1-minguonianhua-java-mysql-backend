package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("分页结果")
public class PageResult<T> {

    @ApiModelProperty("数据列表")
    private List<T> items;

    @ApiModelProperty("总记录数")
    private Long total;

    @ApiModelProperty("当前页码")
    private Integer page;

    @ApiModelProperty("每页数量")
    private Integer pageSize;

    @ApiModelProperty("是否有更多数据")
    private Boolean hasMore;

    public static <T> PageResult<T> of(List<T> items, Long total, Integer page, Integer pageSize) {
        return PageResult.<T>builder()
                .items(items)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .hasMore(total > (long) page * pageSize)
                .build();
    }
}
