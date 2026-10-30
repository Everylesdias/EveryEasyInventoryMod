package fr.every.everyeasyinventory.logic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.every.everyeasyinventory.config.EeiSetting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@code /eei list | reload | force <réglage> <valeur> | free <réglage>}, réservé aux opérateurs.
 */
public final class EeiCommands {

    private static final SuggestionProvider<CommandSourceStack> SETTINGS = (context, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(EeiSetting.values()).map(setting -> setting.key), builder);

    private static final SuggestionProvider<CommandSourceStack> VALUES = (context, builder) -> {
        EeiSetting setting = EeiSetting.byKey(StringArgumentType.getString(context, "setting"));
        if (setting == null) {
            return builder.buildFuture();
        }
        List<String> values = switch (setting.kind) {
            case BOOL -> List.of("true", "false");
            case COLOR -> List.of("#FFC21A", "#FF3B30", "#00E5FF", "#FFFFFF");
            case INT, DOUBLE -> List.of(String.valueOf(setting.min), String.valueOf(setting.max));
        };
        return SharedSuggestionProvider.suggest(values, builder);
    };

    private EeiCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eei")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list").executes(EeiCommands::list))
                .then(Commands.literal("reload").executes(EeiCommands::reload))
                .then(Commands.literal("force")
                        .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTINGS)
                                .then(Commands.argument("value", StringArgumentType.greedyString()).suggests(VALUES)
                                        .executes(EeiCommands::force))))
                .then(Commands.literal("free")
                        .then(Commands.argument("setting", StringArgumentType.word()).suggests(SETTINGS)
                                .executes(EeiCommands::free))));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        Map<EeiSetting, Object> forced = ServerSettings.all();
        if (forced.isEmpty()) {
            context.getSource().sendSuccess(() -> message("none", "No forced setting"), false);
        }
        forced.forEach((setting, value) -> context.getSource().sendSuccess(
                () -> Component.literal(setting.key + " = " + setting.format(value)), false));
        return forced.size();
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        ServerSettings.reload();
        ServerSettings.broadcast(context.getSource().getServer());
        context.getSource().sendSuccess(() -> message("reloaded", "Forced settings reloaded"), true);
        return 1;
    }

    private static int force(CommandContext<CommandSourceStack> context) {
        EeiSetting setting = setting(context);
        if (setting == null) {
            return 0;
        }
        Object value = setting.parse(StringArgumentType.getString(context, "value"));
        if (value == null) {
            context.getSource().sendFailure(message("invalid_value", "Invalid value for this setting"));
            return 0;
        }
        ServerSettings.force(context.getSource().getServer(), setting, value);
        context.getSource().sendSuccess(() -> Component.literal(setting.key + " = " + setting.format(value)), true);
        return 1;
    }

    private static int free(CommandContext<CommandSourceStack> context) {
        EeiSetting setting = setting(context);
        if (setting == null) {
            return 0;
        }
        ServerSettings.free(context.getSource().getServer(), setting);
        context.getSource().sendSuccess(() -> Component.literal(setting.key).append(" : ").append(message("freed", "free")), true);
        return 1;
    }

    private static EeiSetting setting(CommandContext<CommandSourceStack> context) {
        EeiSetting setting = EeiSetting.byKey(StringArgumentType.getString(context, "setting"));
        if (setting == null) {
            context.getSource().sendFailure(message("unknown_setting", "Unknown setting"));
        }
        return setting;
    }

    /** Traduit pour les joueurs qui ont le mod, en anglais pour la console et les autres. */
    private static Component message(String key, String english) {
        return Component.translatableWithFallback("command.everyeasyinventorymod." + key, english);
    }
}
