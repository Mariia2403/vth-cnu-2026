package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import java.util.UUID;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

// Entity: один зіграний матч гравця. Чистий Java-клас — без SQL і без HTTP.
public class Match {

    private final String id;
    private final String rosterId;
    private final String playerId;
    private final String mapName;
    private final boolean win;
    private final int roundMargin;
    private final PlayerStats stats;
    private final RatingChange ratingChange;

    private Match(String id, String rosterId, String playerId, String mapName, boolean win,
                  int roundMargin, PlayerStats stats, RatingChange ratingChange) {
        this.id = id;
        this.rosterId = rosterId;
        this.playerId = playerId;
        this.mapName = mapName;
        this.win = win;
        this.roundMargin = roundMargin;
        this.stats = stats;
        this.ratingChange = ratingChange;
    }

    public static Match record(String rosterId, String playerId, String mapName, boolean win,
                               int roundMargin, PlayerStats stats, RatingChange ratingChange) {
        requireText(rosterId, "rosterId");
        requireText(playerId, "playerId");
        requireText(mapName, "mapName");

        if (roundMargin < 0) {                                              // ← TODO 1
            throw new DomainException("INVALID_MATCH", "roundMargin must be >= 0");
        }

        if (stats == null || ratingChange == null) {
            throw new DomainException("INVALID_MATCH", "stats and ratingChange are required");
        }
        return new Match(UUID.randomUUID().toString(), rosterId, playerId, mapName.trim(),
                         win, roundMargin, stats, ratingChange);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException("INVALID_MATCH", field + " is required");
        }
    }

    public String result() {                                                // ← TODO 2
        if (win) {
            return "WIN";
        } else {
            return "LOSS";
        }
    }

    public String getId() { return id; }
    public String getRosterId() { return rosterId; }
    public String getPlayerId() { return playerId; }
    public String getMapName() { return mapName; }
    public boolean isWin() { return win; }
    public int getRoundMargin() { return roundMargin; }
    public PlayerStats getStats() { return stats; }
    public RatingChange getRatingChange() { return ratingChange; }
}