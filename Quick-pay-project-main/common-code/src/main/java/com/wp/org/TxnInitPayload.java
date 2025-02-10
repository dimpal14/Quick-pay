package com.wp.org;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class TxnInitPayload {
    private Long id;
    private String fromUserEmail;
    private String toUserEmail;
    private Double amount;
    private String requestId;
}
