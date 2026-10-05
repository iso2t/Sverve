package com.iso2t.sverve.client.moisture;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.client.SurvivalHudVisibility;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

@RequiredArgsConstructor
public final class MoistureHud {

	public static final  Identifier          LAYER       = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "moisture");
	public static final  int                 ICON_SIZE   = 5;
	private static final int                 SPRITE_SIZE = 9;
	private static final Identifier          EMPTY       = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hud/moisture/empty");
	private static final Identifier          FULL        = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hud/moisture/full");
	@NonNull
	private final        ClientMoistureState state;

	public boolean isVisible () {
		var snapshot = state.getSnapshot();
		return snapshot != null && snapshot.fillUnits() > 0 && SurvivalHudVisibility.canShow(snapshot.enabled());
	}

	public void extract (GuiGraphicsExtractor graphics) {
		if (!isVisible()) return;
		var x = iconX(graphics.guiWidth());
		var y = iconY(graphics.guiHeight());

		var fill = state.getSnapshot().fillUnits() + 1;
		var top = SPRITE_SIZE - fill;
		var pose = graphics.pose();
		pose.pushMatrix();
		try {
			pose.translate(x, y);
			var scale = (float) ICON_SIZE / SPRITE_SIZE;
			pose.scale(scale, scale);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY, 0, 0, SPRITE_SIZE, SPRITE_SIZE);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FULL, SPRITE_SIZE, SPRITE_SIZE, 0, top, 0, top, SPRITE_SIZE, fill);
		} finally {
			pose.popMatrix();
		}
	}

	public static int iconX (int width) {
		return MoistureIconLayout.x(width);
	}

	public static int iconY (int height) {
		return MoistureIconLayout.y(height, ICON_SIZE);
	}
}
