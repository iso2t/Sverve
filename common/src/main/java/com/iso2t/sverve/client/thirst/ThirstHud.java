package com.iso2t.sverve.client.thirst;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.client.SurvivalHudVisibility;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

@RequiredArgsConstructor
public final class ThirstHud {

	public static final  Identifier LAYER      = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "thirst");
	public static final  int        ROW_HEIGHT = 10;
	private static final Identifier EMPTY      = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hud/water_empty");
	private static final Identifier HALF       = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hud/water_half");
	private static final Identifier FULL       = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hud/water_full");

	@NonNull
	private final ClientThirstState state;

	public boolean isVisible () {
		var snapshot = state.getSnapshot();
		return snapshot != null && SurvivalHudVisibility.canShow(snapshot.enabled());
	}

	public void extract (GuiGraphicsExtractor graphics, int y) {
		if (!isVisible()) return;
		var snapshot = state.getSnapshot();
		for (var i = 0; i < ThirstBarLayout.ICON_COUNT; i++) {
			var x = ThirstBarLayout.iconX(graphics.guiWidth(), i);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY, x, y, ThirstBarLayout.ICON_SIZE, ThirstBarLayout.ICON_SIZE);
			var remaining = snapshot.halfUnits() - i * 2;
			if (remaining > 0) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, remaining >= 2 ? FULL : HALF, x, y, ThirstBarLayout.ICON_SIZE, ThirstBarLayout.ICON_SIZE);
			}
		}
	}
}
