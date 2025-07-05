package ru.iguana.integrationroles.data.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
public class UserKey implements Serializable {

    private String sub;
    private String systemCode;

}

