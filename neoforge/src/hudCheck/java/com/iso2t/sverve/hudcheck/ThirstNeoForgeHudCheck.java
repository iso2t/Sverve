package com.iso2t.sverve.hudcheck;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.network.temperature.TemperatureSnapshot;
import com.iso2t.sverve.network.temperature.TemperatureSyncPayload;
import com.iso2t.sverve.network.thirst.ThirstSnapshot;
import com.iso2t.sverve.network.thirst.ThirstSyncPayload;
import com.iso2t.sverve.player.temperature.TemperatureDamage;
import com.iso2t.sverve.player.temperature.TemperatureDamageTimer;
import com.iso2t.sverve.player.temperature.TemperatureProtection;
import com.iso2t.sverve.player.thirst.DehydrationDamage;
import com.iso2t.sverve.survival.moisture.MoistureState;
import com.iso2t.sverve.survival.temperature.TemperatureBand;
import com.iso2t.sverve.survival.temperature.TemperatureState;
import com.iso2t.sverve.survival.thirst.ThirstState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Separate test mod and run directory; never loaded by ordinary clients or packaged for release.
 */
@Mod(value = "sverve_hud_check", dist = Dist.CLIENT)
public final class ThirstNeoForgeHudCheck {
	private final List<String>            checks    = new ArrayList<>();
	private       CompletableFuture<Void> pending   = CompletableFuture.completedFuture(null);
	private       int                     stage;
	private       long                    changedAt = System.nanoTime();
	private       boolean                 finished;
	private       ServerPlayer            dehydrationVictim;
	private       double                  hotHydration;
	private       int                     hotStartedAt;
	private       double                  exposedWetness;
	private       BlockPos                rainRoof;

	public ThirstNeoForgeHudCheck () {
		NeoForge.EVENT_BUS.addListener(this::afterFrame);
	}

