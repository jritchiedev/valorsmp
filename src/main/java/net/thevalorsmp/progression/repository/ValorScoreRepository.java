package net.thevalorsmp.progression.repository;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Persistence for per-season Valor scores (REPOSITORIES.md, DATABASE.md {@code valor_scores}).
 * Owns no business rules: flooring at 0 and the +1/-1 award logic live in {@code ValorScoreService}.
 */
public interface ValorScoreRepository {

    /**
     * Returns a player's current-season score, defaulting to 0 when no row exists yet.
     *
     * @param playerId player UUID
     * @param season   season number
     * @return the stored score, or 0 if the player has none for this season
     */
    int findScore(@NotNull UUID playerId, int season);

    /**
     * Stores a player's score for a season, inserting the row if absent.
     *
     * @param playerId player UUID
     * @param season   season number
     * @param score    the absolute score to persist; must be non-negative
     */
    void saveScore(@NotNull UUID playerId, int season, int score);
}
