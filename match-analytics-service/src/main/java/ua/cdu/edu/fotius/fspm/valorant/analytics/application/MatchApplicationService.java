package ua.cdu.edu.fotius.fspm.valorant.analytics.application;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.exception.DomainException;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.Match;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.MatchHistoryEntry;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.PlayerStats;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.model.RatingChange;
import ua.cdu.edu.fotius.fspm.valorant.analytics.domain.repository.MatchRepository;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RankCalculatorClient;
import ua.cdu.edu.fotius.fspm.valorant.analytics.infrastructure.client.RosterServiceClient;

// "Шеф-кухар": координує прийом результату матчу (Use Case).
// Не знає про HTTP-запити від браузера і не пише SQL.
@Singleton
public class MatchApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MatchApplicationService.class);

    // Вхідні дані use case (не залежить від формату HTTP-запиту).
    public record SubmitMatchCommand(String rosterId, String playerId, String mapName, boolean win,
                                     int roundMargin, int kills, int deaths, int assists,
                                     int combatScore, boolean afk) {}

    private final MatchRepository matchRepository;
    private final RankCalculatorClient rankClient;
    private final RosterServiceClient rosterClient;

    @Inject
    public MatchApplicationService(MatchRepository matchRepository,
                                   RankCalculatorClient rankClient,
                                   RosterServiceClient rosterClient) {
        this.matchRepository = matchRepository;
        this.rankClient = rankClient;
        this.rosterClient = rosterClient;
    }

    public Match submitMatch(SubmitMatchCommand cmd, String authHeader) {
        // 1. Доменна валідація статистики — одразу, ще до дзвінків іншим сервісам (fail-fast)
        PlayerStats stats = new PlayerStats(cmd.kills(), cmd.deaths(), cmd.assists(), cmd.combatScore());

        // 2. Склад має бути LOCKED
        String rosterStatus = fetchRosterStatus(cmd.rosterId(), authHeader);
        if ("ARCHIVED".equals(rosterStatus)) {
            throw new DomainException("MATCH_ALREADY_SUBMITTED",
                "Match already submitted for roster " + cmd.rosterId());
        }
        if (!"LOCKED".equals(rosterStatus)) {
            throw new DomainException("ROSTER_NOT_LOCKED",
                "Roster must be LOCKED before match submission (current status: " + rosterStatus + ")");
        }

        // 3. Розрахунок рейтингу в Rank Calculator
        var rankRes = rankClient.calculateRating(new RankCalculatorClient.RankCalculationRequest(
            cmd.win(), cmd.roundMargin(), cmd.kills(), cmd.deaths(), cmd.assists(),
            cmd.combatScore(), cmd.afk()));
        RatingChange rating = new RatingChange(rankRes.deltaRR(), rankRes.tierStatus());
        log.info("Rank calculated for roster {}: deltaRR={}, tier={}",
            cmd.rosterId(), rating.deltaRR(), rating.tierStatus());

        // Створюємо доменний об'єкт матчу (тут спрацюють перевірки Match.record)
        Match match = Match.record(cmd.rosterId(), cmd.playerId(), cmd.mapName(), cmd.win(),
                                   cmd.roundMargin(), stats, rating);

        // 4. Архівуємо склад (ДО запису в БД)
        archiveRoster(cmd.rosterId(), authHeader);

        // 5. Зберігаємо матч; якщо не вдалося — компенсація (Saga): повертаємо склад з архіву
        try {
            matchRepository.save(match);
        } catch (Exception e) {
            compensateArchive(cmd.rosterId(), authHeader);
            throw new DomainException("PERSISTENCE_FAILED", "Failed to persist match: " + e.getMessage());
        }

        log.info("Match recorded: matchId={}", match.getId());
        return match;
    }

    public List<MatchHistoryEntry> getHistory(String playerId) {
        return matchRepository.findHistoryByPlayerId(playerId);
    }

    private String fetchRosterStatus(String rosterId, String authHeader) {
        try {
            return rosterClient.getRoster(rosterId, authHeader).status();
        } catch (HttpClientResponseException e) {
            if (e.getStatus().getCode() == 404) {
                throw new DomainException("ROSTER_NOT_FOUND", "Roster not found for roster " + rosterId);
            }
            throw new DomainException("ROSTER_UNAVAILABLE",
                "Could not verify roster status for roster " + rosterId + ": " + e.getMessage());
        } catch (Exception e) {
            throw new DomainException("ROSTER_UNAVAILABLE",
                "Could not verify roster status for roster " + rosterId + ": " + e.getMessage());
        }
    }

    private void archiveRoster(String rosterId, String authHeader) {
        try {
            rosterClient.archiveRoster(rosterId, authHeader);
        } catch (HttpClientResponseException e) {
            if (e.getStatus().getCode() == 409) {
                throw new DomainException("MATCH_ALREADY_SUBMITTED",
                    "Match already submitted for roster " + rosterId);
            }
            throw new DomainException("ARCHIVE_FAILED",
                "Failed to archive roster " + rosterId + ": " + e.getMessage());
        } catch (Exception e) {
            throw new DomainException("ARCHIVE_FAILED",
                "Failed to archive roster " + rosterId + ": " + e.getMessage());
        }
    }

    private void compensateArchive(String rosterId, String authHeader) {
        try {
            rosterClient.unarchiveRoster(rosterId, authHeader);
            log.info("Compensated: roster {} unarchived", rosterId);
        } catch (Exception e) {
            log.warn("Compensation failed for roster {}: {}", rosterId, e.getMessage());
        }
    }
}
