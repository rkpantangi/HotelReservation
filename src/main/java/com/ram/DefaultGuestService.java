package com.ram;

import java.util.HashMap;
import java.util.Map;

/**
 * This is mostly for data stubbing for this test. In real-world, this is
 * generally a database or distributed cache etc.
 * 
 * @author Ram
 * 
 */
public class DefaultGuestService implements GuestService {
	private Map<String, Guest> cache = new HashMap<>();

	public DefaultGuestService(Map<String, Guest> cache) {
		this.cache.putAll(cache);
	}

	@Override
	public Guest getGuest(String guestId) {
		return cache.get(guestId);
	}
}
