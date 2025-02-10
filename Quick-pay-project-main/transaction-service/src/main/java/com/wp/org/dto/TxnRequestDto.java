package com.wp.org.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class TxnRequestDto {
    @NotNull
    private String toUserEmail;

    @NotNull
    private Double amount;

    private String comment;
}
