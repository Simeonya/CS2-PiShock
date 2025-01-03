package me.simeonya.pishock;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Credits to: https://github.com/ScoreUnder/pishock-zap-fabric/tree/mc1.21
Edited by me (Simeon) to work with the latest version of the plugin.
 */

@Data
@Builder
public class PishockZapConfig {
    @Builder.Default
    private boolean enabled = true;
    @Builder.Default
    private int webPort = 3000;
    @Builder.Default
    private boolean vibrationOnly = false;
    @Builder.Default
    private boolean startUpVibrate = true;
    @Builder.Default
    private boolean playStartupSound = true;
    @Builder.Default
    private float duration = 1.0f;
    @Builder.Default
    private float maxDuration = 10.0f;
    @Builder.Default
    private int vibrationIntensityMin = 20;
    @Builder.Default
    private int shockIntensityMin = 5;
    @Builder.Default
    private int shockIntensityDeath = 75;
    @Builder.Default
    private float shockDurationDeath = 5.0f;
    @Builder.Default
    private int waitBetweenShocks = 100;
    @Builder.Default
    private ShockDistribution shockDistribution = ShockDistribution.ALL;
    @Builder.Default
    private String serialPort = "ENTER_SERIAL_PORT_HERE";
    @Builder.Default
    private String playerNameToShock = "ENTER_USERNAME_HERE";
    @Builder.Default
    private List<Integer> deviceIds = List.of(12345, 23456, 34567, 45678, 56789);
    @Builder.Default
    private Map<Integer, Integer> maxIntensityPerShocker = new HashMap<Integer, Integer>() {{
        put(12345, 0);
    }};

    /**
     * Get the maximum intensity for a shocker device.
     *
     * @param deviceId the device ID
     * @return the maximum intensity
     */
    public int getMaxShockerIntensity(int deviceId) {
        return maxIntensityPerShocker.getOrDefault(deviceId, 5);
    }
}



