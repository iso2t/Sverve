package com.iso2t.sverve.client.temperature;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.client.SurvivalHudVisibility;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Locale;

@RequiredArgsConstructor
public final class TemperatureHud {

	public static final Identifier             LAYER = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature");
	@NonNull
	private final       ClientTemperatureState state;

	public boolean isVisible () {
		var snapshot = state.getSnapshot();
		return snapshot != null && SurvivalHudVisibility.canShow(snapshot.enabled());
	}

	public void extract (GuiGraphicsExtractor graphics) {
		if (!isVisible()) return;
		var x = TemperatureIconLayout.iconX(graphics.guiWidth());
		var y = TemperatureIconLayout.iconY(graphics.guiHeight());
		var size = TemperatureIconLayout.ICON_SIZE;
		var band = state.getSnapshot().band();
		if (Minecraft.getInstance().player != null) PlayerFaceExtractor.extractRenderState(graphics, Minecraft.getInstance().player.getSkin(), x, y, size);
		if (band != TemperatureBand.NORMAL) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, overlaySprite(band), x, y, size, size);
		}
	}

	private static Identifier overlaySprite (TemperatureBand band) {
		return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hud/temperature/" + band.name().toLowerCase(Locale.ROOT) + "_overlay");
	}
}
