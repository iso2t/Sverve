package com.iso2t.sverve.client.temperature;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.client.SurvivalHudVisibility;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * Each 16x16 status head is rendered at the smaller HUD size; animated sprites use the same path.
 */
@RequiredArgsConstructor
public final class TemperatureHud {
	public static final Identifier             LAYER = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature");
	@NonNull
	private final       ClientTemperatureState state;

	public boolean isVisible () {
		var snapshot = state.getSnapshot();
		return snapshot != null && SurvivalHudVisibility.canShow(snapshot.isEnabled());
	}

	public void extract (GuiGraphicsExtractor graphics) {
		if (!isVisible()) return;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(state.getSnapshot().getBand()), TemperatureIconLayout.iconX(graphics.guiWidth()), TemperatureIconLayout.iconY(graphics.guiHeight()), TemperatureIconLayout.ICON_SIZE, TemperatureIconLayout.ICON_SIZE);
	}

	public static Identifier sprite (TemperatureBand band) {
		return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hud/temperature/" + band.name().toLowerCase(Locale.ROOT));
	}
}
