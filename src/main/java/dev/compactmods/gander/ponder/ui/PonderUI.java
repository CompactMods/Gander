package dev.compactmods.gander.ponder.ui;

import com.mojang.blaze3d.platform.InputConstants;

import dev.compactmods.gander.gui.TickableGuiEventListener;
import dev.compactmods.gander.ponder.SceneRaytracer;
import dev.compactmods.gander.ponder.widget.SceneWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.compactmods.gander.ponder.PonderRegistry;
import dev.compactmods.gander.ponder.PonderScene;
import dev.compactmods.gander.utility.Color;
import dev.compactmods.gander.utility.Pair;
import dev.compactmods.gander.utility.animation.LerpedFloat;
import dev.compactmods.gander.utility.animation.LerpedFloat.Chaser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class PonderUI extends Screen {

	public static int ponderTicks;
	public static float ponderPartialTicksPaused;

	private final PonderScene scene;
	ItemStack stack;

	private boolean identifyMode;
	private ItemStack hoveredTooltipItem;
	private BlockPos hoveredBlockPos;

	private final ClipboardManager clipboardHelper;

	private final LerpedFloat lazyIndex;
	private final int index = 0;

	private int extendedTickLength = 0;
	private int extendedTickTimer = 0;

	protected int windowWidth, windowHeight;
	protected int windowXOffset, windowYOffset;
	protected int guiLeft, guiTop;

	protected boolean autoRotate = false;
	private SceneWidget sceneRenderer;
	private Vector2f mainCameraRotation;
	private static final Vector2f DEFAULT_ROTATION = new Vector2f((float) Math.toRadians(-25), (float) Math.toRadians(-135));

	protected PonderUI(PonderScene scene) {
		super(Component.empty());

		ResourceLocation component = scene.getComponent();
		if (BuiltInRegistries.ITEM.containsKey(component))
			stack = new ItemStack(BuiltInRegistries.ITEM.get(component));
		else
			stack = new ItemStack(BuiltInRegistries.BLOCK.get(component));

		this.scene = scene;
		lazyIndex = LerpedFloat.linear()
				.startWithValue(index);

		clipboardHelper = new ClipboardManager();
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

		this.sceneRenderer = this.addRenderableOnly(new SceneWidget(scene, width, height));
		// this.sceneRenderer.shouldRenderCompass(true);
		this.mainCameraRotation = new Vector2f(DEFAULT_ROTATION);
	}

	@Override
	public void tick() {
		super.tick();

		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener tickable) {
				tickable.tick();
			}
		}

		lazyIndex.tickChaser();
		extendedTickLength = 0;

		if (extendedTickTimer == 0) {
			if (!identifyMode) {
				ponderTicks++;
				scene.tick();
			}

			if (!identifyMode) {
				float lazyIndexValue = lazyIndex.getValue();
				if (Math.abs(lazyIndexValue - index) > 1 / 512f)
					scene.tick();
			}
			extendedTickTimer = extendedTickLength;
		} else
			extendedTickTimer--;

		if (autoRotate) {
			this.mainCameraRotation.y += Math.toRadians(2.5);
		}

		updateIdentifiedItem(scene);
	}

	public PonderScene getActiveScene() {
		return scene;
	}

	public void updateIdentifiedItem(PonderScene activeScene) {
		hoveredTooltipItem = ItemStack.EMPTY;
		hoveredBlockPos = null;
		if (!identifyMode)
			return;

		Window w = minecraft.getWindow();
		double mouseX = minecraft.mouseHandler.xpos() * w.getGuiScaledWidth() / w.getScreenWidth();
		double mouseY = minecraft.mouseHandler.ypos() * w.getGuiScaledHeight() / w.getScreenHeight();

		Pair<ItemStack, BlockPos> pair = SceneRaytracer.rayTraceScene(activeScene, mouseX, mouseY);
		hoveredTooltipItem = pair.getFirst();
		hoveredBlockPos = pair.getSecond();
	}

	@Override
	public void renderTransparentBackground(GuiGraphics pGuiGraphics) {

	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		this.sceneRenderer.prepareCamera(this.mainCameraRotation);
		super.render(graphics, mouseX, mouseY, partialTicks);

		// Chapter title
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		graphics.renderItem(stack, 20, 20);
		graphics.pose().popPose();

		RenderSystem.enableBlend();

		RenderSystem.disableDepthTest();

		float lazyIndexValue = lazyIndex.getValue(partialTicks);
		float indexDiff = Math.abs(lazyIndexValue - index);

		boolean noWidgetsHovered = true;
		for (GuiEventListener child : children())
			noWidgetsHovered &= !child.isMouseOver(mouseX, mouseY);

		int tooltipColor = Color.WHITE.getRGB();

		RenderSystem.enableDepthTest();
	}

	@Override
	public boolean keyPressed(int code, int scanCode, int modifiers) {
		Options settings = Minecraft.getInstance().options;

		var transform = getActiveScene().getTransform();

		final var rads = 1 / 12f;

		if (code == InputConstants.KEY_A) {
			this.autoRotate = !autoRotate;
			return true;
		}

		if (code == InputConstants.KEY_R) {
			this.mainCameraRotation.set(DEFAULT_ROTATION);
			return true;
		}

		if (code == InputConstants.KEY_UP) {
			if(this.mainCameraRotation.x < -rads)
				this.mainCameraRotation.x += rads;

			return true;
		}

		if (code == InputConstants.KEY_DOWN) {
			if(this.mainCameraRotation.x > -(Math.PI / 2) + (rads * 2))
				this.mainCameraRotation.x -= rads;
			return true;
		}

		if (code == InputConstants.KEY_LEFT) {
			this.mainCameraRotation.y += rads;
			return true;
		}

		if (code == InputConstants.KEY_RIGHT) {
			this.mainCameraRotation.y -= rads;
			return true;
		}

		if (code == InputConstants.KEY_I) {
			this.identifyMode = !identifyMode;
			if (!identifyMode) {
				scene.deselect();
			}
			return true;
		}

		return super.keyPressed(code, scanCode, modifiers);
	}

	public Font getFontRenderer() {
		return font;
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

	@Override
	public void removed() {
		super.removed();
		hoveredTooltipItem = ItemStack.EMPTY;
	}

	public void drawRightAlignedString(GuiGraphics graphics, PoseStack ms, String string, int x, int y, int color) {
		graphics.drawString(font, string, (float) (x - font.width(string)), (float) y, color, false);
	}
}
