package com.iso2t.sverve.compat.heavyinventories.client;

import com.iso2t.heavyinventories.api.client.*;
import com.iso2t.sverve.client.exertion.LoadBarLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class LoadBarHud implements HudIntegration {

	@Override
	public HudLayout layout (HudContext context, HudElement element, HudLayout original) {
		var bounds = new HudBounds(LoadBarLayout.x(context.screenWidth()), LoadBarLayout.y(context.screenHeight()), LoadBarLayout.WIDTH, LoadBarLayout.HEIGHT);
		return new HudLayout(bounds, original.visible(), 0);
	}

	@Override
	public void render (GuiGraphicsExtractor graphics, HudContext context, HudLayout layout, HudDrawing drawing) {
		var bounds = layout.bounds();
		var x = bounds.x();
		var y = bounds.y();

		graphics.nextStratum();
		graphics.fill(x, y, x + bounds.width(), y + bounds.height(), 0xFF171717);

		graphics.fill(x + 1, y, x + bounds.width() - 1, y + 1, LoadBarLayout.EMPTY_COLOR);
		var ratio = context.player().orElseThrow().loadRatio();
		if (ratio.isEmpty()) {
			for (var pixel = 0; pixel < LoadBarLayout.INNER_WIDTH; pixel += 2) {
				graphics.fill(x + 1 + pixel, y, x + 2 + pixel, y + 1, LoadBarLayout.WARNING_COLOR);
			}
			return;
		}
		var load = ratio.getAsDouble();
		var fill = LoadBarLayout.fillWidth(load);
		if (fill > 0) graphics.fill(x + 1, y, x + 1 + fill, y + 1, LoadBarLayout.color(load));
	}
}
