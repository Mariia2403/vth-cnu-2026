package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

// Value Object: статистика гравця за один матч.
public record PlayerStats(int kills, int deaths, int assists, int combatScore) {

    public PlayerStats {
        if (kills < 0) {
            throw new DomainException("INVALID_STATS", "kills must be >= 0");
        }
        if (deaths < 0) {
            throw new DomainException("INVALID_STATS", "deaths must be >= 0");
        }
        if (assists < 0) {
            throw new DomainException("INVALID_STATS", "assists must be >= 0");
        }
        if (combatScore < 0) {
            throw new DomainException("INVALID_STATS", "combatScore must be >= 0");
        }
    }
}