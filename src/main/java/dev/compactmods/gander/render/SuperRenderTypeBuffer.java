//package dev.compactmods.gander.render;
//
//import java.util.SortedMap;
//
//import com.mojang.blaze3d.vertex.BufferBuilder;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//
//import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
//import net.minecraft.Util;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.client.renderer.SectionBufferBuilderPack;
//import net.minecraft.client.renderer.Sheets;
//import net.minecraft.client.resources.model.ModelBakery;
//
//public class SuperRenderTypeBuffer implements MultiBufferSource {
//
//	private static final SuperRenderTypeBuffer INSTANCE = new SuperRenderTypeBuffer();
//
//	public static SuperRenderTypeBuffer getInstance() {
//		return INSTANCE;
//	}
//
//	private final SuperRenderTypeBufferPhase defaultBuffer;
//
//	public SuperRenderTypeBuffer() {
//		defaultBuffer = new SuperRenderTypeBufferPhase();
//	}
//
//	@Override
//	public VertexConsumer getBuffer(RenderType type) {
//		return defaultBuffer.bufferSource.getBuffer(type);
//	}
//
//	public void draw() {
//		defaultBuffer.bufferSource.endBatch();
//	}
//
//	public void draw(RenderType type) {
//		defaultBuffer.bufferSource.endBatch(type);
//	}
//
//	private static class SuperRenderTypeBufferPhase {
//
//		// Visible clones from RenderBuffers
//		private final SectionBufferBuilderPack fixedBufferPack = new SectionBufferBuilderPack();
//		private final SortedMap<RenderType, BufferBuilder> fixedBuffers = Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
//				map.put(Sheets.solidBlockSheet(), fixedBufferPack.builder(RenderType.solid()));
//				map.put(Sheets.cutoutBlockSheet(), fixedBufferPack.builder(RenderType.cutout()));
//				map.put(Sheets.bannerSheet(), fixedBufferPack.builder(RenderType.cutoutMipped()));
//				map.put(Sheets.translucentCullBlockSheet(), fixedBufferPack.builder(RenderType.translucent()));
//				put(map, Sheets.shieldSheet());
//				put(map, Sheets.bedSheet());
//				put(map, Sheets.shulkerBoxSheet());
//				put(map, Sheets.signSheet());
//				put(map, Sheets.chestSheet());
//				put(map, RenderType.translucentMovingBlock());
//				put(map, RenderType.armorGlint());
//				put(map, RenderType.armorEntityGlint());
//				put(map, RenderType.glint());
//				put(map, RenderType.glintDirect());
//				put(map, RenderType.glintTranslucent());
//				put(map, RenderType.entityGlint());
//				put(map, RenderType.entityGlintDirect());
//				put(map, RenderType.waterMask());
//				put(map, RenderTypes.getOutlineSolid());
//				ModelBakery.DESTROY_TYPES.forEach((p_173062_) -> {
//					put(map, p_173062_);
//				});
//			});
//		private final MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new BufferBuilder(256));
//
//		private static void put(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> map, RenderType type) {
//			map.put(type, new BufferBuilder(type.bufferSize()));
//		}
//	}
//}
