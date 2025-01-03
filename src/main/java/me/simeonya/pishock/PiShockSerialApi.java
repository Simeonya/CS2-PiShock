package me.simeonya.pishock;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.NonNull;
import me.simeonya.Main;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/*
Credits to: https://github.com/ScoreUnder/pishock-zap-fabric/tree/mc1.21
Edited by me (Simeon) to work with the latest version of the plugin.
 */

/**
 * PiShock API implementation for serial communication.
 */
public class PiShockSerialApi implements PiShockApi {
    public static final int PISHOCK_SERIAL_BAUD_RATE = 115200;
    private final Logger logger = Logger.getLogger(Main.class.getName());
    private final PishockZapConfig config;
    private final Executor executor;
    @Getter
    private final String portName;
    private final Gson gson = new Gson();
    private SerialPort commPort;
    private Writer jsonWriter;

    public PiShockSerialApi(@NonNull PishockZapConfig config, @NonNull Executor executor, @NonNull String portName) {
        this.config = config;
        this.executor = executor;
        this.portName = portName;
    }

    @Override
    public void performOp(@NonNull ShockDistribution distribution, @NonNull OpType op, int intensity, float duration) {
        if (!config.isEnabled()) return;
        if (config.isVibrationOnly()) {
            op = OpType.VIBRATE;
        }

        List<Integer> shockers = config.getDeviceIds();
        if (shockers.isEmpty()) {
            logger.warning("No PiShock shocker IDs configured");
            return;
        }

        if (distribution == ShockDistribution.ALL) {
            for (int deviceId : shockers) {
                try {
                    logger.info("Shocked with values: " + deviceId + " " + op + " " + intensity + " " + duration);
                    executeShockOperation(deviceId, op, intensity, duration);
                    logger.info("Shocked now waiting: " + Main.config.getWaitBetweenShocks());
                    Thread.sleep(Main.config.getWaitBetweenShocks());
                } catch (InterruptedException e) {
                    logger.severe("Interrupted during PiShock API call; exception: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    close();
                }
            }
        } else if (distribution == ShockDistribution.RANDOM) {
            try {
                int randomDevice = getRandomDevice();
                logger.info("Shocked with values: " + randomDevice + " " + op + " " + intensity + " " + duration);
                executeShockOperation(randomDevice, op, intensity, duration);
                logger.info("Shocked now waiting: " + Main.config.getWaitBetweenShocks());
                Thread.sleep(Main.config.getWaitBetweenShocks());
            } catch (InterruptedException e) {
                logger.severe("Interrupted during PiShock API call; exception: " + e.getMessage());
                Thread.currentThread().interrupt();
                close();
            }
        } else if (distribution == ShockDistribution.FIRST) {
            try {
                int firstDevice = shockers.get(0);
                logger.info("Shocked with values: " + firstDevice + " " + op + " " + intensity + " " + duration);
                executeShockOperation(firstDevice, op, intensity, duration);
                logger.info("Shocked now waiting: " + Main.config.getWaitBetweenShocks());
                Thread.sleep(Main.config.getWaitBetweenShocks());
            } catch (InterruptedException e) {
                logger.severe("Interrupted during PiShock API call; exception: " + e.getMessage());
                Thread.currentThread().interrupt();
                close();
            }
        } else if (distribution == ShockDistribution.LAST) {
            try {
                int lastDevice = shockers.get(shockers.size() - 1);
                logger.info("Shocked with values: " + lastDevice + " " + op + " " + intensity + " " + duration);
                executeShockOperation(lastDevice, op, intensity, duration);
                logger.info("Shocked now waiting: " + Main.config.getWaitBetweenShocks());
                Thread.sleep(Main.config.getWaitBetweenShocks());
            } catch (InterruptedException e) {
                logger.severe("Interrupted during PiShock API call; exception: " + e.getMessage());
                Thread.currentThread().interrupt();
                close();
            }
        } else {
            logger.warning("Unknown shock distribution: " + distribution);
        }
    }

    public int getRandomDevice() {
        List<Integer> deviceIds = config.getDeviceIds();
        if (deviceIds.isEmpty()) {
            throw new IllegalStateException("Device ID list is empty");
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(deviceIds.size());
        return deviceIds.get(randomIndex);
    }


    private int calculateIntensity(int intensity, int maxIntensity) {
        return Math.min(intensity, maxIntensity);
    }

    private void executeShockOperation(int deviceId, OpType op, int intensity, float duration) {
        new Thread(() -> {
            int maxIntensity = config.getMaxShockerIntensity(deviceId);
            int adjustedIntensity = Math.min(intensity, maxIntensity);
            Map<String, Object> data = new HashMap<>();
            data.put("cmd", "operate");
            Map<String, Object> params = new HashMap<>();
            data.put("value", params);
            params.put("id", deviceId);
            params.put("op", op.firmwareCode);
            params.put("intensity", calculateIntensity(adjustedIntensity, maxIntensity));
            params.put("duration", transformDuration(duration));
            doApiCallOnThread(data);
        }).start();
    }


    private int transformDuration(float duration) {
        return Math.round(duration * 1000.0f);
    }

    private synchronized @NonNull SerialPort createAndOpenPort() {
        if (commPort == null || !commPort.isOpen()) {
            commPort = SerialPort.getCommPort(portName);
            commPort.setBaudRate(PISHOCK_SERIAL_BAUD_RATE);
            commPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 100, 0);
            commPort.openPort();
        }
        return commPort;
    }

    private synchronized @NonNull Writer openWriter() {
        if (jsonWriter == null || commPort == null || !commPort.isOpen()) {
            createAndOpenPort();
            jsonWriter = new OutputStreamWriter(commPort.getOutputStream());
        }
        return jsonWriter;
    }

    private void writeAsJson(@NonNull Map<String, Object> data) throws IOException {
        Writer jsonWriter = openWriter();
        jsonWriter.write(gson.toJson(data));
        jsonWriter.write('\n');
        jsonWriter.flush();
    }

    private void doApiCallOnThread(@NonNull Map<String, Object> data) {
        executor.execute(() -> {
            try {
                writeAsJson(data);
            } catch (Exception e) {
                logger.severe("Unable to find an open serial port for PiShock API call: " + e.getMessage());
                close();
            }
        });
    }

    @Override
    public void close() {
        synchronized (this) {
            if (commPort != null) {
                commPort.closePort();
                commPort = null;
            }
            jsonWriter = null;
        }
    }
}
