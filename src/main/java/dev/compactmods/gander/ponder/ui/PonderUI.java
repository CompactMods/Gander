package dev.compactmods.gander.ponder.ui;

import static dev.compactmods.gander.ponder.PonderLocalization.LANG_PREFIX;

import java.util.List;

import dev.compactmods.gander.gui.TickableGuiEventListener;
import dev.compactmods.gander.ponder.widget.CompassOverlay;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.network.chat.Component;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;

import dev.compactmods.gander.gui.AllIcons;
import dev.compactmods.gander.gui.Theme;
import dev.compactmods.gander.ponder.PonderRegistry;
import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.ponder.PonderScene.SceneTransform;
import dev.compactmods.gander.render.SuperRenderTypeBuffer;
import dev.compactmods.gander.utility.Color;
import dev.compactmods.gander.utility.Iterate;
import dev.compactmods.gander.utility.Pair;
import dev.compactmods.gander.utility.Pointing;
import dev.compactmods.gander.utility.animation.LerpedFloat;
import dev.compactmods.gander.utility.animation.LerpedFloat.Chaser;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

public class PonderUI extends Screen {

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

	private final List<PonderScene> scenes;
	private final LerpedFloat fadeIn;
	ItemStack stack;

	private boolean userViewMode;
	private boolean identifyMode;
	private ItemStack hoveredTooltipItem;
	private BlockPos hoveredBlockPos;

	private final ClipboardManager clipboardHelper;
	private BlockPos copiedBlockPos;

	private final LerpedFloat finishingFlash;
	private final LerpedFloat nextUp;
	private int finishingFlashWarmup = 0;
	private int nextUpWarmup = 0;

	private final LerpedFloat lazyIndex;
	private final int index = 0;

	private int skipCooling = 0;

	private int extendedTickLength = 0;
	private int extendedTickTimer = 0;

	protected int windowWidth, windowHeight;
	protected int windowXOffset, windowYOffset;
	protected int guiLeft, guiTop;

	protected CompassOverlay compassOverlay;
	protected boolean autoRotate = false;

	protected PonderUI(List<PonderScene> scenes) {
		super(Component.empty());

		ResourceLocation component = scenes.get(0)
				.getComponent();
		if (BuiltInRegistries.ITEM.containsKey(component))
			stack = new ItemStack(BuiltInRegistries.ITEM.get(component));
		else
			stack = new ItemStack(BuiltInRegistries.BLOCK.get(component));

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

		guiLeft = (width - windowWidth) / 2;
		guiTop = (height - windowHeight) / 2;
		guiLeft += windowXOffset;
		guiTop += windowYOffset;

		Options bindings = minecraft.options;
		int bX = (width / 2) - 10;
		int bY = height - 20 - 31;

		this.compassOverlay = new CompassOverlay(getActiveScene());
	}

	@Override
	public void tick() {
		super.tick();

		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}

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

		if (autoRotate) {
			var transform = getActiveScene().getTransform();
			float targetLeftRight = transform.yRotation.getChaseTarget() - 1;
			transform.yRotation.chase(targetLeftRight, .1f, Chaser.EXP);
		}

		updateIdentifiedItem(activeScene);
	}

	public PonderScene getActiveScene() {
		return scenes.get(index);
	}

	public void seekToTime(int time) {
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

		Pair<ItemStack, BlockPos> pair = activeScene.rayTraceScene(mouseX, mouseY);
		hoveredTooltipItem = pair.getFirst();
		hoveredBlockPos = pair.getSecond();
	}

	@Override
	public void renderTransparentBackground(GuiGraphics pGuiGraphics) {

	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);

		RenderSystem.enableBlend();

		renderScene(graphics, mouseX, mouseY, index, skipCooling > 0 ? 0 : identifyMode ? ponderPartialTicksPaused : partialTicks);
		renderWidgets(graphics, mouseX, mouseY, identifyMode ? ponderPartialTicksPaused : partialTicks);
	}

	protected void renderScene(GuiGraphics graphics, int mouseX, int mouseY, int i, float partialTicks) {
		SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
		PonderScene scene = scenes.get(i);

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

		scene.getTransform().updateScreenParams(width, height, 0);
		scene.getTransform().apply(ms, partialTicks);
		scene.getTransform().updateSceneRVE(partialTicks);

		// RenderSystem.runAsFancy(() -> {
			scene.renderScene(buffer, ms, partialTicks);
			buffer.draw();
		// });

		this.compassOverlay.render(graphics, mouseX, mouseY, partialTicks);

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
		ms.popPose();
	}

	protected void renderChapterTitle(GuiGraphics graphics, float fade, float indexDiff, PonderScene activeScene, int tooltipColor) {
		PoseStack ms = graphics.pose();

		// Chapter title
		ms.pushPose();
		ms.translate(0, 0, 400);
		int x = 31 + 20 + 8;
		int y = 31;

		graphics.renderItem(stack, x - 39, y - 11);
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
	public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
		Options settings = Minecraft.getInstance().options;

		int KEY_LEFT = settings.keyLeft.getKey().getValue();
		int KEY_RIGHT = settings.keyRight.getKey().getValue();
		int KEY_DROP = settings.keyDrop.getKey().getValue();

		if (code == 263) {
			var transform = getActiveScene().getTransform();
			float targetLeftRight = transform.yRotation.getChaseTarget() - 15;
			transform.yRotation.chase(targetLeftRight, .1f, Chaser.EXP);
			return true;
		}

		if (code == 262) {
			var transform = getActiveScene().getTransform();
			float targetLeftRight = transform.yRotation.getChaseTarget() + 15;
			transform.yRotation.chase(targetLeftRight, .1f, Chaser.EXP);
			return true;
		}

		if (code == 73) {
			this.identifyMode = !identifyMode;
			if (!identifyMode) {
				scenes.get(index).deselect();
			}
			return true;
		}

		return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
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

	public static float getPartialTicks() {
		float renderPartialTicks = Minecraft.getInstance()
				.getFrameTime();

		if (Minecraft.getInstance().screen instanceof PonderUI ui) {
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
