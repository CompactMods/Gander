package com.simibubi.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.CustomUseEffectsItem;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class CustomItemUseEffectsMixin extends Entity {
	private CustomItemUseEffectsMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Shadow
	public abstract ItemStack getUseItem();

	@Inject(method = "shouldTriggerItemUseEffects()Z", at = @At("HEAD"), cancellable = true)
	private void create$onShouldTriggerUseEffects(CallbackInfoReturnable<Boolean> cir) {
		ItemStack using = getUseItem();
		Item item = using.getItem();
		if (item instanceof CustomUseEffectsItem handler) {
			Boolean result = handler.shouldTriggerUseEffects(using, (LivingEntity) (Object) this);
			if (result != null) {
				cir.setReturnValue(result);
			}
		}
	}

	@Inject(method = "triggerItemUseEffects(Lnet/minecraft/world/item/ItemStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/UseAnim;", ordinal = 0), cancellable = true)
	private void create$onTriggerUseEffects(ItemStack stack, int count, CallbackInfo ci) {
		Item item = stack.getItem();
		if (item instanceof CustomUseEffectsItem handler) {
			if (handler.triggerUseEffects(stack, (LivingEntity) (Object) this, count, random)) {
				ci.cancel();
			}
		}
	}
}
