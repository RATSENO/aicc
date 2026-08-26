package com.onestar.aicc.sample.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "도서 판매 상태")
public enum BookStatus {

    @Schema(description = "판매중")
    AVAILABLE,

    @Schema(description = "품절")
    SOLD_OUT,

    @Schema(description = "절판")
    DISCONTINUED
}
