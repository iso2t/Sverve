package com.iso2t.sverve.player.exertion;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.minecraft.server.level.ServerPlayer;

@UtilityClass
public class CarriedLoad {

	private static final    Source NONE   = _ -> 0.0;
	private static volatile Source source = NONE;

	public static void install (@NonNull Source query) {
		source = query;
	}

	public static boolean isInstalled () {
		return source != NONE;
	}

	public static double ratio (ServerPlayer player) {
		var ratio = source.ratio(player);
		return Double.isFinite(ratio) && ratio >= 0.0 ? ratio : 0.0;
	}

	@FunctionalInterface
	public interface Source {
		double ratio (ServerPlayer player);
	}
}
