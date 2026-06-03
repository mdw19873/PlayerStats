package com.artemis.the.gr8.playerstats.core.utils;

import com.artemis.the.gr8.playerstats.core.Main;
import com.artemis.the.gr8.playerstats.core.config.ConfigHandler;
import com.artemis.the.gr8.playerstats.core.multithreading.ThreadManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * A utility class that deals with OfflinePlayers. It stores a list
 * of all OfflinePlayer-names that need to be included in statistic
 * calculations, and can retrieve the corresponding OfflinePlayer
 * object for a given player-name.
 */
public final class OfflinePlayerHandler extends YamlFileHandler implements Closable {

    private static volatile OfflinePlayerHandler instance;
    private final ConfigHandler config;

    //a single reusable background thread for (re)loading the offline-player list,
    //rather than spinning up and tearing down an ExecutorService on every reload.
    //Daemon so it never keeps the JVM alive; shut down in close() on plugin disable.
    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PlayerStats-OfflinePlayerLoader");
        thread.setDaemon(true);
        return thread;
    });
    private static ConcurrentHashMap<String, UUID> includedPlayerUUIDs;
    private static ConcurrentHashMap<String, UUID> excludedPlayerUUIDs;
    //mirrors the values of excludedPlayerUUIDs so isExcludedPlayer(UUID) is O(1)
    //instead of an O(n) ConcurrentHashMap.containsValue scan (hit once per player
    //on every reload, see PlayerLoadAction).
    private static Set<UUID> excludedPlayerUUIDSet;

    private OfflinePlayerHandler() {
        super("excluded_players.yml");
        config = ConfigHandler.getInstance();

        loadOfflinePlayers();
        Main.registerReloadable(this);
        Main.registerClosable(this);
    }

    public static OfflinePlayerHandler getInstance() {
        OfflinePlayerHandler localVar = instance;
        if (localVar != null) {
            return localVar;
        }

        synchronized (OfflinePlayerHandler.class) {
            if (instance == null) {
                instance = new OfflinePlayerHandler();
            }
            return instance;
        }
    }

    @Override
    public void reload() {
        super.reload();
        loadOfflinePlayers();
    }

    /**
     * Checks if a given player is currently
     * included for /statistic lookups.
     *
     * @param playerName String (case-sensitive)
     * @return true if this player is included
     */
    public boolean isIncludedPlayer(String playerName) {
        return includedPlayerUUIDs.containsKey(playerName);
    }

    public boolean isExcludedPlayer(String playerName) {
        return excludedPlayerUUIDs.containsKey(playerName);
    }

    public boolean isExcludedPlayer(UUID uniqueID) {
        return excludedPlayerUUIDSet.contains(uniqueID);
    }

    public boolean addPlayerToExcludeList(String playerName) {
        if (isIncludedPlayer(playerName)) {
            UUID uuid = includedPlayerUUIDs.get(playerName);

            super.writeEntryToList("excluded", uuid.toString());
            includedPlayerUUIDs.remove(playerName);
            excludedPlayerUUIDs.put(playerName, uuid);
            excludedPlayerUUIDSet.add(uuid);
            return true;
        }
        return false;
    }

    public boolean removePlayerFromExcludeList(String playerName) {
        if (isExcludedPlayer(playerName)) {
            UUID uuid = excludedPlayerUUIDs.get(playerName);

            super.removeEntryFromList("excluded", uuid.toString());
            excludedPlayerUUIDs.remove(playerName);
            excludedPlayerUUIDSet.remove(uuid);
            includedPlayerUUIDs.put(playerName, uuid);
            return true;
        }
        return false;
    }

    public void addNewIncludedPlayer(@NotNull Player player) {
        includedPlayerUUIDs.put(player.getName(), player.getUniqueId());
    }

    @Contract(" -> new")
    public @NotNull ArrayList<String> getExcludedPlayerNames() {
        return new ArrayList<>(excludedPlayerUUIDs.keySet());
    }

    /**
     * Gets an ArrayList of names from all OfflinePlayers that should
     * be included in statistic calculations.
     *
     * @return the ArrayList
     */
    @Contract(" -> new")
    public @NotNull ArrayList<String> getIncludedOfflinePlayerNames() {
        return new ArrayList<>(includedPlayerUUIDs.keySet());
    }

    /**
     * Gets the number of OfflinePlayers that are
     * currently included in statistic calculations.
     *
     * @return the number of included OfflinePlayers
     */
    public int getIncludedPlayerCount() {
        return includedPlayerUUIDs.size();
    }

    /**
     * Uses the playerName to get the player's UUID from a private HashMap,
     * and uses the UUID to get the corresponding OfflinePlayer Object.
     *
     * @param playerName name of the target player (case-sensitive)
     * @return OfflinePlayer
     * @throws IllegalArgumentException if this player is not on the list
     * of players that should be included in statistic calculations
     */
    public @NotNull OfflinePlayer getIncludedOfflinePlayer(String playerName) throws IllegalArgumentException {
        UUID uuid = includedPlayerUUIDs.get(playerName);
        if (uuid != null) {
            return Bukkit.getOfflinePlayer(uuid);
        }
        else {
            MyLogger.logWarning("Cannot calculate statistics for player-name: " + playerName +
                    "! Double-check if the name is spelled correctly (including capital letters), " +
                    "or if any of your config settings exclude them");
            throw new IllegalArgumentException("PlayerStats does not know a player by this name");
        }
    }

    public @NotNull OfflinePlayer getExcludedOfflinePlayer(String playerName) throws IllegalArgumentException {
        UUID uuid = excludedPlayerUUIDs.get(playerName);
        if (uuid != null) {
            return Bukkit.getOfflinePlayer(uuid);
        }
        throw new IllegalArgumentException("There is no player on the exclude-list with this name");
    }

    private void loadOfflinePlayers() {
        loadExecutor.execute(() -> {
            loadExcludedPlayerNames();
            loadIncludedOfflinePlayers();
        });
    }

    @Override
    public void close() {
        loadExecutor.shutdown();
    }

    private void loadIncludedOfflinePlayers() {
        long startTime = System.currentTimeMillis();

        OfflinePlayer[] offlinePlayers;
        if (config.whitelistOnly()) {
            offlinePlayers = getWhitelistedPlayers();
        } else if (config.excludeBanned()) {
            offlinePlayers = getNonBannedPlayers();
        } else {
            offlinePlayers = Bukkit.getOfflinePlayers();
        }

        int size = includedPlayerUUIDs != null ? includedPlayerUUIDs.size() : 16;
        includedPlayerUUIDs = new ConcurrentHashMap<>(size);

        ForkJoinPool.commonPool().invoke(ThreadManager.getPlayerLoadAction(offlinePlayers, includedPlayerUUIDs));

        MyLogger.actionFinished();
        MyLogger.logLowLevelTask(("Loaded " + includedPlayerUUIDs.size() + " offline players"), startTime);
    }

    private void loadExcludedPlayerNames() {
        long time = System.currentTimeMillis();

        excludedPlayerUUIDs = new ConcurrentHashMap<>();
        excludedPlayerUUIDSet = ConcurrentHashMap.newKeySet();
        List<String> excluded = super.getFileConfiguration().getStringList("excluded");
        excluded.stream()
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                        .forEach(uuid -> {
                            excludedPlayerUUIDSet.add(uuid);
                            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                            String playerName = player.getName();
                            if (playerName != null) {
                                excludedPlayerUUIDs.put(playerName, uuid);
                            }
                        });

        MyLogger.logLowLevelTask("Loaded " + excludedPlayerUUIDs.size() + " excluded players from file", time);
    }

    private OfflinePlayer[] getWhitelistedPlayers() {
        return Bukkit.getWhitelistedPlayers().toArray(OfflinePlayer[]::new);
    }

    private @NotNull OfflinePlayer[] getNonBannedPlayers() {
        if (Bukkit.getPluginManager().isPluginEnabled("LiteBans")) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .parallel()
                    .filter(Predicate.not(OfflinePlayer::isBanned))
                    .toArray(OfflinePlayer[]::new);
        }

        Set<OfflinePlayer> banList = Bukkit.getBannedPlayers();
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .parallel()
                .filter(Predicate.not(banList::contains))
                .toArray(OfflinePlayer[]::new);
    }
}