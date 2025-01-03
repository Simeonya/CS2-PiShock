package me.simeonya.pishock;

import lombok.NonNull;

/*
Credits to: https://github.com/ScoreUnder/pishock-zap-fabric/tree/mc1.21
 */


/**
 * PiShock API for the System.
 */
public interface PiShockApi {
    void performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration);

    default void close() {
    }
}
