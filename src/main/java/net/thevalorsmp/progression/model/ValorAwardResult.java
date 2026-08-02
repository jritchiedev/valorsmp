package net.thevalorsmp.progression.model;

/**
 * Outcome of resolving a PvP kill through {@code ValorScoreService}: the resulting scores for both
 * players, used by the listener to message them.
 *
 * @param killerScore the killer's Valor score after the +1 award
 * @param victimScore the victim's Valor score after the -1 deduction (floored at 0)
 */
public record ValorAwardResult(int killerScore, int victimScore) {

    /** Validates record invariants. */
    public ValorAwardResult {
        if (killerScore < 0 || victimScore < 0) {
            throw new IllegalArgumentException("Valor scores are never negative");
        }
    }
}
