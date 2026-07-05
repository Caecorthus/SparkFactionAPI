package dev.caecorthus.sparkfactionapi.command.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.TaskCompletePayload;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class TaskCommand {
    private static final DynamicCommandExceptionType INVALID_TASK =
            new DynamicCommandExceptionType(task -> Text.literal("Unknown SparkFactionAPI task: " + task));
    private static final List<String> TASK_SUGGESTIONS = List.of("Sleep", "Eat", "Drink", "Breathe_Air", "outside", "All");

    private TaskCommand() {
    }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        registerTaskCommand(dispatcher, "sparkfactionapi:addTask", TaskAction.ADD);
        registerTaskCommand(dispatcher, "sparkfactionapi:deleteTask", TaskAction.DELETE);
        registerTaskCommand(dispatcher, "sparkfactionapi:completeTask", TaskAction.COMPLETE);
    }

    private static void registerTaskCommand(
            CommandDispatcher<ServerCommandSource> dispatcher,
            String literal,
            TaskAction action
    ) {
        dispatcher.register(CommandManager.literal(literal)
                .requires(Permissions.require(
                        SparkFactionPermissions.COMMAND_ADMIN,
                        SparkFactionPermissions.DEFAULT_COMMAND_LEVEL
                ))
                .then(CommandManager.argument("task", StringArgumentType.word())
                        .suggests(TaskCommand::suggestTasks)
                        .executes(context -> updateTasks(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrThrow()),
                                taskMode(context),
                                action
                        ))
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(context -> updateTasks(
                                        context.getSource(),
                                        EntityArgumentType.getPlayers(context, "targets"),
                                        taskMode(context),
                                        action
                                )))));
    }

    private static int updateTasks(
            ServerCommandSource source,
            Collection<ServerPlayerEntity> targets,
            TaskMode mode,
            TaskAction action
    ) {
        int changed = 0;
        for (ServerPlayerEntity target : targets) {
            changed += switch (action) {
                case ADD -> addTasks(target, mode);
                case DELETE -> deleteTasks(target, mode);
                case COMPLETE -> completeTasks(target, mode);
            };
        }
        int finalChanged = changed;
        source.sendFeedback(() -> Text.literal(action.feedbackVerb + " " + finalChanged + " task(s)."), true);
        return changed;
    }

    private static int addTasks(ServerPlayerEntity player, TaskMode mode) {
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        int changed = 0;
        for (PlayerMoodComponent.Task task : tasksToAdd(mode, mood.tasks.keySet())) {
            mood.tasks.put(task, createTask(task));
            mood.timesGotten.putIfAbsent(task, 1);
            mood.timesGotten.put(task, mood.timesGotten.get(task) + 1);
            changed++;
        }
        if (changed > 0) {
            mood.sync();
        }
        return changed;
    }

    private static int deleteTasks(ServerPlayerEntity player, TaskMode mode) {
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        int changed = 0;
        for (PlayerMoodComponent.Task task : tasksToAffect(mode, mood.tasks.keySet())) {
            if (mood.tasks.remove(task) != null) {
                changed++;
            }
        }
        if (changed > 0) {
            mood.sync();
        }
        return changed;
    }

    private static int completeTasks(ServerPlayerEntity player, TaskMode mode) {
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        boolean hasRealMood = role != null && role.getMoodType() == Role.MoodType.REAL;
        int changed = 0;
        for (PlayerMoodComponent.Task task : tasksToAffect(mode, mood.tasks.keySet())) {
            if (!mood.tasks.containsKey(task)) {
                continue;
            }
            if (hasRealMood) {
                mood.setMood(mood.getMood() + GameConstants.MOOD_GAIN);
            }
            ServerPlayNetworking.send(player, new TaskCompletePayload());
            TaskComplete.EVENT.invoker().onTaskComplete(player, task);
            mood.tasks.remove(task);
            changed++;
        }
        if (changed > 0) {
            mood.sync();
        }
        return changed;
    }

    private static TaskMode taskMode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String rawTask = StringArgumentType.getString(context, "task");
        TaskMode mode = parseTaskMode(rawTask);
        if (mode == null) {
            throw INVALID_TASK.create(rawTask);
        }
        return mode;
    }

    static TaskMode parseTaskMode(String task) {
        return switch (task.toLowerCase()) {
            case "sleep" -> TaskMode.SLEEP;
            case "eat" -> TaskMode.EAT;
            case "drink" -> TaskMode.DRINK;
            case "breathe_air", "outside" -> TaskMode.BREATHE_AIR;
            case "all" -> TaskMode.ALL;
            default -> null;
        };
    }

    static List<PlayerMoodComponent.Task> tasksToAdd(TaskMode mode, Set<PlayerMoodComponent.Task> existingTasks) {
        List<PlayerMoodComponent.Task> candidates = mode.tasks();
        List<PlayerMoodComponent.Task> result = new ArrayList<>();
        for (PlayerMoodComponent.Task task : candidates) {
            if (!existingTasks.contains(task)) {
                result.add(task);
            }
        }
        return result;
    }

    static List<PlayerMoodComponent.Task> tasksToAffect(TaskMode mode, Set<PlayerMoodComponent.Task> existingTasks) {
        List<PlayerMoodComponent.Task> candidates = mode == TaskMode.ALL
                ? List.of(PlayerMoodComponent.Task.values())
                : mode.tasks();
        List<PlayerMoodComponent.Task> result = new ArrayList<>();
        for (PlayerMoodComponent.Task task : candidates) {
            if (existingTasks.contains(task)) {
                result.add(task);
            }
        }
        return result;
    }

    private static PlayerMoodComponent.TrainTask createTask(PlayerMoodComponent.Task task) {
        return switch (task) {
            case SLEEP -> new PlayerMoodComponent.SleepTask(GameConstants.SLEEP_TASK_DURATION);
            case OUTSIDE -> new PlayerMoodComponent.OutsideTask(GameConstants.OUTSIDE_TASK_DURATION);
            case EAT -> new PlayerMoodComponent.EatTask();
            case DRINK -> new PlayerMoodComponent.DrinkTask();
        };
    }

    static CompletableFuture<Suggestions> suggestTasks(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder
    ) {
        for (String suggestion : TASK_SUGGESTIONS) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }

    enum TaskMode {
        SLEEP(PlayerMoodComponent.Task.SLEEP),
        EAT(PlayerMoodComponent.Task.EAT),
        DRINK(PlayerMoodComponent.Task.DRINK),
        BREATHE_AIR(PlayerMoodComponent.Task.OUTSIDE),
        ALL(PlayerMoodComponent.Task.values());

        private final List<PlayerMoodComponent.Task> tasks;

        TaskMode(PlayerMoodComponent.Task task) {
            this.tasks = List.of(task);
        }

        TaskMode(PlayerMoodComponent.Task[] tasks) {
            this.tasks = List.of(tasks);
        }

        List<PlayerMoodComponent.Task> tasks() {
            return tasks;
        }
    }

    private enum TaskAction {
        ADD("Added"),
        DELETE("Deleted"),
        COMPLETE("Completed");

        private final String feedbackVerb;

        TaskAction(String feedbackVerb) {
            this.feedbackVerb = feedbackVerb;
        }
    }
}
