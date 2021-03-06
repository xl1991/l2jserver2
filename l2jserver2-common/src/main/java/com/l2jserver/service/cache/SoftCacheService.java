/*
 * This file is part of l2jserver2 <l2jserver2.com>.
 *
 * l2jserver2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * l2jserver2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with l2jserver2.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.service.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.l2jserver.service.AbstractService;
import com.l2jserver.service.ServiceStartException;
import com.l2jserver.service.ServiceStopException;

/**
 * This {@link Cache} service implementation uses a {@link SoftReference} to
 * store values.
 * 
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 */
public class SoftCacheService extends AbstractService implements CacheService {
	/**
	 * The logger
	 */
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The interface cache
	 */
	private Cache<MethodInvocation, Object> interfaceCache;

	@Override
	protected void doStart() throws ServiceStartException {
		interfaceCache = createCache("interface-cache");
	}

	@Override
	public <T extends Cacheable> T decorate(final Class<T> interfaceType,
			final T instance) {
		Preconditions.checkNotNull(interfaceType, "interfaceType");
		Preconditions.checkNotNull(instance, "instance");
		Preconditions.checkArgument(interfaceType.isInterface(),
				"interfaceType is not an interface");

		log.debug("Decorating {} with cache", interfaceType);

		@SuppressWarnings("unchecked")
		final T proxy = (T) Proxy.newProxyInstance(this.getClass()
				.getClassLoader(), new Class[] { interfaceType },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method,
							Object[] args) throws Throwable {
						if (method.isAnnotationPresent(IgnoreCaching.class))
							return method.invoke(instance, args);
						final MethodInvocation invocation = new MethodInvocation(
								method, args);
						Object result = interfaceCache.get(invocation);
						if (result == null)
							return doInvoke(invocation, proxy, method, args);
						return result;
					}

					private Object doInvoke(MethodInvocation invocation,
							Object proxy, Method method, Object[] args)
							throws IllegalArgumentException,
							IllegalAccessException, InvocationTargetException {
						Object result = method.invoke(instance, args);
						interfaceCache.put(invocation, result);
						return result;
					}
				});
		return proxy;
	}

	@Override
	public <K, V> Cache<K, V> createCache(String name, int size) {
		log.debug("Creating cache {} with minimum size of {}", name, size);
		return new SoftCache<K, V>(name);
	}

	@Override
	public <K, V> Cache<K, V> createEternalCache(String name, int size) {
		log.debug("Creating eternal cache {} with minimum size of {}", name,
				size);
		return new EternalCache<K, V>(name);
	}

	@Override
	public <K, V> Cache<K, V> createCache(String name) {
		return createCache(name, 200);
	}

	@Override
	public <K, V> void dispose(Cache<K, V> cache) {
		log.debug("Disposing {}", cache);
		cache.clear();
	}

	@Override
	protected void doStop() throws ServiceStopException {
		dispose(interfaceCache);
		interfaceCache = null;
	}

	/**
	 * This class is a simple map implementation for cache usage.<br>
	 * <br>
	 * Value may be stored in map really long, but it for sure will be removed
	 * if there is low memory (and of course there isn't any strong reference to
	 * value object)
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 * @param <K>
	 *            the key type
	 * @param <V>
	 *            the value type
	 */
	private class SoftCache<K, V> extends AbstractReferenceCache<K, V>
			implements Cache<K, V> {
		/**
		 * This class is a {@link SoftReference} with additional responsibility
		 * of holding key object
		 * 
		 * @author <a href="http://www.rogiel.com">Rogiel</a>
		 */
		private class SoftEntry extends SoftReference<V> {
			/**
			 * The key
			 */
			private K key;

			/**
			 * @param key
			 *            the key
			 * @param referent
			 *            the value
			 * @param q
			 *            the queue
			 */
			SoftEntry(K key, V referent, ReferenceQueue<? super V> q) {
				super(referent, q);
				this.key = key;
			}

			/**
			 * @return the key
			 */
			K getKey() {
				return key;
			}
		}

		/**
		 * @param cacheName
		 *            the cache name
		 */
		public SoftCache(String cacheName) {
			super(cacheName);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected synchronized void cleanQueue() {
			SoftEntry en = null;
			while ((en = (SoftEntry) refQueue.poll()) != null) {
				K key = en.getKey();
				if (log.isDebugEnabled())
					log.debug("{} : cleaned up value for key: {}", cacheName,
							key);
				cacheMap.remove(key);
			}
		}

		@Override
		protected Reference<V> newReference(K key, V value,
				ReferenceQueue<V> vReferenceQueue) {
			return new SoftEntry(key, value, vReferenceQueue);
		}
	}
}
