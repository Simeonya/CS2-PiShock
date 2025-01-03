package me.simeonya;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import me.simeonya.pishock.OpType;
import me.simeonya.pishock.PiShockApi;
import me.simeonya.pishock.PiShockSerialApi;
import me.simeonya.pishock.PishockZapConfig;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static javax.sound.sampled.AudioSystem.getAudioInputStream;

/**
 * Main class that integrates all components of the application.
 * It manages configuration loading, server setup, and shocker device control.
 * Credits to me (https://github.com/simeonya/)
 */
public class Main {

    public static PishockZapConfig config;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger logger = Logger.getLogger("PISHOCK-CS2");
    private static final String CONFIG_FILE = "config.json";
    private static List<PiShockApi> shockers = new ArrayList<>();
    private static int previousHealth = 100;
    private static boolean inWarmup = true;

    public static void main(String[] args) {
        initializeApplication();
    }

    /**
     * Initializes the application by loading the configuration, setting up devices, and starting the server.
     */
    private static void initializeApplication() {
        loadConfiguration();
        if (config.isEnabled()) {
            setupShockers();
            startServer();
            setupGlobalKeyListener();
        }
    }


    /**
     * Plays a sound file to indicate that the application has started.
     */
    private static void playSound() {
        try {
            String sound = "sound.wav";
            String currentPath = System.getProperty("user.dir");
            File soundFile = new File(currentPath, sound);
            if (!soundFile.exists()) {
                downloadFile("https://raw.githubusercontent.com/Simeonya/CS2-PiShock/refs/heads/master/sound/sound.wav", soundFile);
            }
            play(soundFile.getAbsolutePath());
        } catch (Exception e) {
            logger.severe("Failed to play sound: " + e.getMessage());
        }
    }

