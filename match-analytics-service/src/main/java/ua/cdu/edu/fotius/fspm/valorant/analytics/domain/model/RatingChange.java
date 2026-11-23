package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;

// Value Object: результат розрахунку рейтингу від Rank Calculator.
public record RatingChange(int deltaRR, String tierStatus) {

    public RatingChange {
        if (tierStatus == null || tierStatus.isBlank()) {
            throw new DomainException("INVALID_RATING", "tierStatus is required");
        }
    }
}