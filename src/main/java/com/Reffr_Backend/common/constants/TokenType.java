package com.Reffr_Backend.common.constants;

public enum TokenType {
    ACCESS,
    REFRESH;

    public boolean isRefresh() { return this == REFRESH; }
    public boolean isAccess()  { return this == ACCESS;  }
}