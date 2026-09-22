package net.thevalorsmp.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.thevalorsmp.core.command.AdminCommand;
import net.thevalorsmp.core.event.DomainEventPublisher;
import net.thevalorsmp.progression.InMemoryValorScoreRepository;
import net.thevalorsmp.progression.repository.ValorScoreRepository;
import net.thevalorsmp.progression.service.ValorScoreService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.slf4j.helpers.NOPLogger;

/**
 * Exercises {@code /valorsmp valor} end to end under MockBukkit: the real plugin supplies its
 * config, while the command gets a {@link ValorScoreService} backed by an in-memory repository so
 * score effects can be asserted directly (TESTING.md section 3).
 */
class AdminCommandIntegrationTest {

    private ServerMock server;
    private ValorPlugin plugin;
    private PlayerMock sender;
    private PlayerMock target;
    private AdminCommand command;
    private ValorScoreRepository repository;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(ValorPlugin.class);
        sender = server.addPlayer("Admin");
        target = server.addPlayer("Target");
        repository = new InMemoryValorScoreRepository();
        DomainEventPublisher publisher = plugin.getServer().getPluginManager()::callEvent;
        ValorScoreService valorScoreService = new ValorScoreService(
                repository, plugin.getConfigService().progression(), publisher, NOPLogger.NOP_LOGGER);
        command = new AdminCommand(plugin, plugin.getConfigService(), valorScoreService);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void valorGive_targetOnline_addsScoreAndNotifiesBoth() {
        run(sender, "valor", "give", "Target", "5");

        assertThat(repository.findScore(target.getUniqueId(), 1)).isEqualTo(5);
        assertThat(lastMessage(sender)).contains("Target now has 5 Valor");
        assertThat(lastMessage(target)).contains("Your Valor was set to 5");
    }

    @Test
    void valorSet_aboveMax_clampsAtCap() {
        run(sender, "valor", "set", "Target", "25");

        assertThat(repository.findScore(target.getUniqueId(), 1)).isEqualTo(20);
        assertThat(lastMessage(sender)).contains("now has 20 Valor");
    }

    @Test
    void valorTake_subtractsScore() {
        repository.saveScore(target.getUniqueId(), 1, 20);

        run(sender, "valor", "take", "Target", "3");

        assertThat(repository.findScore(target.getUniqueId(), 1)).isEqualTo(17);
    }

    @Test
    void valor_unknownPlayer_reportsErrorAndChangesNothing() {
        run(sender, "valor", "give", "NoSuchPlayerXyz", "5");

        assertThat(lastMessage(sender)).contains("Unknown player: NoSuchPlayerXyz");
    }

    @Test
    void valor_badAmount_showsUsageAndChangesNothing() {
        repository.saveScore(target.getUniqueId(), 1, 4);

        run(sender, "valor", "give", "Target", "abc");

        assertThat(repository.findScore(target.getUniqueId(), 1)).isEqualTo(4);
        assertThat(lastMessage(sender)).contains("Usage: /valorsmp valor");
    }

    @Test
    void onTabComplete_secondArg_filtersValorActions() {
        List<String> completions =
                command.onTabComplete(sender, null, "valorsmp", new String[] {"valor", "s"});

        assertThat(completions).containsExactly("set");
    }

    private void run(CommandSender who, String... args) {
        command.onCommand(who, null, "valorsmp", args);
    }

    private static String lastMessage(PlayerMock player) {
        StringBuilder messages = new StringBuilder();
        net.kyori.adventure.text.Component message;
        while ((message = player.nextComponentMessage()) != null) {
            messages.append(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(message)).append('\n');
        }
        return messages.toString();
    }
}
