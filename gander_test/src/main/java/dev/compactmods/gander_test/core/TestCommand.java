package dev.compactmods.gander_test.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.compactmods.gander_test.network.GanderDebugRenderPacket;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TestCommand implements Command<CommandSourceStack>
{
    private TestCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext ctx)
    {
        return Commands.literal("ganderdebug")
            .then(Commands.argument("state", BlockStateArgument.block(ctx))
                .executes(new TestCommand()));
    }

    @Override
    public int run(final CommandContext<CommandSourceStack> commandContext)
        throws CommandSyntaxException
    {
        var state = BlockStateArgument.getBlock(commandContext, "state");
        PacketDistributor.sendToAllPlayers(new GanderDebugRenderPacket(state.getState()));
        return SINGLE_SUCCESS;
    }
}
