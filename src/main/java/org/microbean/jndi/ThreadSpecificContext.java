/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2018 microBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.jndi;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingException;

public class ThreadSpecificContext extends AbstractContext<String> {

  private static final ThreadLocal<Map<ThreadSpecificContext, Map<String, Object>>> threadAndInstanceSpecificMaps = ThreadLocal.withInitial(() -> new HashMap<>());

  public ThreadSpecificContext(final NameParser nameParser) {
    this(null, null, nameParser, null);
  }
  
  public ThreadSpecificContext(final Hashtable<?, ?> environment, final NameParser nameParser) {
    this(null, environment, nameParser, null);
  }
  
  public ThreadSpecificContext(final Map<? extends String, ?> map, final Hashtable<?, ?> environment, final NameParser nameParser, final Name prefix) {
    // TODO: should environment be thread-and-instance-specific as well?  Probably not.
    super(environment, nameParser, prefix);
    final Map<String, Object> storage;
    if (map == null || map.isEmpty()) {
      storage = new HashMap<>();
    } else {
      storage = new HashMap<>(map);
    }
    threadAndInstanceSpecificMaps.get().put(this, storage);
  }

  @Override
  public void close() throws NamingException {
    try {
      super.close();
    } finally {
      threadAndInstanceSpecificMaps.remove();
    }
  }

  @Override
  protected Context newContext(final Name prefix) throws NamingException {
    return new MapContext(new HashMap<>(), this.environment, this.getNameParser(EMPTY_NAME), Objects.requireNonNull(prefix));
  }

  @Override
  protected Context copy() throws NamingException {
    return new ThreadSpecificContext(threadAndInstanceSpecificMaps.get().get(this), this.environment, this.getNameParser(EMPTY_NAME), this.prefix);
  }

  @Override
  protected final boolean containsKey(final String mapKey) throws NamingException {
    return threadAndInstanceSpecificMaps.get().get(this).containsKey(mapKey);
  }
  
  @Override
  protected final Object get(final String mapKey) throws NamingException {
    return threadAndInstanceSpecificMaps.get().get(this).get(mapKey);
  }

  @Override
  protected final Set<String> keySet() {
    return threadAndInstanceSpecificMaps.get().get(this).keySet();
  }
  
  @Override
  protected final Object remove(final String key) throws NamingException {
    return threadAndInstanceSpecificMaps.get().get(this).remove(key);
  }

  @Override
  protected final Object put(final String key, final Object value) throws NamingException {
    return threadAndInstanceSpecificMaps.get().get(this).put(key, value);
  }

  @Override
  protected final String extractKey(final Name name) throws NamingException {
    final Name compoundName = this.toCompoundName(Objects.requireNonNull(name));
    assert compoundName != null;
    final String returnValue;
    if (compoundName.isEmpty()) {
      returnValue = "";
    } else {
      returnValue = compoundName.get(0);
    }
    return returnValue;
  }

}
