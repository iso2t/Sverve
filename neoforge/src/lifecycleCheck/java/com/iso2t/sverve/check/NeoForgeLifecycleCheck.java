package com.iso2t.sverve.check;

import com.iso2t.sverve.test.LifecycleChecks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Test-only access to the production attachment registrations.
 */
@Mod("sverve_lifecycle_check")
public final class NeoForgeLifecycleCheck implements LifecycleChecks.Attachments {
	public NeoForgeLifecycleCheck () {
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> LifecycleChecks.run(event.getServer(), this));
	}

	@Override
	public Object get (ServerPlayer player, String name) {
		return player.getData(type(name));
	}

	@Override
	public void set (ServerPlayer player, String name, Object value) {
		player.setData(type(name), value);
	}

	@SuppressWarnings("unchecked")
	private static AttachmentType<Object> type (String name) {
		return (AttachmentType<Object>) NeoForgeRegistries.ATTACHMENT_TYPES.getValue(Identifier.fromNamespaceAndPath("sverve", name));
	}
}