    /**
     * Downloads a file from a URL to a destination file.
     * @param fileURL the URL of the file to download
     * @param destination the destination file
     * @throws IOException if an I/O error occurs
     */
    private static void downloadFile(String fileURL, File destination) throws IOException {
        HttpURLConnection httpConn = (HttpURLConnection) new URL(fileURL).openConnection();
        int responseCode = httpConn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            int contentLength = httpConn.getContentLength();
            try (InputStream inputStream = httpConn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destination)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                int percentCompleted;
                long fileSize = contentLength;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    percentCompleted = (int) (totalBytesRead * 100 / fileSize);
                    logger.info("Download " + percentCompleted + "%\r");
                }
                logger.info("Download complete");
            }
        } else {
            throw new IOException("No file to download. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }

    /**
     * Plays an audio file.
     *
     * @param filePath the path to the audio file
     */
    private static void play(String filePath) {
        final File file = new File(filePath);

        try (final AudioInputStream in = getAudioInputStream(file)) {
            final AudioFormat outFormat = getOutFormat(in.getFormat());
            final Line.Info info = new Line.Info(SourceDataLine.class);

            try (final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                if (line != null) {
                    line.open(outFormat);
                    line.start();
                    stream(getAudioInputStream(outFormat, in), line);
                    line.drain();
                    line.stop();
                }
            }

        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts the input audio format to PCM signed format.
     *
     * @param inFormat the input audio format
     * @return the output audio format
     */
    private static AudioFormat getOutFormat(AudioFormat inFormat) {
        final int ch = inFormat.getChannels();
        final float rate = inFormat.getSampleRate();
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
    }

    /**
     * Streams the audio input stream to the source data line.
     *
     * @param in   the audio input stream
     * @param line the source data line
     * @throws IOException if an I/O error occurs
     */
    private static void stream(AudioInputStream in, SourceDataLine line) throws IOException {
        final byte[] buffer = new byte[4096];
        for (int n = 0; n != -1; n = in.read(buffer, 0, buffer.length)) {
            line.write(buffer, 0, n);
        }
    }

    /**
     * Loads the configuration from a JSON file, creating a default if none exists.
     */
    private static void loadConfiguration() {
        try (Reader reader = Files.newBufferedReader(Paths.get(CONFIG_FILE))) {
            config = gson.fromJson(reader, PishockZapConfig.class);
            logger.info("Configuration loaded successfully.");
        } catch (IOException e) {
            logger.warning("Failed to load configuration, creating default: " + e.getMessage());
            config = PishockZapConfig.builder().build();
            saveConfiguration();
        }
    }

    /**
     * Saves the configuration to a JSON file.
     */
    private static void saveConfiguration() {
        try (Writer writer = Files.newBufferedWriter(Paths.get(CONFIG_FILE))) {
            gson.toJson(config, writer);
            logger.info("Configuration saved successfully.");
        } catch (IOException e) {
            logger.severe("Failed to save configuration: " + e.getMessage());
        }
    }

    /**
     * Sets up shocker devices based on the loaded configuration.
     */
    private static void setupShockers() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        PiShockApi api = new PiShockSerialApi(config, executorService, config.getSerialPort());
        shockers.add(api);
        if (config.isStartUpVibrate()) {
            api.performOp(config.getShockDistribution(), OpType.VIBRATE, config.getVibrationIntensityMin(), 1.0f);
        }
    }

    /**
     * Starts the HTTP server to handle game state changes.
     */
    private static void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(config.getWebPort()), 0);
            server.createContext("/", new GameHandler());
            server.start();
            logger.info("Server started on port " + config.getWebPort() + ".");
            playSound();
        } catch (IOException e) {
            logger.severe("Failed to start server: " + e.getMessage());
        }
    }

    /**
     * Sets up a global key listener to handle system-wide key events.
     */
    private static void setupGlobalKeyListener() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new GlobalKeyListener());
        } catch (NativeHookException e) {
            logger.severe("Problem registering the native hook: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * HTTP handler that processes POST requests to handle game state changes.
     */
    static class GameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String post_data = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8")).lines().collect(Collectors.joining("\n"));
                GameState gameState = gson.fromJson(post_data, GameState.class);
                handleGameState(gameState);
                exchange.sendResponseHeaders(200, -1);
            }
        }
    }

    /**
     * Processes the current game state, adjusting shocker behavior based on player health and activity.
     */
    private static void handleGameState(GameState gameState) {
        if (gameState == null || gameState.player == null) return;

        String playerName = gameState.player.name;
        int playerHealth = gameState.player.state.health;
        String roundPhase = gameState.round.phase;
        String playerActivity = gameState.player.activity;

        logger.info(String.format("Player name: %s, Activity: %s", playerName, playerActivity));

        if (!playerName.equals(config.getPlayerNameToShock()) || !playerActivity.equals("playing")) {
            logger.info("Player not target or not playing, skipping shocks.");
            return;
        }

        updateWarmupStatus(roundPhase, playerHealth);
        if (shouldShockPlayer(playerHealth)) {
            shockPlayer();
        }
    }

    /**
     * Updates the warmup status based on the round phase.
     */
    private static void updateWarmupStatus(String roundPhase, int playerHealth) {
        switch (roundPhase) {
            case "warmup":
                inWarmup = true;
                break;
            case "live":
                if (inWarmup) {
                    inWarmup = false;
                    previousHealth = playerHealth;
                }
                break;
            case "over":
                resetGame();
                break;
        }
    }

    /**
     * Resets the game status to default after a round is over.
     */
    private static void resetGame() {
        previousHealth = 100;
        inWarmup = true;
    }

    /**
     * Determines if a player should be shocked based on health changes.
     */
    private static boolean shouldShockPlayer(int playerHealth) {
        int damageTaken = previousHealth - playerHealth;
        if (damageTaken > 0) {
            previousHealth = playerHealth;
            return true;
        }
        return false;
    }

    /**
     * Shocks the player based on the configuration settings.
     */
    private static void shockPlayer() {
        for (PiShockApi shocker : shockers) {
            if (config.isVibrationOnly()) {
                shocker.performOp(config.getShockDistribution(), OpType.VIBRATE, config.getVibrationIntensityMin(), 1.0f);
            } else {
                shocker.performOp(config.getShockDistribution(), OpType.SHOCK, config.getShockIntensityMin(), 1.0f);
            }
        }
    }

    static class GameState {
        Player player;
        Round round;

        static class Player {
            String name;
            PlayerState state;
            String activity;
        }

        static class PlayerState {
            int health;
        }

        static class Round {
            String phase;
        }
    }

    /**
     * Key listener that allows for application exit through a specific key combination.
     */
    static class GlobalKeyListener implements NativeKeyListener {
        @Override
        public void nativeKeyPressed(NativeKeyEvent e) {
            if (e.getModifiers() == NativeKeyEvent.CTRL_L_MASK && e.getKeyCode() == NativeKeyEvent.VC_M) {
                System.out.println("CTRL + M pressed. Terminating program.");
                System.exit(0);
            }
        }
    }
}
