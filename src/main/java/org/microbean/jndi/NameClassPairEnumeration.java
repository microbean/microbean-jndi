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
import java.util.Objects;

import javax.naming.NameClassPair;
import javax.naming.NamingException;

public abstract class NameClassPairEnumeration extends AbstractNamingEnumeration<NameClassPair> {

  public NameClassPairEnumeration(final Iterable<? extends String> names) {
    this(Objects.requireNonNull(names).iterator());
  }
  
  public NameClassPairEnumeration(final Iterator<? extends String> names) {
    super(names);
  }
  
  @Override
  public NameClassPair next() throws NamingException {
    final String name = this.names.next();
    return new NameClassPair(name, this.getClassName(name));
  }
  
}
