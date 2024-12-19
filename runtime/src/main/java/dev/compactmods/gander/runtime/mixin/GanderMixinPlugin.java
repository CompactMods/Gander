package dev.compactmods.gander.runtime.mixin;

import dev.compactmods.gander.runtime.mixin.injection.points.InvokeDynamic;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

import java.util.List;
import java.util.Set;

public class GanderMixinPlugin implements IMixinConfigPlugin
{
    @Override
    public void onLoad(final String mixinPackage)
    {
        InjectionPoint.register(InvokeDynamic.class, null);
    }

    @Override
    public String getRefMapperConfig()
    {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName)
    {
        return true;
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets)
    {

    }

    @Override
    public List<String> getMixins()
    {
        return List.of();
    }

    @Override
    public void preApply(
        final String targetClassName,
        final ClassNode targetClass,
        final String mixinClassName,
        final IMixinInfo mixinInfo)
    {

    }

    @Override
    public void postApply(
        final String targetClassName,
        final ClassNode targetClass,
        final String mixinClassName,
        final IMixinInfo mixinInfo)
    {

    }
}
