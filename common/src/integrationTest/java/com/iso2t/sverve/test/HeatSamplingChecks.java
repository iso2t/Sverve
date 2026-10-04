package com.iso2t.sverve.test;

import com.iso2t.sverve.player.environment.NearbyHeatSampler;
import com.iso2t.sverve.survival.environment.HeatSourceConfig;
import com.mojang.authlib.GameProfile;
import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Measures a synchronous 32-player sampling burst; timings are diagnostic, never a flaky test gate.
 */
@UtilityClass
public class HeatSamplingChecks {
	private static volatile double consumed;

	public static void run (ServerPlayer player, Path report) throws IOException {
		var level = player.level();
		player.setPos(8.5, 160, 8.5);
		var center = player.blockPosition();
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) level.getChunk(x, z);
		for (var pos : BlockPos.betweenClosed(center.offset(-9, -9, -9), center.offset(9, 9, 9))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		var results = new StringBuilder("scenario,radius,samples_per_burst,median_ms,p95_ms\n");
		var config = new HeatSourceConfig();
		var sampler = new NearbyHeatSampler(config);
		var players = new ServerPlayer[32];
		for (int i = 0; i < players.length; i++) {
			players[i] = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "heat-bench"), ClientInformation.createDefault());
			players[i].setPos(1.5 + (i % 8) * 2, 160, 4.5 + (i / 8) * 2);
		}
		for (int radius : new int[] { 6, 8 }) {
			config.getLavaRadius().set(radius);
			measure(sampler, players, "empty", radius, results);
		}
		for (var pos : BlockPos.betweenClosed(center.offset(-8, -8, -8), center.offset(8, 8, 8))) {
			if (pos.getX() % 4 == 0 && pos.getY() % 4 == 0 && pos.getZ() % 4 == 0 && !pos.equals(center)) level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
		}
		for (int radius : new int[] { 6, 8 }) {
			config.getLavaRadius().set(radius);
			LifecycleChecks.require(sampler.sample(player) > 0, "Loaded lava must contribute warmth");
			measure(sampler, players, "lava_grid", radius, results);
		}
		config.getEnabled().set(false);
		LifecycleChecks.require(sampler.sample(player) == 0, "Disabled heat must return zero");
		config.getEnabled().set(true);

		Files.writeString(report, results);
		System.out.println("Sverve heat sampling (32 calls per burst):\n" + results);
		// World generation can also load neighboring chunks; find the actual loaded boundary.
		level.getChunk(64, 64);
		BlockPos boundary = null;
		for (int x = 48; x <= 80 && boundary == null; x++)
			for (int z = 48; z <= 80; z++) {
				if (level.hasChunk(x, z) && !level.hasChunk(x - 1, z)) {
					boundary = new BlockPos(x << 4, 160, (z << 4) + 8);
					break;
				}
			}
		LifecycleChecks.require(boundary != null, "The fixture must have an unloaded chunk boundary");
		player.setPos(boundary.getX() + 0.5, 160, boundary.getZ() + 0.5);
		level.setBlock(boundary.east(), Blocks.LAVA.defaultBlockState(), 2);
		int loaded = level.getChunkSource().getLoadedChunksCount();
		LifecycleChecks.require(sampler.sample(player) > 0, "A nearby loaded source must still warm at the chunk boundary");
		LifecycleChecks.require(level.getChunkSource().getLoadedChunksCount() == loaded, "Heat sampling must not load chunks");
	}

	private static void measure (NearbyHeatSampler sampler, ServerPlayer[] players, String scenario, int radius, StringBuilder result) {
		for (int i = 0; i < 256; i++) consumed = sampler.sample(players[i % players.length]);
		long[] bursts = new long[40];
		for (int batch = 0; batch < bursts.length; batch++) {
			long start = System.nanoTime();
			double sum = 0;
			for (var player : players) sum += sampler.sample(player);
			bursts[batch] = System.nanoTime() - start;
			consumed = sum;
		}
		Arrays.sort(bursts);
		result.append(String.format(Locale.ROOT, "%s,%d,32,%.3f,%.3f%n", scenario, radius, bursts[20] / 1e6, bursts[37] / 1e6));
	}
}
