package dev.compactmods.gander.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class GanderCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> make(CommandBuildContext buildContext) {
        final var root = Commands.literal("gander")
            .requires(cs -> cs.hasPermission(Commands.LEVEL_ALL));

        GanderInWorldCommand.addInWorldSubtree(buildContext, root);
        GanderScreenCommand.addScreenSubtree(buildContext, root);
        return root;
    }
}
