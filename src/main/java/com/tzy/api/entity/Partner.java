package com.tzy.api.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Partner {
    private String nickname;
    private String secUid;
}
