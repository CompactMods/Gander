package com.simibubi.create.ponder.ui;

import static com.simibubi.create.ponder.PonderLocalization.LANG_PREFIX;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.simibubi.create.Create;
import com.simibubi.create.gui.AllGuiTextures;
import com.simibubi.create.gui.AllIcons;
import com.simibubi.create.gui.Theme;
import com.simibubi.create.gui.UIRenderHelper;
import com.simibubi.create.gui.element.BoxElement;
import com.simibubi.create.gui.element.GuiGameElement;
import com.simibubi.create.ItemHelper;
import com.simibubi.create.ponder.PonderRegistry;
import com.simibubi.create.ponder.PonderScene;
import com.simibubi.create.ponder.PonderScene.SceneTransform;
import com.simibubi.create.ponder.PonderStoryBoardEntry;
import com.simibubi.create.ponder.PonderWorld;
import com.simibubi.create.render.SuperRenderTypeBuffer;
import com.simibubi.create.utility.Color;
import com.simibubi.create.utility.Components;
import com.simibubi.create.utility.FontHelper;
import com.simibubi.create.utility.Iterate;
import com.simibubi.create.utility.Lang;
import com.simibubi.create.utility.Pair;
import com.simibubi.create.utility.Pointing;
import com.simibubi.create.utility.RegisteredObjects;
import com.simibubi.create.utility.animation.LerpedFloat;
import com.simibubi.create.utility.animation.LerpedFloat.Chaser;
import com.simibubi.create.infrastructure.ponder.DebugScenes;
import com.simibubi.create.infrastructure.ponder.PonderIndex;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class PonderUI extends NavigatableSimiScreen {

	public static int ponderTicks;
	public static float ponderPartialTicksPaused;

	public static final String PONDERING = LANG_PREFIX + "pondering";
	public static final String IDENTIFY_MODE = LANG_PREFIX + "identify_mode";
	public static final String IN_CHAPTER = LANG_PREFIX + "in_chapter";
	public static final String IDENTIFY = LANG_PREFIX + "identify";
	public static final String PREVIOUS = LANG_PREFIX + "previous";
	public static final String CLOSE = LANG_PREFIX + "close";
	public static final String NEXT = LANG_PREFIX + "next";
	public static final String NEXT_UP = LANG_PREFIX + "next_up";
	public static final String REPLAY = LANG_PREFIX + "replay";
	public static final String SLOW_TEXT = LANG_PREFIX + "slow_text";

	private List<PonderScene> scenes;
	private LerpedFloat fadeIn;
	ItemStack stack;

	private boolean userViewMode;
	private boolean identifyMode;
	private ItemStack hoveredTooltipItem;
	private BlockPos hoveredBlockPos;

	private ClipboardManager clipboardHelper;
	private BlockPos copiedBlockPos;

	private LerpedFloat finishingFlash;
	private LerpedFloat nextUp;
	private int finishingFlashWarmup = 0;
	private int nextUpWarmup = 0;

	private LerpedFloat lazyIndex;
	private int index = 0;

	private PonderButton scan;
	private int skipCooling = 0;

	private int extendedTickLength = 0;
	private int extendedTickTimer = 0;

	protected PonderUI(List<PonderScene> scenes) {
		ResourceLocation component = scenes.get(0)
				.getComponent();
		if (ForgeRegistries.ITEMS.containsKey(component))
			stack = new ItemStack(ForgeRegistries.ITEMS.getValue(component));
		else
			stack = new ItemStack(ForgeRegistries.BLOCKS.getValue(component));

		this.scenes = scenes;
		lazyIndex = LerpedFloat.linear()
				.startWithValue(index);
		fadeIn = LerpedFloat.linear()
				.startWithValue(0)
				.chase(1, .1f, Chaser.EXP);
		clipboardHelper = new ClipboardManager();
		finishingFlash = LerpedFloat.linear()
				.startWithValue(0)
				.chase(0, .1f, Chaser.EXP);
		nextUp = LerpedFloat.linear()
				.startWithValue(0)
				.chase(0, .4f, Chaser.EXP);
	}

	public static PonderUI of(ResourceLocation id) {
		return new PonderUI(PonderRegistry.compile(id));
	}

	@Override
	protected void init() {
		super.init();

		Options bindings = minecraft.options;
		int bX = (width / 2) - 10;
		int bY = height - 20 - 31;

		addRenderableWidget(scan = new PonderButton(bX, bY)
				.withShortcut(bindings.keyDrop)
				.showing(AllIcons.I_MTD_SCAN)
				.atZLevel(600)
				.withCallback(() -> {
					identifyMode = !identifyMode;
					if (!identifyMode)
						scenes.get(index)
								.deselect();
					else
						ponderPartialTicksPaused = minecraft.getFrameTime();
				}));
	}

	@Override
	protected void initBackTrackIcon(PonderButton backTrack) {
		backTrack.showing(stack);
	}

	@Override
	public void tick() {
		super.tick();

		if (skipCooling > 0)
			skipCooling--;

		lazyIndex.tickChaser();
		fadeIn.tickChaser();
		finishingFlash.tickChaser();
		nextUp.tickChaser();
		PonderScene activeScene = scenes.get(index);

		extendedTickLength = 0;

		if (extendedTickTimer == 0) {
			if (!identifyMode) {
				ponderTicks++;
				if (skipCooling == 0)
					activeScene.tick();
			}

			if (!identifyMode) {
				float lazyIndexValue = lazyIndex.getValue();
				if (Math.abs(lazyIndexValue - index) > 1 / 512f)
					scenes.get(lazyIndexValue < index ? index - 1 : index + 1)
							.tick();
			}
			extendedTickTimer = extendedTickLength;
		} else
			extendedTickTimer--;

		if (activeScene.getCurrentTime() == activeScene.getTotalTime() - 1) {
			finishingFlashWarmup = 30;
			nextUpWarmup = 50;
		}

		if (finishingFlashWarmup > 0) {
			finishingFlashWarmup--;
			if (finishingFlashWarmup == 0) {
				finishingFlash.setValue(1);
				finishingFlash.setValue(1);
			}
		}

		if (nextUpWarmup > 0) {
			nextUpWarmup--;
			if (nextUpWarmup == 0)
				nextUp.updateChaseTarget(1);
		}

		updateIdentifiedItem(activeScene);
	}

	public PonderScene getActiveScene() {
		return scenes.get(index);
	}

	public void seekToTime(int time) {
		if (getActiveScene().getCurrentTime() > time)
			replay();

		getActiveScene().seekToTime(time);
		if (time != 0)
			coolDownAfterSkip();
	}

	public void updateIdentifiedItem(PonderScene activeScene) {
		hoveredTooltipItem = ItemStack.EMPTY;
		hoveredBlockPos = null;
		if (!identifyMode)
			return;

		Window w = minecraft.getWindow();
		double mouseX = minecraft.mouseHandler.xpos() * w.getGuiScaledWidth() / w.getScreenWidth();
		double mouseY = minecraft.mouseHandler.ypos() * w.getGuiScaledHeight() / w.getScreenHeight();
		SceneTransform t = activeScene.getTransform();
		Vec3 vec1 = t.screenToScene(mouseX, mouseY, 1000, 0);
		Vec3 vec2 = t.screenToScene(mouseX, mouseY, -100, 0);
		Pair<ItemStack, BlockPos> pair = activeScene.rayTraceScene(vec1, vec2);
		hoveredTooltipItem = pair.getFirst();
		hoveredBlockPos = pair.getSecond();
	}

	protected void replay() {
		identifyMode = false;
		PonderScene scene = scenes.get(index);

		if (hasShiftDown()) {
			List<PonderStoryBoardEntry> list = PonderRegistry.ALL.get(scene.getComponent());
			PonderStoryBoardEntry sb = list.get(index);
			StructureTemplate activeTemplate = PonderRegistry.loadSchematic(sb.getSchematicLocation());
			PonderWorld world = new PonderWorld(BlockPos.ZERO, Minecraft.getInstance().level);
			activeTemplate.placeInWorld(world, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(), RandomSource.create(),
					Block.UPDATE_CLIENTS);
			world.createBackup();
			scene = PonderRegistry.compileScene(sb, world);
			scene.begin();
			scenes.set(index, scene);
		}

		scene.begin();
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		partialTicks = getPartialTicks();
		RenderSystem.enableBlend();

		renderScene(graphics, mouseX, mouseY, index, skipCooling > 0 ? 0 : identifyMode ? ponderPartialTicksPaused : partialTicks);
		renderWidgets(graphics, mouseX, mouseY, identifyMode ? ponderPartialTicksPaused : partialTicks);
	}

	@Override
	public void renderBackground(GuiGraphics graphics) {
		super.renderBackground(graphics);
	}

	protected void renderScene(GuiGraphics graphics, int mouseX, int mouseY, int i, float partialTicks) {
		SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
		PonderScene scene = scenes.get(i);
		double value = lazyIndex.getValue(minecraft.getFrameTime());
		double diff = i - value;
		double slide = Mth.lerp(diff * diff, 200, 600) * diff;

		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.backupProjectionMatrix();

		// has to be outside of MS transforms, important for vertex sorting
		Matrix4f matrix4f = new Matrix4f(RenderSystem.getProjectionMatrix());
		matrix4f.translate(0, 0, 800);
		RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);

		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(0, 0, -800);

		scene.getTransform()
				.updateScreenParams(width, height, slide);
		scene.getTransform()
				.apply(ms, partialTicks);

//		ms.translate(-story.getBasePlateOffsetX() * .5, 0, -story.getBasePlateOffsetZ() * .5);

		scene.getTransform()
				.updateSceneRVE(partialTicks);

		scene.renderScene(buffer, ms, partialTicks);
		buffer.draw();

		BoundingBox bounds = scene.getBounds();
		ms.pushPose();

		// kool shadow fx
		if (!scene.shouldHidePlatformShadow()) {
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			ms.pushPose();
			ms.translate(scene.getBasePlateOffsetX(), 0, scene.getBasePlateOffsetZ());
			UIRenderHelper.flipForGuiRender(ms);

			float flash = finishingFlash.getValue(partialTicks) * .9f;
			float alpha = flash;
			flash *= flash;
			flash = ((flash * 2) - 1);
			flash *= flash;
			flash = 1 - flash;

			for (int f = 0; f < 4; f++) {
				ms.translate(scene.getBasePlateSize(), 0, 0);
				ms.pushPose();
				ms.translate(0, 0, -1 / 1024f);
				if (flash > 0) {
					ms.pushPose();
					ms.scale(1, .5f + flash * .75f, 1);
					graphics.fillGradient(0, -1, -scene.getBasePlateSize(), 0, 0, 0x00_c6ffc9,
							new Color(0xaa_c6ffc9).scaleAlpha(alpha)
									.getRGB());
					ms.popPose();
				}
				ms.translate(0, 0, 2 / 1024f);
				graphics.fillGradient(0, 0, -scene.getBasePlateSize(), 4, 0, 0x66_000000, 0x00_000000);
				ms.popPose();
				ms.mulPose(Axis.YP.rotationDegrees(-90));
			}
			ms.popPose();
			RenderSystem.disableCull();
			RenderSystem.disableDepthTest();
		}

		// coords for debug
		if (PonderIndex.editingModeActive() && !userViewMode) {

			ms.scale(-1, -1, 1);
			ms.scale(1 / 16f, 1 / 16f, 1 / 16f);
			ms.translate(1, -8, -1 / 64f);

			// X AXIS
			ms.pushPose();
			ms.translate(4, -3, 0);
			ms.translate(0, 0, -2 / 1024f);
			for (int x = 0; x <= bounds.getXSpan(); x++) {
				ms.translate(-16, 0, 0);
				graphics.drawString(font, x == bounds.getXSpan() ? "x" : "" + x, 0, 0, 0xFFFFFFFF, false);
			}
			ms.popPose();

			// Z AXIS
			ms.pushPose();
			ms.scale(-1, 1, 1);
			ms.translate(0, -3, -4);
			ms.mulPose(Axis.YP.rotationDegrees(-90));
			ms.translate(-8, -2, 2 / 64f);
			for (int z = 0; z <= bounds.getZSpan(); z++) {
				ms.translate(16, 0, 0);
				graphics.drawString(font, z == bounds.getZSpan() ? "z" : "" + z, 0, 0, 0xFFFFFFFF, false);
			}
			ms.popPose();

			// DIRECTIONS
			ms.pushPose();
			ms.translate(bounds.getXSpan() * -8, 0, bounds.getZSpan() * 8);
			ms.mulPose(Axis.YP.rotationDegrees(-90));
			for (Direction d : Iterate.horizontalDirections) {
				ms.mulPose(Axis.YP.rotationDegrees(90));
				ms.pushPose();
				ms.translate(0, 0, bounds.getZSpan() * 16);
				ms.mulPose(Axis.XP.rotationDegrees(-90));
				graphics.drawString(font, d.name()
						.substring(0, 1), 0, 0, 0x66FFFFFF, false);
				graphics.drawString(font, "|", 2, 10, 0x44FFFFFF, false);
				graphics.drawString(font, ".", 2, 14, 0x22FFFFFF, false);
				ms.popPose();
			}
			ms.popPose();
			buffer.draw();
		}

		ms.popPose();
		ms.popPose();
		RenderSystem.restoreProjectionMatrix();
	}

	protected void renderWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		RenderSystem.disableDepthTest();

		float fade = fadeIn.getValue(partialTicks);
		float lazyIndexValue = lazyIndex.getValue(partialTicks);
		float indexDiff = Math.abs(lazyIndexValue - index);
		PonderScene activeScene = scenes.get(index);
		PonderScene nextScene = scenes.size() > index + 1 ? scenes.get(index + 1) : null;

		boolean noWidgetsHovered = true;
		for (GuiEventListener child : children())
			noWidgetsHovered &= !child.isMouseOver(mouseX, mouseY);

		int tooltipColor = Color.WHITE.getRGB();
		renderChapterTitle(graphics, fade, indexDiff, activeScene, tooltipColor);

		PoseStack ms = graphics.pose();

		if (identifyMode) {
			if (noWidgetsHovered && mouseY < height - 80) {
				ms.pushPose();
				ms.translate(mouseX, mouseY, 100);
				if (hoveredTooltipItem.isEmpty()) {
					MutableComponent text = Lang
							.translateDirect(IDENTIFY_MODE,
									((MutableComponent) minecraft.options.keyDrop.getTranslatedKeyMessage())
											.withStyle(ChatFormatting.WHITE))
							.withStyle(ChatFormatting.GRAY);
					graphics.renderTooltip(font, font.split(text, width / 3), 0, 0);
				} else
					graphics.renderTooltip(font, hoveredTooltipItem, 0, 0);

				ms.popPose();
			}
			scan.flash();
		} else {
			scan.dim();
		}

		// Widgets
		renderables.forEach(w -> {
			if (w instanceof PonderButton button) {
				button.fade().startWithValue(fade);
			}
		});

		renderSceneOverlay(graphics, partialTicks, lazyIndexValue, indexDiff);
		renderHoverTooltips(graphics, tooltipColor);
		RenderSystem.enableDepthTest();
	}

	protected void renderSceneOverlay(GuiGraphics graphics, float partialTicks, float lazyIndexValue, float indexDiff) {
		PoseStack ms = graphics.pose();

		// Scene overlay
		float scenePT = skipCooling > 0 ? 0 : partialTicks;
		ms.pushPose();
		ms.translate(0, 0, 100);
		renderOverlay(graphics, index, scenePT);
		if (indexDiff > 1 / 512f)
			renderOverlay(graphics, lazyIndexValue < index ? index - 1 : index + 1, scenePT);
		ms.popPose();
	}

	protected void renderHoverTooltips(GuiGraphics graphics, int tooltipColor) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(0, 0, 500);
		int tooltipY = height - 16;
		if (scan.isHoveredOrFocused())
			graphics.drawCenteredString(font, Lang.translateDirect(IDENTIFY), scan.getX() + 10, tooltipY, tooltipColor);

		ms.popPose();
	}

	protected void renderChapterTitle(GuiGraphics graphics, float fade, float indexDiff, PonderScene activeScene, int tooltipColor) {
		PoseStack ms = graphics.pose();

		// Chapter title
		ms.pushPose();
		ms.translate(0, 0, 400);
		int x = 31 + 20 + 8;
		int y = 31;

		new BoxElement()
				.withBackground(Color.TRANSPARENT_BLACK.setAlpha(0.2f))
				.flatBorder(0)
				.at(21, 21, 100)
				.withBounds(14, 14)
				.render(graphics);

		GuiGameElement.of(stack)
				.at(x - 39f, y - 11f)
				.render(graphics);

		ms.popPose();
	}

	private void renderOverlay(GuiGraphics graphics, int i, float partialTicks) {
		if (identifyMode)
			return;
		PoseStack ms = graphics.pose();
		ms.pushPose();
		PonderScene story = scenes.get(i);
		story.renderOverlay(this, graphics, skipCooling > 0 ? 0 : identifyMode ? ponderPartialTicksPaused : partialTicks);
		ms.popPose();
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		if (identifyMode && hoveredBlockPos != null && PonderIndex.editingModeActive()) {
			long handle = minecraft.getWindow()
					.getWindow();
			if (copiedBlockPos != null && button == 1) {
				clipboardHelper.setClipboard(handle,
						"util.select.fromTo(" + copiedBlockPos.getX() + ", " + copiedBlockPos.getY() + ", "
								+ copiedBlockPos.getZ() + ", " + hoveredBlockPos.getX() + ", " + hoveredBlockPos.getY() + ", "
								+ hoveredBlockPos.getZ() + ")");
				copiedBlockPos = hoveredBlockPos;
				return true;
			}

			if (hasShiftDown())
				clipboardHelper.setClipboard(handle, "util.select.position(" + hoveredBlockPos.getX() + ", "
						+ hoveredBlockPos.getY() + ", " + hoveredBlockPos.getZ() + ")");
			else
				clipboardHelper.setClipboard(handle, "util.grid.at(" + hoveredBlockPos.getX() + ", "
						+ hoveredBlockPos.getY() + ", " + hoveredBlockPos.getZ() + ")");
			copiedBlockPos = hoveredBlockPos;
			return true;
		}

		return super.mouseClicked(x, y, button);
	}

	@Override
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		Options settings = Minecraft.getInstance().options;

		int KEY_LEFT = settings.keyLeft.getKey().getValue();
		int KEY_RIGHT = settings.keyRight.getKey().getValue();
		int KEY_DROP = settings.keyDrop.getKey().getValue();

		if (code == 263) {
			var currentScene = scenes.get(index);
			var cam = currentScene.camera();
			cam.set(cam.getXRot(), cam.getYRot() + 180);
			cam.tick();
			return true;
		}

		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
	}

	@Override
	protected String getBreadcrumbTitle() {
		return stack.getItem()
				.getDescription()
				.getString();
	}

	public Font getFontRenderer() {
		return font;
	}

	protected boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
		boolean hovered = !(mouseX < x || mouseX > x + w);
		hovered &= !(mouseY < y || mouseY > y + h);
		return hovered;
	}

	public static void renderSpeechBox(GuiGraphics graphics, int x, int y, int w, int h, boolean highlighted, Pointing pointing,
									   boolean returnWithLocalTransform) {
		PoseStack ms = graphics.pose();
		if (!returnWithLocalTransform)
			ms.pushPose();

		int boxX = x;
		int boxY = y;
		int divotX = x;
		int divotY = y;
		int divotRotation = 0;
		int divotSize = 8;
		int distance = 1;
		int divotRadius = divotSize / 2;
		var borderColors = Theme.pair(highlighted ? Theme.Key.PONDER_BUTTON_HOVER : Theme.Key.PONDER_IDLE);
		Color c;

		switch (pointing) {
			default:
			case DOWN:
				divotRotation = 0;
				boxX -= w / 2;
				boxY -= h + divotSize + 1 + distance;
				divotX -= divotRadius;
				divotY -= divotSize + distance;
				c = borderColors.getSecond();
				break;
			case LEFT:
				divotRotation = 90;
				boxX += divotSize + 1 + distance;
				boxY -= h / 2;
				divotX += distance;
				divotY -= divotRadius;
				c = Color.mixColors(borderColors.getFirst(), borderColors.getSecond(), 0.5f);
				break;
			case RIGHT:
				divotRotation = 270;
				boxX -= w + divotSize + 1 + distance;
				boxY -= h / 2;
				divotX -= divotSize + distance;
				divotY -= divotRadius;
				c = Color.mixColors(borderColors.getFirst(), borderColors.getSecond(), 0.5f);
				break;
			case UP:
				divotRotation = 180;
				boxX -= w / 2;
				boxY += divotSize + 1 + distance;
				divotX -= divotRadius;
				divotY += distance;
				c = borderColors.getFirst();
				break;
		}

		new BoxElement().withBackground(Theme.color(Theme.Key.PONDER_BACKGROUND_FLAT))
				.gradientBorder(borderColors)
				.at(boxX, boxY, 100)
				.withBounds(w, h)
				.render(graphics);

		ms.pushPose();
		ms.translate(divotX + divotRadius, divotY + divotRadius, 10);
		ms.mulPose(Axis.ZP.rotationDegrees(divotRotation));
		ms.translate(-divotRadius, -divotRadius, 0);
		AllGuiTextures.SPEECH_TOOLTIP_BACKGROUND.render(graphics, 0, 0);
		AllGuiTextures.SPEECH_TOOLTIP_COLOR.render(graphics, 0, 0, c);
		ms.popPose();

		if (returnWithLocalTransform) {
			ms.translate(boxX, boxY, 0);
			return;
		}

		ms.popPose();

	}

	public ItemStack getHoveredTooltipItem() {
		return hoveredTooltipItem;
	}

	public ItemStack getSubject() {
		return stack;
	}

	@Override
	public boolean isEquivalentTo(NavigatableSimiScreen other) {
		if (other instanceof PonderUI)
			return ItemHelper.sameItem(stack, ((PonderUI) other).stack);
		return super.isEquivalentTo(other);
	}

	public static float getPartialTicks() {
		float renderPartialTicks = Minecraft.getInstance()
				.getFrameTime();

		if (Minecraft.getInstance().screen instanceof PonderUI) {
			PonderUI ui = (PonderUI) Minecraft.getInstance().screen;
			if (ui.identifyMode)
				return ponderPartialTicksPaused;

			return (renderPartialTicks + (ui.extendedTickLength - ui.extendedTickTimer)) / (ui.extendedTickLength + 1);
		}

		return renderPartialTicks;
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	public void coolDownAfterSkip() {
		skipCooling = 15;
	}

	@Override
	public void removed() {
		super.removed();
		hoveredTooltipItem = ItemStack.EMPTY;
	}

	public void drawRightAlignedString(GuiGraphics graphics, PoseStack ms, String string, int x, int y, int color) {
		graphics.drawString(font, string, (float) (x - font.width(string)), (float) y, color, false);
	}
}
