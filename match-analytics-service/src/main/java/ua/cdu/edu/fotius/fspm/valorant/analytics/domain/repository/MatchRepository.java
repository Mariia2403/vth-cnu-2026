package ua.cdu.edu.fotius.fspm.valorant.analytics.domain.repository;

import java.util.List;

import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.Match;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchHistoryEntry;

// "Розетка": що вміє сховище матчів. Жодного слова про SQL чи PostgreSQL.
public interface MatchRepository {

    void save(Match match);

    List<MatchHistoryEntry> findHistoryByPlayerId(String playerId);
}
