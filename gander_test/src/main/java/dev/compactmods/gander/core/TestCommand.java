package dev.compactmods.gander.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.compactmods.gander.network.GanderDebugRenderPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TestCommand implements Command<CommandSourceStack>
{
    private TestCommand() { }

    public static LiteralArgumentBuilder<CommandSourceStack> COMMAND
        = Commands.literal("ganderdebug")
            .executes(new TestCommand());


    @Override
    public int run(final CommandContext<CommandSourceStack> commandContext)
        throws CommandSyntaxException
    {
        PacketDistributor.sendToAllPlayers(GanderDebugRenderPacket.INSTANCE);
        return SINGLE_SUCCESS;
    }
}
