package com.artemis.the.gr8.playerstats.core.commands;

import com.artemis.the.gr8.playerstats.MockBukkitTestBase;
import com.artemis.the.gr8.playerstats.core.utils.EnumHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TabCompleter")
class TabCompleterTest extends MockBukkitTestBase {

    private TabCompleter tabCompleter;
    private Command statCommand;
    private CommandSender console;

    @BeforeEach
    void getTabCompleter() {
        statCommand = server.getPluginCommand("statistic");
        tabCompleter = (TabCompleter) server.getPluginCommand("statistic").getTabCompleter();
        console = server.getConsoleSender();
    }

    private List<String> complete(String... args) {
        return tabCompleter.onTabComplete(console, statCommand, "stat", args);
    }

    @Nested
    @DisplayName("first-argument suggestions")
    class FirstArgument {

        @Test
        @DisplayName("suggests statistic names plus the help/example aliases")
        void includesStatsAndAliases() {
            List<String> suggestions = complete("");
            assertThat(suggestions).contains("jump", "help", "info", "examples");
        }

        @Test
        @DisplayName("does not pollute the shared EnumHandler stat-name list")
        void doesNotMutateSharedStatList() {
            //regression: firstStatCommandArgSuggestions() used to append the
            //help/info/examples aliases to EnumHandler's backing list on every call,
            //growing it without bound and corrupting isStatistic() lookups.
            EnumHandler enumHandler = EnumHandler.getInstance();
            int sizeBefore = enumHandler.getAllStatNames().size();

            complete("");
            complete("ju");
            complete("");

            assertThat(enumHandler.getAllStatNames())
                    .hasSize(sizeBefore)
                    .doesNotContain("help", "info", "examples");
        }
    }
}
