package dev.compactmods.gander.runtime.mixin.injection.points;

import dev.compactmods.gander.runtime.mixinutil.ElementNodeInvokeDynamicInsn;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.points.BeforeInvoke;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector.Configure;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorConstraintException;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Handles;

import java.util.Collection;
import java.util.ListIterator;

@AtCode(value = "INDY", namespace = "GANDER")
public class InvokeDynamic extends BeforeInvoke
{
    public InvokeDynamic(InjectionPointData data)
    {
        super(data);
    }

    protected boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes, ITargetSelector selector, SearchType searchType) {
        if (selector == null) {
            return false;
        }

        ITargetSelector target = (searchType == SearchType.PERMISSIVE ? selector.configure(Configure.PERMISSIVE) : selector)
            .configure(Configure.SELECT_INSTRUCTION);

        int ordinal = 0, found = 0, matchCount = 0;

        for (final AbstractInsnNode insn : insns)
        {
            if (this.matchesInsn(insn))
            {
                var node = new ElementNodeInvokeDynamicInsn((InvokeDynamicInsnNode)insn);
                this.log("{}->{} is considering {}", this.context, this.className, node);

                if (target.match(node).isExactMatch())
                {
                    this.log("{}->{} > found a matching insn, checking preconditions...", this.context, this.className);
                    if (++matchCount > target.getMaxMatchCount())
                    {
                        break;
                    }

                    if (this.matchesOrdinal(ordinal))
                    {
                        this.log(
                            "{}->{} > > > found a matching insn at ordinal {}",
                            this.context,
                            this.className,
                            ordinal);

                        if (this.addInsn(insns, nodes, insn))
                        {
                            found++;
                        }
                    }

                    ordinal++;
                }
            }

            this.inspectInsn(desc, insns, insn);
        }

        if (searchType == SearchType.PERMISSIVE && found > 1) {
            this.logger.warn("A permissive match for {} using \"{}\" in {} matched {} instructions, this may cause unexpected behaviour. "
                + "To inhibit permissive search set mixin.env.allowPermissiveMatch=false", this.className, selector, this.mixin, found);
        }

        if (matchCount < target.getMinMatchCount()) {
            throw new SelectorConstraintException(target, String.format("%s did not match the required number of targets (required=%d, matched=%d)",
                target, selector.getMinMatchCount(), matchCount));
        }

        return found > 0;
    }

    @Override
    protected boolean matchesInsn(final AbstractInsnNode insn)
    {
        return insn instanceof InvokeDynamicInsnNode;
    }


}
