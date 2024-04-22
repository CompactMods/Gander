//package dev.compactmods.gander.ponder;
//
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.level.levelgen.structure.BoundingBox;
//import net.minecraft.world.phys.Vec3;
//
//public abstract class Selection implements Predicate<BlockPos> {
//
//	public static Selection of(BoundingBox bb) {
//		return new Simple(bb);
//	}
//
//	public abstract Selection add(Selection other);
//
//	public abstract Selection substract(Selection other);
//
//	public abstract Selection copy();
//
//	public abstract Vec3 getCenter();
//
//	public abstract void forEach(Consumer<BlockPos> callback);
//
//}
