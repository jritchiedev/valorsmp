package net.thevalorsmp.progression;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.thevalorsmp.progression.repository.ValorScoreRepository;
import org.jetbrains.annotations.NotNull;

/** In-memory {@link ValorScoreRepository} double for integration tests (REPOSITORIES.md section 2). */
public final class InMemoryValorScoreRepository implements ValorScoreRepository {

    private final Map<String, Integer> scores = new HashMap<>();

    @Override
    public int findScore(@NotNull UUID playerId, int season) {
        return scores.getOrDefault(key(playerId, season), 0);
    }

    @Override
    public void saveScore(@NotNull UUID playerId, int season, int score) {
        scores.put(key(playerId, season), score);
    }

    private static String key(UUID playerId, int season) {
        return Objects.requireNonNull(playerId, "playerId") + "#" + season;
    }
}
