package me.simeonya.pishock;

import lombok.NonNull;

/*
Credits to: https://github.com/ScoreUnder/pishock-zap-fabric/tree/mc1.21
 */


/**
 * Operation types for the System.
 */
public enum OpType {
    SHOCK(0, "shock"), VIBRATE(1, "vibrate"), BEEP(2, "beep");

    public final int code;
    public final String firmwareCode;

    OpType(int code, @NonNull String firmwareCode) {
        this.code = code;
        this.firmwareCode = firmwareCode;
    }
}
