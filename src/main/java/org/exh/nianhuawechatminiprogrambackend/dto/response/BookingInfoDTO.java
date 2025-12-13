package org.exh.nianhuawechatminiprogrambackend.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("预订基础信息")
public class BookingInfoDTO {

    @ApiModelProperty("主题标题")
    private String themeTitle;

    @ApiModelProperty("场次时间")
    private LocalDateTime sessionTime;

    @ApiModelProperty("人数")
    private Integer peopleCount;

    @ApiModelProperty(value = "主题图片", required = true)
    private String themePic;
}
