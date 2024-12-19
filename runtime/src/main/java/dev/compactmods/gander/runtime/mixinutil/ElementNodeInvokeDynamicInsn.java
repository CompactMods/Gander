package dev.compactmods.gander.runtime.mixinutil;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;
import org.spongepowered.asm.util.Handles;

/**
 * (Patched!) ElementNode for InvokeDynamicInsnNode
 */
public class ElementNodeInvokeDynamicInsn extends ElementNode<InvokeDynamicInsnNode>
{

    private InvokeDynamicInsnNode insn;

    private Type samMethodType;

    private Handle implMethod;

    private Type instantiatedMethodType;

    public ElementNodeInvokeDynamicInsn(InvokeDynamicInsnNode invokeDynamic) {
        this.insn = invokeDynamic;

        if (invokeDynamic.bsmArgs != null && invokeDynamic.bsmArgs.length > 1) {
            Object samMethodType = invokeDynamic.bsmArgs[0];
            Object implMethod = invokeDynamic.bsmArgs[1];
            Object instantiatedMethodType = invokeDynamic.bsmArgs[2];
            if (samMethodType instanceof Type && implMethod instanceof Handle && instantiatedMethodType instanceof Type) {
                this.samMethodType = (Type)samMethodType;
                this.implMethod = (Handle)implMethod;
                this.instantiatedMethodType = (Type)instantiatedMethodType;
            }
        }
    }

    @Override
    public NodeType getType() {
        return NodeType.INVOKEDYNAMIC_INSN;
    }

    @Override
    public boolean isField() {
        return this.implMethod != null && Handles.isField(this.implMethod);
    }

    @Override
    public AbstractInsnNode getInsn() {
        return this.insn;
    }

    @Override
    public String getOwner() {
        return this.implMethod != null ? this.implMethod.getOwner() : this.insn.name;
    }

    @Override
    public String getName() {
        return this.implMethod != null ? this.implMethod.getName() : this.insn.name;
    }

    @Override
    public String getSyntheticName() {
        return this.implMethod != null ? this.implMethod.getName() : this.insn.name;
    }

    @Override
    public String getDesc() {
        return this.implMethod != null ? this.implMethod.getDesc() : this.insn.desc;
    }

    @Override
    public String getDelegateDesc() {
        return this.samMethodType != null ? this.samMethodType.getDescriptor() : this.getDesc();
    }

    @Override
    public String getImplDesc() {
        return this.instantiatedMethodType != null ? this.instantiatedMethodType.getDescriptor() : this.getDesc();
    }

    @Override
    public String getSignature() {
        return null;
    }

    @Override
    public InvokeDynamicInsnNode get() {
        return this.insn;
    }

    @Override
    public boolean equals(Object obj) {
        return this.insn.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.insn.hashCode();
    }

}
