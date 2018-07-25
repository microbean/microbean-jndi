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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import java.util.function.Function;

import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

public abstract class AbstractNamingEnumeration<K, T> implements NamingEnumeration<T> {

  protected final Iterator<? extends K> names;

  protected AbstractNamingEnumeration(final Iterator<? extends K> names) {
    super();
    this.names = Objects.requireNonNull(names);
  }
  
  @Override
  public void close() {

  }

  @Override
  public boolean hasMore() throws NamingException {
    return this.names.hasNext();
  }

  @Override
  public final boolean hasMoreElements() {
    try {
      return this.hasMore();
    } catch (final NamingException namingException) {
      throw (NoSuchElementException)new NoSuchElementException(namingException.getMessage()).initCause(namingException);
    }
  }

  @Override
  public final T nextElement() {
    try {
      return this.next();
    } catch (final NamingException namingException) {
      throw (NoSuchElementException)new NoSuchElementException(namingException.getMessage()).initCause(namingException);
    }
  }

  protected abstract Object get(final K name) throws NamingException;

  protected String getClassName(final K name) throws NamingException {
    final String returnValue;
    final Object object = this.get(name);
    if (object == null) {
      returnValue = null;
    } else {
      returnValue = object.getClass().getName();
    }
    return returnValue;
  }
  
}
