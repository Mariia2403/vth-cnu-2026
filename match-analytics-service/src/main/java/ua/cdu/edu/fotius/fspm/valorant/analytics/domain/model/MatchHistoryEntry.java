package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

// Один рядок історії матчів гравця (лише для читання).
public record MatchHistoryEntry(String matchId, String mapName, String result,
                                int deltaRR, String tierStatus) {
}
