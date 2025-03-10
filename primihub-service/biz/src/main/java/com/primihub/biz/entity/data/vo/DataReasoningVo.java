package com.primihub.biz.entity.data.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class DataReasoningVo {

    private Long id;

    private String reasoningId;

    private String reasoningName;

    private String reasoningDesc;

    private Integer reasoningType;

    private Integer reasoningState;

    private Long taskId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date releaseDate;
}
