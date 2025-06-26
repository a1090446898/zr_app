package com.gsoft.inventory.entities;

import com.orm.SugarRecord;
import com.orm.dsl.Column;

import java.io.Serializable;

/**
 * 资产清查人员
 */
public class ZCQCY extends SugarRecord implements Serializable {
    public String CODE = "";
    public String NAME = "";

    @Column(name = "USER_TYPE")
    public String userType = "";
}
