/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.config.loaderwriter;

import org.ehcache.internal.classes.ClassInstanceProviderFactoryConfig;
import org.ehcache.spi.loaderwriter.DefaultCacheLoaderWriterFactory;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;
import org.ehcache.spi.service.ServiceConfiguration;

/**
 * @author Alex Snaps
 */
public class DefaultCacheLoaderWriterFactoryConfiguration extends ClassInstanceProviderFactoryConfig<CacheLoaderWriter<?, ?>> implements ServiceConfiguration<DefaultCacheLoaderWriterFactory> {

  @Override
  public Class<DefaultCacheLoaderWriterFactory> getServiceType() {
    return DefaultCacheLoaderWriterFactory.class;
  }

  public DefaultCacheLoaderWriterFactoryConfiguration addLoaderFor(String alias, final Class<? extends CacheLoaderWriter<?, ?>> clazz) {
    getDefaults().put(alias, clazz);
    return this;
  }
}
