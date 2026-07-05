package dev.caecorthus.sparkfactionapi.command.admin;

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
    void adminCommandsUseStablePermissionNode() {
        assertEquals(2, SparkFactionPermissions.DEFAULT_COMMAND_LEVEL);
        assertEquals("sparkfactionapi.command.admin", SparkFactionPermissions.COMMAND_ADMIN);
    }

    @Test
    void parsesTaskNamesCaseInsensitivelyWithBreatheAirAlias() {
        assertEquals(TaskCommand.TaskMode.SLEEP, TaskCommand.parseTaskMode("Sleep"));
        assertEquals(TaskCommand.TaskMode.EAT, TaskCommand.parseTaskMode("eat"));
        assertEquals(TaskCommand.TaskMode.DRINK, TaskCommand.parseTaskMode("DRINK"));
        assertEquals(TaskCommand.TaskMode.BREATHE_AIR, TaskCommand.parseTaskMode("Breathe_Air"));
        assertEquals(TaskCommand.TaskMode.BREATHE_AIR, TaskCommand.parseTaskMode("outside"));
        assertEquals(TaskCommand.TaskMode.ALL, TaskCommand.parseTaskMode("All"));
        assertNull(TaskCommand.parseTaskMode("swim"));
    }

    @Test
    void suggestsMixedCaseTaskNamesFromLowerCaseInput() throws ExecutionException, InterruptedException {
        var sleepSuggestions = TaskCommand.suggestTasks(null, new SuggestionsBuilder("s", 0)).get();
        var breatheSuggestions = TaskCommand.suggestTasks(null, new SuggestionsBuilder("b", 0)).get();

        assertEquals(List.of("Sleep"), sleepSuggestions.getList().stream()
                .map(suggestion -> suggestion.getText())
                .toList());
        assertEquals(List.of("Breathe_Air"), breatheSuggestions.getList().stream()
                .map(suggestion -> suggestion.getText())
                .toList());
    }

    @Test
    void convertsSanityPercentToWatheMoodValue() {
        assertEquals(-1.0f, SanityCommand.sanityPercentToMood(-100.0f), 0.0001f);
        assertEquals(0.0f, SanityCommand.sanityPercentToMood(0.0f), 0.0001f);
        assertEquals(1.0f, SanityCommand.sanityPercentToMood(100.0f), 0.0001f);
        assertEquals(1.25f, SanityCommand.sanityPercentToMood(125.0f), 0.0001f);
    }

    @Test
    void addTaskAllOnlySelectsMissingTasks() {
        List<PlayerMoodComponent.Task> missing = TaskCommand.tasksToAdd(
                TaskCommand.TaskMode.ALL,
                EnumSet.of(PlayerMoodComponent.Task.SLEEP, PlayerMoodComponent.Task.EAT)
        );

        assertEquals(List.of(PlayerMoodComponent.Task.OUTSIDE, PlayerMoodComponent.Task.DRINK), missing);
    }

    @Test
    void completeAndDeleteAllOnlyAffectExistingTasks() {
        List<PlayerMoodComponent.Task> existing = TaskCommand.tasksToAffect(
                TaskCommand.TaskMode.ALL,
                EnumSet.of(PlayerMoodComponent.Task.SLEEP, PlayerMoodComponent.Task.DRINK)
        );

        assertEquals(List.of(PlayerMoodComponent.Task.SLEEP, PlayerMoodComponent.Task.DRINK), existing);
    }

    @Test
    void singleTaskSelectionDoesNotCreatePhantomCompletion() {
        List<PlayerMoodComponent.Task> existing = TaskCommand.tasksToAffect(
                TaskCommand.TaskMode.BREATHE_AIR,
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
