package dev.caecorthus.sparkfactionapi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.server.command.ServerCommandSource;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SparkFactionAdminCommandsTest {
    @Test
    void registersAdminCommandTreeWithOptionalTargets() {
        CommandDispatcher<ServerCommandSource> dispatcher = new CommandDispatcher<>();

        SparkFactionAdminCommands.register(dispatcher);

        assertTargetedCommand(dispatcher, "sparkfactionapi:clearCooldown");
        assertTaskCommand(dispatcher, "sparkfactionapi:addTask");
        assertTaskCommand(dispatcher, "sparkfactionapi:deleteTask");
        assertTaskCommand(dispatcher, "sparkfactionapi:completeTask");
        assertValueCommand(dispatcher, "sparkfactionapi:setSanity");
    }

    @Test
    void parsesTaskNamesCaseInsensitivelyWithBreatheAirAlias() {
        assertEquals(SparkFactionAdminCommands.TaskMode.SLEEP, SparkFactionAdminCommands.parseTaskMode("Sleep"));
        assertEquals(SparkFactionAdminCommands.TaskMode.EAT, SparkFactionAdminCommands.parseTaskMode("eat"));
        assertEquals(SparkFactionAdminCommands.TaskMode.DRINK, SparkFactionAdminCommands.parseTaskMode("DRINK"));
        assertEquals(SparkFactionAdminCommands.TaskMode.BREATHE_AIR, SparkFactionAdminCommands.parseTaskMode("Breathe_Air"));
        assertEquals(SparkFactionAdminCommands.TaskMode.BREATHE_AIR, SparkFactionAdminCommands.parseTaskMode("outside"));
        assertEquals(SparkFactionAdminCommands.TaskMode.ALL, SparkFactionAdminCommands.parseTaskMode("All"));
        assertNull(SparkFactionAdminCommands.parseTaskMode("swim"));
    }

    @Test
    void suggestsMixedCaseTaskNamesFromLowerCaseInput() throws ExecutionException, InterruptedException {
        var sleepSuggestions = SparkFactionAdminCommands.suggestTasks(null, new SuggestionsBuilder("s", 0)).get();
        var breatheSuggestions = SparkFactionAdminCommands.suggestTasks(null, new SuggestionsBuilder("b", 0)).get();

        assertEquals(List.of("Sleep"), sleepSuggestions.getList().stream()
                .map(suggestion -> suggestion.getText())
                .toList());
        assertEquals(List.of("Breathe_Air"), breatheSuggestions.getList().stream()
                .map(suggestion -> suggestion.getText())
                .toList());
    }

    @Test
    void convertsSanityPercentToWatheMoodValue() {
        assertEquals(-1.0f, SparkFactionAdminCommands.sanityPercentToMood(-100.0f), 0.0001f);
        assertEquals(0.0f, SparkFactionAdminCommands.sanityPercentToMood(0.0f), 0.0001f);
        assertEquals(1.0f, SparkFactionAdminCommands.sanityPercentToMood(100.0f), 0.0001f);
        assertEquals(1.25f, SparkFactionAdminCommands.sanityPercentToMood(125.0f), 0.0001f);
    }

    @Test
    void addTaskAllOnlySelectsMissingTasks() {
        List<PlayerMoodComponent.Task> missing = SparkFactionAdminCommands.tasksToAdd(
                SparkFactionAdminCommands.TaskMode.ALL,
                EnumSet.of(PlayerMoodComponent.Task.SLEEP, PlayerMoodComponent.Task.EAT)
        );

        assertEquals(List.of(PlayerMoodComponent.Task.OUTSIDE, PlayerMoodComponent.Task.DRINK), missing);
    }

    @Test
    void completeAndDeleteAllOnlyAffectExistingTasks() {
        List<PlayerMoodComponent.Task> existing = SparkFactionAdminCommands.tasksToAffect(
                SparkFactionAdminCommands.TaskMode.ALL,
                EnumSet.of(PlayerMoodComponent.Task.SLEEP, PlayerMoodComponent.Task.DRINK)
        );

        assertEquals(List.of(PlayerMoodComponent.Task.SLEEP, PlayerMoodComponent.Task.DRINK), existing);
    }

    @Test
    void singleTaskSelectionDoesNotCreatePhantomCompletion() {
        List<PlayerMoodComponent.Task> existing = SparkFactionAdminCommands.tasksToAffect(
                SparkFactionAdminCommands.TaskMode.BREATHE_AIR,
                EnumSet.of(PlayerMoodComponent.Task.SLEEP)
        );

        assertEquals(List.of(), existing);
    }

    private static void assertTargetedCommand(CommandDispatcher<ServerCommandSource> dispatcher, String commandName) {
        CommandNode<ServerCommandSource> command = dispatcher.getRoot().getChild(commandName);
        assertNotNull(command);
        assertNotNull(command.getCommand());
        CommandNode<ServerCommandSource> targets = command.getChild("targets");
        assertNotNull(targets);
        assertNotNull(targets.getCommand());
    }

    private static void assertTaskCommand(CommandDispatcher<ServerCommandSource> dispatcher, String commandName) {
        CommandNode<ServerCommandSource> command = dispatcher.getRoot().getChild(commandName);
        assertNotNull(command);
        CommandNode<ServerCommandSource> task = command.getChild("task");
        assertNotNull(task);
        assertNotNull(task.getCommand());
        CommandNode<ServerCommandSource> targets = task.getChild("targets");
        assertNotNull(targets);
        assertNotNull(targets.getCommand());
    }

    private static void assertValueCommand(CommandDispatcher<ServerCommandSource> dispatcher, String commandName) {
        CommandNode<ServerCommandSource> command = dispatcher.getRoot().getChild(commandName);
        assertNotNull(command);
        CommandNode<ServerCommandSource> value = command.getChild("value");
        assertNotNull(value);
        assertNotNull(value.getCommand());
        CommandNode<ServerCommandSource> targets = value.getChild("targets");
        assertNotNull(targets);
        assertNotNull(targets.getCommand());
    }
}
