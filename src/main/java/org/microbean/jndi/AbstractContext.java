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
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;

public abstract class AbstractContext implements Context {

  public static final Name EMPTY_NAME = new CompositeName() {

      private static final long serialVersionUID = 1L;
      
      @Override
      public final Name add(final int position, final String component) throws InvalidNameException {
        throw new InvalidNameException("EMPTY_NAME");
      }

      @Override
      public final Name add(final String component) throws InvalidNameException {
        throw new InvalidNameException("EMPTY_NAME");
      }

      @Override
      public final Name addAll(final int position, final Name name) throws InvalidNameException {
        throw new InvalidNameException("EMPTY_NAME");
      }

      @Override
      public final Name addAll(final Name name) throws InvalidNameException {
        throw new InvalidNameException("EMPTY_NAME");
      }

      @Override
      public final Object remove(final int position) throws InvalidNameException {
        throw new InvalidNameException("EMPTY_NAME");
      }
    };
  
  protected final Hashtable<Object, Object> environment;

  protected final NameParser nameParser;

  protected final Name prefix;

  protected AbstractContext(final Hashtable<?, ?> environment, final NameParser nameParser, final Name prefix) {
    super();
    this.nameParser = nameParser;
    if (environment == null || environment.isEmpty()) {
      this.environment = null;
    } else {
      this.environment = new Hashtable<>(environment);
    }
    if (prefix == null) {
      this.prefix = EMPTY_NAME;
    } else {
      this.prefix = prefix;
    }
  }

  @Override
  public NameParser getNameParser(final Name name) throws NamingException {
    return this.nameParser; // TODO: if name identifies a federation, have to traverse it
  }
  
  protected abstract Context copy();

  @Override
  public final Object lookup(final String name) throws NamingException {
    return this.lookup(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public void bind(final Name name, final Object obj) throws NamingException {
    throw new OperationNotSupportedException("bind");
  }

  @Override
  public final void bind(final String name, final Object obj) throws NamingException {
    this.bind(new CompositeName(Objects.requireNonNull(name)), obj);
  }

  @Override
  public void rebind(final Name name, final Object obj) throws NamingException {
    throw new OperationNotSupportedException("rebind");
  }

  @Override
  public final void rebind(final String name, final Object obj) throws NamingException {
    this.rebind(new CompositeName(Objects.requireNonNull(name)), obj);
  }

  @Override
  public void unbind(final Name name) throws NamingException {
    throw new OperationNotSupportedException("unbind");
  }

  @Override
  public final void unbind(final String name) throws NamingException {
    this.unbind(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public void rename(final Name oldName, final Name newName) throws NamingException {
    throw new OperationNotSupportedException("rename");
  }

  @Override
  public final void rename(final String oldName, String newName) throws NamingException {
    this.rename(new CompositeName(Objects.requireNonNull(oldName)), new CompositeName(Objects.requireNonNull(newName)));
  }

  @Override
  public final NamingEnumeration<NameClassPair> list(final String name) throws NamingException {
    return this.list(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public NamingEnumeration<Binding> listBindings(final String name) throws NamingException {
    return this.listBindings(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public void destroySubcontext(final Name name) throws NamingException {
    throw new OperationNotSupportedException("destroySubcontext");
  }

  @Override
  public final void destroySubcontext(final String name) throws NamingException {
    this.destroySubcontext(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public Context createSubcontext(final Name name) throws NamingException {
    throw new OperationNotSupportedException("createSubcontext");
  }

  @Override
  public final Context createSubcontext(final String name) throws NamingException {
    return this.createSubcontext(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public Object lookupLink(final Name name) throws NamingException {
    return this.lookup(name);
  }

  @Override
  public final Object lookupLink(final String name) throws NamingException {
    return this.lookupLink(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public final NameParser getNameParser(final String name) throws NamingException {
    return this.getNameParser(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public Name composeName(final Name name, final Name prefix) throws NamingException {
    Objects.requireNonNull(name);
    Objects.requireNonNull(prefix);
    final Name returnValue = (Name)(prefix.clone());
    returnValue.addAll(name);
    return returnValue;
  }

  @Override
  public final String composeName(final String name, final String prefix) throws NamingException {
    final Name composedName = this.composeName(new CompositeName(Objects.requireNonNull(name)), new CompositeName(prefix));
    return composedName.toString();
  }

  @Override
  public final Object addToEnvironment(final String propName, final Object propVal) throws NamingException {
    return this.environment.put(propName, propVal);
  }

  @Override
  public final Object removeFromEnvironment(final String propName) throws NamingException {
    return this.environment.remove(propName);
  }

  @Override
  public final Hashtable<?, ?> getEnvironment() throws NamingException {
    return new Hashtable<>(this.environment);
  }

  @Override
  public void close() {

  }

  @Override
  public String getNameInNamespace() throws NamingException {
    return this.prefix.toString();
  }

}
