package com.iso2t.sverve.platform;

import com.iso2t.sverve.Constants;
import com.iso2t.sverve.platform.services.PlatformHelper;
import lombok.experimental.UtilityClass;

import java.util.ServiceLoader;

@UtilityClass
public class Services {

	public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

	private static <T> T load (Class<T> type) {
		var providers = ServiceLoader.load(type, Services.class.getClassLoader()).iterator();
		if (!providers.hasNext()) {
			throw new IllegalStateException("Missing loader service: " + type.getName());
		}
		var service = providers.next();
		if (providers.hasNext()) {
			throw new IllegalStateException("Multiple loader services: " + type.getName());
		}
		Constants.LOG.debug("Loaded {} for {}", service.getClass().getName(), type.getName());
		return service;
	}
}