	private void afterFrame (RenderFrameEvent.Post event) {
		if (finished) return;
		var client = Minecraft.getInstance();
		try {
			long elapsedMillis = (System.nanoTime() - changedAt) / 1_000_000;
			if (elapsedMillis > 90_000) throw new AssertionError("Timed out in HUD check stage " + stage);
			if (!pending.isDone()) return;
			pending.join();
			if (stage == 27) {
				// Disconnect after a frame, never from a GPU screenshot callback during rendering.
				client.disconnect(new TitleScreen(), false);
				require(client.player == null, "Disconnect must release the client player");
				finish(client, "PASS\n" + String.join("\n", checks));
				return;
			}
			if (client.player == null || client.level == null || client.gameMode == null) return;
			if (stage == 26) {
				if (!(client.gui.screen() instanceof DeathScreen)) return;
				onServer(client, player -> require(player.getLastDamageSource().is(TemperatureDamage.OVERHEATING), "Native NeoForge overheating must be lethal with its custom damage source"));
				checks.add("temperature-overheating-death");
				next(27);
				return;
			}
			if (stage == 15) {
				if (!(client.gui.screen() instanceof DeathScreen)) return;
				onServer(client, player -> require(dehydrationVictim.getLastDamageSource().is(DehydrationDamage.TYPE), "The real NeoForge tick hook must kill with dehydration damage"));
				client.player.respawn();
				client.gui.setScreen(null);
				next(16);
				return;
			}
			if (client.gui.screen() != null || elapsedMillis < 350) return;
			if ((stage == 20 || stage == 22 || stage == 24) && elapsedMillis < 4500) return;
			if ((stage == 29 || stage == 31 || stage == 33 || stage == 36 || stage == 37) && elapsedMillis < 1500) return;
			int height = client.gui.hud.rightHeight;
			switch (stage) {
				case 0 -> {
					onServer(client, player -> {
						player.setGameMode(GameType.SURVIVAL);
						player.setPermanentlyInvulnerable(true);
						player.setAirSupply(300);
						player.teleportTo(player.level().getServer().overworld(), 0.5, -60, 0.5, Set.of(), 0, 0, true);
						player.setData(thirstType(), new ThirstState(0.425));
						player.setData(temperatureType(), new TemperatureState(0.625));
						player.setExperienceLevels(25);
						player.setItemInHand(InteractionHand.OFF_HAND, Items.SHIELD.getDefaultInstance());
					});
					next(1);
				}
				case 1 -> {
					if (client.level.dimension() != Level.OVERWORLD) return;
					if (client.player.experienceLevel != 25) return;
					require(height == 59, "The connected Survival HUD must reserve hunger plus thirst rows");
					capture(client, "01-partial");
					next(2);
				}
				case 2 -> {
					// A normal client cancels server-started use unless its use key stays held.
					client.options.keyUse.setDown(true);
					onServer(client, player -> {
						player.getInventory().clearContent();
						var water = Items.POTION.getDefaultInstance().copyWithCount(16);
						require(water.getMaxStackSize() == 16, "NeoForge water bottles must stack to sixteen");
						player.setItemInHand(InteractionHand.MAIN_HAND, water);
						player.startUsingItem(InteractionHand.MAIN_HAND);
					});
					next(3);
				}
				case 3 -> {
					if (!client.player.getMainHandItem().is(Items.POTION) || client.player.getMainHandItem().getCount() != 15) return;
					client.options.keyUse.setDown(false);
					// The locally consumed stack can appear before the server's remainder slot update.
					if (client.player.getInventory().countItem(Items.GLASS_BOTTLE) == 0) return;
					require(height == 59, "Drinking must retain the thirst row and vanilla glass bottle");
					require(client.player.getMainHandItem().getMaxStackSize() == 16, "NeoForge's client must use a stack limit of sixteen");
					require(client.player.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "Drinking a stack must return one empty bottle");
					capture(client, "02-after-drinking");
					next(4);
				}
				case 4 -> {
					onServer(client, player -> {
						require(player.getData(thirstType()).getHydration() > 0.70, "Completed water drinking must restore hydration through the NeoForge event");
						require(player.getMainHandItem().getCount() == 15 && player.getInventory().countItem(Items.GLASS_BOTTLE) == 1,
								"NeoForge's server must consume one water bottle and return one empty bottle");
						player.setAirSupply(150);
						player.setData(temperatureType(), new TemperatureState(1));
					});
					next(5);
				}
				case 5 -> {
					if (client.player.getAirSupply() >= 300) return;
					require(height == 69, "Air bubbles must add their own row below thirst");
					capture(client, "03-with-air");
					next(6);
				}
				case 6 -> {
					onServer(client, player -> {
						player.setGameMode(GameType.CREATIVE);
						player.setData(temperatureType(), new TemperatureState(-1));
					});
					next(7);
				}
				case 7 -> {
					if (client.gameMode.getPlayerMode() != GameType.CREATIVE) return;
					require(height == 39, "Creative must not reserve a thirst row");
					capture(client, "04-creative");
					next(8);
				}
				case 8 -> {
					onServer(client, player -> {
						player.setGameMode(GameType.SURVIVAL);
						player.setAirSupply(300);
						// Exercise the real client receiver's enable flag; shared config toggles are tested on Fabric.
						PacketDistributor.sendToPlayer(player, new ThirstSyncPayload(new ThirstSnapshot(15, false)));
						PacketDistributor.sendToPlayer(player, new TemperatureSyncPayload(new TemperatureSnapshot(TemperatureBand.FREEZING, false)));
					});
					next(9);
				}
				case 9 -> {
					if (client.gameMode.getPlayerMode() != GameType.SURVIVAL) return;
					require(height == 49, "A disabled server snapshot must remove the thirst row without a gap");
					capture(client, "05-disabled-snapshot");
					next(10);
				}
				case 10 -> {
					onServer(client, player -> {
						PacketDistributor.sendToPlayer(player, new ThirstSyncPayload(new ThirstSnapshot(15, true)));
						PacketDistributor.sendToPlayer(player, new TemperatureSyncPayload(new TemperatureSnapshot(TemperatureBand.FREEZING, true)));
						var mount = (Mob) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("horse")).create(player.level(), EntitySpawnReason.COMMAND);
						require(mount != null, "Could not create the test horse");
						mount.setPos(player.position());
						mount.setNoAi(true);
						player.level().addFreshEntity(mount);
						player.startRiding(mount, true, false);
					});
					next(11);
				}
				case 11 -> {
					if (!(client.player.getVehicle() instanceof LivingEntity mount)) return;
					int hearts = Math.min(30, (int) (mount.getMaxHealth() + 0.5F) / 2);
					int expected = 49 + ((hearts + 9) / 10) * 10;
					require(height == expected, "Mount hearts must reserve their rows below thirst");
					capture(client, "06-mounted");
					next(12);
				}
				case 12 -> {
					client.gui.hud.toggle();
					next(13);
				}
				case 13 -> {
					require(client.gui.hud.isHidden() && height == 39, "F1 must hide thirst and release its row");
					capture(client, "07-hidden-hud");
					next(14);
				}
				case 14 -> {
					client.gui.hud.toggle();
					onServer(client, player -> {
						double exposure = player.getData(temperatureType()).getExposure();
						require(exposure < 0 && exposure > -1, "Native NeoForge temperature ticks must warm exposure toward the plains biome baseline");
						player.stopRiding();
						player.getFoodData().setFoodLevel(10);
						player.setItemSlot(EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE.getDefaultInstance());
						equipProtection(player, TemperatureProtection.INSULATION);
						player.setPermanentlyInvulnerable(false);
						player.setHealth(2);
						player.setData(thirstType(), new ThirstState(0));
						dehydrationVictim = player;
					});
					next(15);
				}
				case 16 -> {
					if (!client.player.isAlive()) return;
					require(height == 59, "Respawn must restore a visible thirst row");
					capture(client, "08-after-respawn");
					next(17);
				}
				case 17 -> {
					onServer(client, player -> {
						require(player.getData(thirstType()).getHydration() > 0.99, "Death respawn must reset hydration");
						require(Math.abs(player.getData(temperatureType()).getExposure()) < 1e-6, "Death respawn must reset body temperature to comfortable");
						player.setPermanentlyInvulnerable(true);
						player.setData(thirstType(), new ThirstState(0.425));
						player.setData(temperatureType(), new TemperatureState(-0.375));
						require(player.getData(moistureType()).getWetness() == 0, "Death respawn must start dry");
						player.setData(moistureType(), new MoistureState(0.625));
						player.teleportTo(player.level().getServer().getLevel(Level.NETHER), 0.5, 80, 0.5, Set.of(), 0, 0, true);
						require(player.getData(thirstType()).getHydration() == 0.425, "Dimension transfer must preserve hydration");
						require(player.getData(temperatureType()).getExposure() == -0.375, "Dimension transfer must preserve body temperature");
						require(player.getData(moistureType()).getWetness() == 0.625, "Dimension transfer must preserve wetness");
					});
					next(18);
				}
				case 18 -> {
					if (client.level.dimension() != Level.NETHER) return;
					require(height == 59, "Dimension transfer must restore the owner's HUD");
					capture(client, "09-nether");
					next(28);
				}
				case 28 -> {
					onServer(client, player -> {
						player.teleportTo(player.level().getServer().overworld(), 0.5, -58, 0.5, Set.of(), 0, 0, true);
						player.setNoGravity(true);
						player.setData(temperatureType(), TemperatureState.comfortable());
						player.setData(moistureType(), MoistureState.dry());
						var pos = player.blockPosition();
						player.level().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
						player.level().setBlock(pos.above(), Blocks.WATER.defaultBlockState(), 3);
					});
					next(29);
				}
				case 29 -> {
					onServer(client, player -> {
						require(player.isInWater(), "The wetness fixture must touch real water");
						require(player.getData(moistureType()).getWetness() == 1, "Native water exposure must soak the player");
						require(player.getData(temperatureType()).getExposure() < 0, "Native wetness must cool the body");
					});
					checks.add("moisture-water-soaking-and-cooling");
					capture(client, "13-moisture-soaked");
					next(30);
				}
				case 30 -> {
					onServer(client, player -> {
						player.level().setBlock(new BlockPos(0, -58, 0), Blocks.AIR.defaultBlockState(), 3);
						player.level().setBlock(new BlockPos(0, -57, 0), Blocks.AIR.defaultBlockState(), 3);
						player.teleportTo(player.level().getServer().overworld(), 10.5, -55, 0.5, Set.of(), 0, 0, true);
						player.setData(moistureType(), MoistureState.dry());
						var weather = player.level().getWeatherData();
						weather.setClearWeatherTime(0);
						weather.setRainTime(1200);
						weather.setRaining(true);
						player.level().setRainLevel(1);
					});
					next(31);
				}
				case 31 -> {
					onServer(client, player -> {
						require(player.level().isRainingAt(player.blockPosition()), "The native rain fixture must be exposed");
						exposedWetness = player.getData(moistureType()).getWetness();
						require(exposedWetness > 0 && exposedWetness < 0.2, "Native rain must wet the player gradually");
					});
					checks.add("moisture-rain-exposure");
					capture(client, "14-moisture-damp");
					next(32);
				}
				case 32 -> {
					onServer(client, player -> {
						rainRoof = player.blockPosition().above(3);
						player.level().setBlock(rainRoof, Blocks.STONE.defaultBlockState(), 3);
						exposedWetness = player.getData(moistureType()).getWetness();
					});
					next(33);
				}
				case 33 -> {
					onServer(client, player -> {
						require(!player.level().isRainingAt(player.blockPosition()), "A real roof must block native rain");
						require(player.getData(moistureType()).getWetness() < exposedWetness, "Native sheltered players must dry during rain");
						player.level().setBlock(rainRoof, Blocks.AIR.defaultBlockState(), 3);
						player.level().getWeatherData().setRaining(false);
						player.level().getWeatherData().setRainTime(1200);
						player.level().setRainLevel(0);
						player.setData(moistureType(), MoistureState.dry());
					});
					checks.add("moisture-shelter-and-drying");
					next(34);
				}
				case 34 -> {
					capture(client, "15-moisture-dry");
					next(35);
				}
				case 35 -> {
					onServer(client, player -> {
						NearbyHeatNeoForgeCheck.prepare(player);
						player.setData(temperatureType(), TemperatureState.comfortable());
						player.setData(moistureType(), new MoistureState(0.8));
					});
					next(36);
				}
				case 36 -> {
					onServer(client, player -> {
						try {
							require(player.getData(temperatureType()).getExposure() > 0, "Native campfire warmth must offset wet cooling");
							require(player.getData(moistureType()).getWetness() < 0.795, "Campfires must speed up native drying");
						} finally {
							player.level().setBlock(player.blockPosition().east(2), Blocks.AIR.defaultBlockState(), 3);
							player.setData(moistureType(), MoistureState.dry());
						}
						player.level().setBlock(player.blockPosition().east(2), Blocks.LAVA.defaultBlockState(), 3);
						player.setData(temperatureType(), TemperatureState.comfortable());
						player.setData(moistureType(), new MoistureState(0.8));
					});
					checks.add("nearby-campfire-and-torch-warmth");
					next(37);
				}
				case 37 -> {
					onServer(client, player -> {
						try {
							require(player.getData(temperatureType()).getExposure() > 0, "Native lava warmth must offset wet cooling");
							require(player.getData(moistureType()).getWetness() < 0.795, "Lava must speed up native drying");
						} finally {
							NearbyHeatNeoForgeCheck.clearLava(player);
							player.setData(moistureType(), MoistureState.dry());
						}
					});
					checks.add("nearby-lava-warmth");
					next(19);
				}
				case 19 -> {
					onServer(client, player -> {
						// Keep the later damage fixture clear of the soaking test's flowing water.
						player.teleportTo(player.level().getServer().overworld(), 10.5, -55, 0.5, Set.of(), 0, 0, true);
						player.setData(temperatureType(), new TemperatureState(-1));
						player.setData(thirstType(), ThirstState.hydrated());
						// Isolate temperature pulses from a Nether transfer's residual fall distance.
						player.resetFallDistance();
						player.setNoGravity(true);
						player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
						player.getFoodData().setFoodLevel(10);
						player.setHealth(20);
						player.setPermanentlyInvulnerable(false);
						player.damageCooldownTime = 0;
						equipProtection(player, TemperatureProtection.INSULATION);
						player.getData(temperatureTimerType()).reset();
					});
					next(20);
				}
				case 20 -> {
					onServer(client, player -> {
						require(player.getHealth() == 20, "Four Insulation IV pieces must prevent native freezing damage");
						require(player.getActiveEffects().isEmpty(), "Temperature must not apply status effects");
						var food = player.getFoodData();
						var originalHealth = player.getHealth();
						resetFood(player, 18, 0);
						player.setHealth(10);
						for (int tick = 0; tick < 159; tick++) food.tick(player);
						require(player.getHealth() == 10, "Native freezing must delay natural healing to 160 ticks");
						food.tick(player);
						require(player.getHealth() == 11, "Native cold healing must retain vanilla healing amount");
						player.setHealth(originalHealth);
						resetFood(player, 10, 0);
					});
					checks.add("temperature-dedicated-cold-immunity");
					checks.add("temperature-cold-natural-healing");
					capture(client, "10-insulation-immunity");
					next(21);
				}
				case 21 -> {
					onServer(client, player -> {
						equipProtection(player, TemperatureProtection.THERMAL_PROTECTION);
						player.setData(temperatureType(), new TemperatureState(-1));
						player.getData(temperatureTimerType()).reset();
					});
					next(22);
				}
				case 22 -> {
					onServer(client, player -> require(Math.abs(player.getHealth() - 19) < 1e-6, "Four Thermal Protection IV pieces must halve a native cold damage pulse"));
					checks.add("temperature-universal-half-damage");
					capture(client, "11-thermal-partial-protection");
					next(23);
				}
				case 23 -> {
					onServer(client, player -> {
						equipProtection(player, TemperatureProtection.HEAT_PROTECTION);
						player.setData(temperatureType(), new TemperatureState(1));
						player.setHealth(20);
						player.damageCooldownTime = 0;
						player.getData(temperatureTimerType()).reset();
						resetFood(player, 20, 3.5f);
						player.setData(thirstType(), ThirstState.hydrated());
						hotHydration = 1;
						hotStartedAt = player.level().getServer().getTickCount();
					});
					next(24);
				}
				case 24 -> {
					onServer(client, player -> {
						require(player.getHealth() == 20, "Four Heat Protection IV pieces must prevent native overheating damage");
						require(player.getActiveEffects().isEmpty(), "Heat must not apply status effects");
						require(player.getFoodData().getFoodLevel() == 19, "Native heat must drain hunger through exhaustion");
						double elapsedSeconds = (player.level().getServer().getTickCount() - hotStartedAt) / 20.0;
						double expected = hotHydration - 2 * elapsedSeconds / 1200;
						require(Math.abs(player.getData(thirstType()).getHydration() - expected) < 0.0002, "Native Hot thirst must drain at twice the baseline rate");
					});
					checks.add("temperature-dedicated-heat-immunity");
					checks.add("temperature-heat-hunger-and-thirst");
					capture(client, "12-heat-immunity");
					next(25);
				}
				case 25 -> {
					onServer(client, player -> {
						for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
							player.setItemSlot(slot, ItemStack.EMPTY);
						}
						player.setData(temperatureType(), new TemperatureState(1));
						player.setHealth(2);
						player.damageCooldownTime = 0;
						player.getData(temperatureTimerType()).reset();
					});
					next(26);
				}
				default -> throw new AssertionError("Unknown test stage " + stage);
			}
		} catch (Throwable failure) {
			Constants.LOG.error("NeoForge thirst HUD check failed in stage {}", stage, failure);
			finish(client, "FAIL stage " + stage + "\n" + failure);
		}
	}

	private void next (int nextStage) {
		stage = nextStage;
		changedAt = System.nanoTime();
	}

	private void onServer (Minecraft client, Consumer<ServerPlayer> action) {
		pending = CompletableFuture.runAsync(() -> action.accept(client.getSingleplayerServer().getPlayerList().getPlayers().getFirst()), client.getSingleplayerServer());
	}

	private void capture (Minecraft client, String name) {
		Path target = client.gameDirectory.toPath().resolve("screenshots").resolve(name + ".png");
		var saved = new CompletableFuture<Void>();
		Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
			try (image) {
				Files.createDirectories(target.getParent());
				image.writeToFile(target);
				saved.complete(null);
			} catch (Exception error) {
				saved.completeExceptionally(error);
			}
		});
		checks.add(name);
		Constants.LOG.info("NeoForge HUD check: {}", name);
		// A stage may also queue server assertions; screenshots must not discard their failures.
		pending = CompletableFuture.allOf(pending, saved);
	}

	@SuppressWarnings("unchecked")
	private static AttachmentType<ThirstState> thirstType () {
		return (AttachmentType<ThirstState>) NeoForgeRegistries.ATTACHMENT_TYPES.getValue(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "thirst"));
	}

	@SuppressWarnings("unchecked")
	private static AttachmentType<MoistureState> moistureType () {
		return (AttachmentType<MoistureState>) NeoForgeRegistries.ATTACHMENT_TYPES.getValue(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "moisture"));
	}

	@SuppressWarnings("unchecked")
	private static AttachmentType<TemperatureDamageTimer> temperatureTimerType () {
		return (AttachmentType<TemperatureDamageTimer>) NeoForgeRegistries.ATTACHMENT_TYPES.getValue(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature_damage_timer"));
	}

	private static void equipProtection (ServerPlayer player, ResourceKey<Enchantment> key) {
		var enchantment = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			var piece = switch (slot) {
				case HEAD -> Items.DIAMOND_HELMET.getDefaultInstance();
				case CHEST -> Items.DIAMOND_CHESTPLATE.getDefaultInstance();
				case LEGS -> Items.DIAMOND_LEGGINGS.getDefaultInstance();
				case FEET -> Items.DIAMOND_BOOTS.getDefaultInstance();
				default -> throw new IllegalArgumentException("Not an armor slot");
			};
			piece.enchant(enchantment, 4);
			player.setItemSlot(slot, piece);
		}
	}

	private static void resetFood (ServerPlayer player, int foodLevel, float exhaustion) {
		var tag = new CompoundTag();
		tag.putInt("foodLevel", foodLevel);
		tag.putFloat("foodSaturationLevel", 0);
		tag.putFloat("foodExhaustionLevel", exhaustion);
		tag.putInt("foodTickTimer", 0);
		player.getFoodData().readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag));
	}

	@SuppressWarnings("unchecked")
	private static AttachmentType<TemperatureState> temperatureType () {
		return (AttachmentType<TemperatureState>) NeoForgeRegistries.ATTACHMENT_TYPES.getValue(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "temperature"));
	}

	private void finish (Minecraft client, String report) {
		finished = true;
		try {
			Files.writeString(client.gameDirectory.toPath().resolve("result.txt"), report + "\n");
		} catch (Exception error) {
			throw new RuntimeException(error);
		}
		client.stop();
	}

	private static void require (boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
