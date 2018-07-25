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

public class MapContext extends AbstractContext<String> {

  private final Map<String, Object> map;

  public MapContext(final NameParser nameParser) {
    this(null, null, nameParser, null);
  }
  
  public MapContext(final Hashtable<?, ?> environment, final NameParser nameParser) {
    this(null, environment, nameParser, null);
  }
  
  public MapContext(final Map<? extends String, ?> map, final Hashtable<?, ?> environment, final NameParser nameParser, final Name prefix) {
    super(environment, nameParser, prefix);
    if (map == null || map.isEmpty()) {
      this.map = new HashMap<>();
    } else {
      this.map = new HashMap<>(map);
    }
  }

  @Override
  protected Context newContext() throws NamingException {
    return new MapContext(new HashMap<>(), this.environment, this.getNameParser(""), this.prefix);
  }

  @Override
  protected Context copy() throws NamingException {
    return new MapContext(this.map, this.environment, this.getNameParser(""), this.prefix);
  }

  @Override
  protected final boolean containsKey(final String mapKey) throws NamingException {
    return this.map.containsKey(mapKey);
  }
  
  @Override
  protected final Object get(final String mapKey) throws NamingException {
    return this.map.get(Objects.requireNonNull(mapKey));
  }

  @Override
  protected final Set<String> keySet() {
    return this.map.keySet();
  }
  
  @Override
  protected final Object remove(final String key) throws NamingException {
    return this.map.remove(Objects.requireNonNull(key));
  }

  @Override
  protected final Object put(final String key, final Object value) throws NamingException {
    return this.map.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
  }

  @Override
  protected final String extractKey(final Name name) throws NamingException {
    final Name compoundName = this.toCompoundName(Objects.requireNonNull(name));
    assert compoundName != null;
    final int size = compoundName.size();
    String component = null;
    for (int i = 0; i < size && (component == null || component.isEmpty()); i++) {
      component = compoundName.get(i);
    }
    final String returnValue;
    if (component == null || component.isEmpty()) {
      returnValue = "";
    } else {
      returnValue = component;
    }
    return returnValue;
  }

  @Override
  public void rename(final Name oldName, final Name newName) throws NamingException {
    Objects.requireNonNull(oldName);
    Objects.requireNonNull(newName);
    if (oldName.isEmpty()) {
      throw new InvalidNameException("oldName.isEmpty()");
    } else if (newName.isEmpty()) {
      throw new InvalidNameException("newName.isEmpty()");
    }

    final String newKey = this.toCompoundName(newName).toString();
    if (this.map.containsKey(newKey)) {
      throw new NameAlreadyBoundException(newName.toString());
    }

    final String oldKey = this.toCompoundName(oldName).toString();
    if (!this.map.containsKey(oldKey)) {
      throw new NameNotFoundException(oldName.toString());
    }
    
    this.put(newKey, this.remove(oldKey));
  }

}
