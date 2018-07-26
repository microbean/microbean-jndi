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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;

import javax.naming.spi.NamingManager;

public abstract class AbstractContext<K> implements Context {

  private static final Pattern urlPattern = Pattern.compile("^[a-zA-Z]+?:(.*)$");

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

  private final NameParser nameParser;
  
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

  protected final Name toCompoundName(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    final Name returnValue;
    if (name instanceof CompositeName) {
      returnValue = this.toCompoundName((CompositeName)name);
    } else {
      returnValue = name;
    }
    return returnValue;
  }

  protected final Name toCompoundName(final CompositeName compositeName) throws NamingException {
    Objects.requireNonNull(compositeName);
    final NameParser nameParser = this.getNameParser(EMPTY_NAME);
    if (nameParser == null) {
      throw (NamingException)new NamingException().initCause(new IllegalStateException("getNameParser(\"\") == null"));
    }
    final Name returnValue = nameParser.parse("");
    assert returnValue != null;
    assert returnValue.size() == 0;
    assert returnValue.isEmpty();
    final int size = compositeName.size();
    assert size >= 0;
    if (size > 0) {
      int i = 0;
      for (; i < size; i++) {
        final String compositeNameComponent = compositeName.get(i);
        assert compositeNameComponent != null;
        if (!compositeNameComponent.isEmpty()) {
          String compositeNameNonUrlComponent = compositeNameComponent;
          if (i == 0) {
            final Matcher urlMatcher = urlPattern.matcher(compositeNameComponent);
            assert urlMatcher != null;
            if (urlMatcher.matches()) {
              compositeNameNonUrlComponent = urlMatcher.group(1);
            }
          }
          assert compositeNameNonUrlComponent != null;
          if (!compositeNameNonUrlComponent.isEmpty()) {
            try {
              returnValue.addAll(nameParser.parse(compositeNameNonUrlComponent));
            } catch (final InvalidNameException parsingFailed) {
              break;
            }
          }
        }
      }

      if (i < size) {
        // at this point i identifies the suffix to the CannotProceedException
        // TODO: deal with federation
        throw new InvalidNameException("compositeName could not be entirely parsed; successful parse: " + returnValue + "; remaining: " + compositeName.getSuffix(i));
      }
    }
    return returnValue;
  }
  
  protected abstract Context copy() throws NamingException;

  protected abstract Context newContext() throws NamingException;

  protected final Object get(final K key, final Name name) throws NamingException {
    final Object returnValue;
    Object temp = null;
    try {
      temp = NamingManager.getObjectInstance(this.get(key), name, this, this.environment);
    } catch (final RuntimeException throwMe) {
      throw throwMe;
    } catch (final NamingException throwMe) {
      throw throwMe;
    } catch (final Exception otherStuff) {
      throw (NamingException)new NamingException(otherStuff.getMessage()).initCause(otherStuff);
    } finally {
      returnValue = temp;
    }
    return returnValue;
  }

  protected boolean containsKey(final K key) throws NamingException {
    return this.get(Objects.requireNonNull(key)) != null;
  }
  
  protected abstract Object get(final K key) throws NamingException;

  protected abstract Object put(final K key, final Object value) throws NamingException;

  protected abstract Object remove(final K key) throws NamingException;

  protected abstract Set<K> keySet() throws NamingException;

  protected abstract K extractKey(final Name name) throws NamingException;
  
  @Override
  public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    final NamingEnumeration<NameClassPair> returnValue;
    if (name.isEmpty()) {
      returnValue = new NameClassPairEnumeration<K>(this.keySet()) {
          @Override
          protected final Object get(final K key) throws NamingException {
            final NameParser nameParser = getNameParser(EMPTY_NAME);
            if (nameParser == null) {
              return AbstractContext.this.get(key, null);
            } else {
              return AbstractContext.this.get(key, nameParser.parse(key.toString()));
            }
          }
        };
    } else {
      final Object target = this.lookup(name);
      if (target instanceof Context) {
        final Context context = (Context)target;
        NamingEnumeration<NameClassPair> temp = null;
        try {
          temp = context.list(EMPTY_NAME);
        } finally {
          returnValue = temp;
        }
      } else {
        returnValue = null;
        throw new NotContextException(name + " cannot be listed");
      }
    }
    return returnValue;
  }

  @Override
  public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    final NamingEnumeration<Binding> returnValue;
    if (name.isEmpty()) {
      returnValue = new BindingEnumeration<K>(this.keySet()) {
          @Override
          protected final Object get(final K key) throws NamingException {
            final NameParser nameParser = getNameParser(EMPTY_NAME);
            if (nameParser == null) {
              return AbstractContext.this.get(key, null);
            } else {
              return AbstractContext.this.get(key, nameParser.parse(key.toString()));
            }
          }
        };
    } else {
      final Object target = this.lookup(name);
      if (target instanceof Context) {
        final Context context = (Context)target;
        NamingEnumeration<Binding> temp = null;
        try {
          temp = context.listBindings(EMPTY_NAME);
        } finally {
          returnValue = temp;
        }
      } else {
        returnValue = null;
        throw new NotContextException(name + " cannot be listed");
      }
    }
    return returnValue;
  }
  
  @Override
  public final Object lookup(final String name) throws NamingException {
    return this.lookup(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public Object lookup(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    
    final Name compoundName = toCompoundName(name);
    assert compoundName != null;

    final Object returnValue;

    final int size = compoundName.size();
    if (size == 0) {
      assert compoundName.isEmpty();
      returnValue = this.copy();
    } else {
      assert size > 0;
      final K mapKey = this.extractKey(compoundName);
      if (mapKey == null ||
          (mapKey instanceof String && ((String)mapKey).isEmpty()) ||
          (mapKey instanceof Name && ((Name)mapKey).isEmpty())) {
        throw new InvalidNameException(name.toString());
      }
      final Object value = this.get(mapKey, compoundName);
      if (size == 1) {
        returnValue = value;
        if (returnValue == null) {
          throw new NameNotFoundException(name.toString());
        }
      } else if (value instanceof Context) {
        assert size > 1;
        returnValue = ((Context)value).lookup(compoundName.getSuffix(1));
      } else {
        throw new NotContextException(mapKey.toString());
      }
    }
    
    return returnValue;
  }

  @Override
  public final void bind(final Name name, final Object obj) throws NamingException {
    this.bind(name, obj, false);
  }

  protected void bind(final Name name, final Object obj, final boolean rebindPermitted) throws NamingException {
    Objects.requireNonNull(name);
    if (name.isEmpty()) {
      throw new InvalidNameException("name.isEmpty()");
    }

    final Name compoundName = this.toCompoundName(name);
    assert compoundName != null;
    if (compoundName.isEmpty()) {
      throw new InvalidNameException(name.toString());
    }
    
    final int size = compoundName.size();
    assert size > 0;

    final K mapKey = this.extractKey(compoundName);
    if (mapKey == null ||
        (mapKey instanceof String && ((String)mapKey).isEmpty()) ||
        (mapKey instanceof Name && ((Name)mapKey).isEmpty())) {
      throw new InvalidNameException(name.toString());
    }
    
    final Object value = this.get(mapKey, compoundName);
    if (size == 1) {
      if (value != null) {
        if (rebindPermitted) {
          final Object old = this.remove(mapKey);
          assert old == value;
        } else {
          throw new NameAlreadyBoundException(name.toString());
        }
      }
      this.put(mapKey, NamingManager.getStateToBind(obj, this.getNameParser(EMPTY_NAME).parse(mapKey.toString()), this, this.environment));
    } else if (value instanceof Context) {
      assert size > 1;
      ((Context)value).bind(compoundName.getSuffix(1), obj);
    } else {
      assert size > 1;
      throw new NotContextException(mapKey.toString());
    }
  }

  @Override
  public final void bind(final String name, final Object obj) throws NamingException {
    this.bind(new CompositeName(Objects.requireNonNull(name)), obj);
  }

  @Override
  public final void rebind(final Name name, final Object obj) throws NamingException {
    this.bind(name, obj, true);
  }

  @Override
  public final void rebind(final String name, final Object obj) throws NamingException {
    this.rebind(new CompositeName(Objects.requireNonNull(name)), obj);
  }

  @Override
  public void unbind(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    if (name.isEmpty()) {
      throw new InvalidNameException("name.isEmpty()");
    }

    final Name compoundName = this.toCompoundName(name);
    assert compoundName != null;
    if (compoundName.isEmpty()) {
      throw new InvalidNameException(name.toString());
    }
    
    final int size = compoundName.size();
    assert size > 0;

    final K mapKey = this.extractKey(compoundName);
    if (mapKey == null ||
        (mapKey instanceof String && ((String)mapKey).isEmpty()) ||
        (mapKey instanceof Name && ((Name)mapKey).isEmpty())) {
      throw new InvalidNameException(name.toString());
    }

    if (size == 1) {
      this.remove(mapKey);
    } else {
      assert size > 1;
      final Object value = this.get(mapKey, compoundName);
      if (value instanceof Context) {
        ((Context)value).unbind(compoundName.getSuffix(1));
      } else {
        throw new NotContextException(mapKey.toString());
      }
    }
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
    if (name == null || name.isEmpty()) {
      return this.list(EMPTY_NAME);
    } else {
      return this.list(new CompositeName(Objects.requireNonNull(name)));
    }
  }

  @Override
  public NamingEnumeration<Binding> listBindings(final String name) throws NamingException {
    if (name == null || name.isEmpty()) {
      return this.listBindings(EMPTY_NAME);
    } else {
      return this.listBindings(new CompositeName(Objects.requireNonNull(name)));
    }
  }

  @Override
  public void destroySubcontext(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    if (name.isEmpty()) {
      throw new InvalidNameException("name.isEmpty()");
    }
    final Name compoundName = this.toCompoundName(name);
    assert compoundName != null;
    if (!compoundName.isEmpty()) {
      final K mapKey = this.extractKey(compoundName);
      if (mapKey == null ||
          (mapKey instanceof String && ((String)mapKey).isEmpty()) ||
          (mapKey instanceof Name && ((Name)mapKey).isEmpty())) {
        throw new InvalidNameException(name.toString());
      }
      final int size = compoundName.size();
      assert size > 0;
      Object value = this.get(mapKey, compoundName);
      if (size == 1) {
        if (value != null) {
          if (value instanceof Context) {
            final Context subcontext = (Context)value;
            final Enumeration<?> enumeration = subcontext.list(EMPTY_NAME);
            if (enumeration != null && enumeration.hasMoreElements()) {
              throw new ContextNotEmptyException(name.toString());
            }
            subcontext.close();
            final Object old = this.remove(mapKey);
            assert old == subcontext;
          } else {
            throw new NotContextException(mapKey + " in " + name.toString());
          }
        }
      } else if (value instanceof Context) {
        assert size > 1;
        ((Context)value).destroySubcontext(compoundName.getSuffix(1));
      } else {
        throw new NotContextException(mapKey + " in " + name.toString());
      }
    } else {
      // Specification says that this method should be a no-op if the
      // compound name is not bound in this context; an empty compound
      // name *is* this context so it is not bound.
    }
  }
  
  @Override
  public final void destroySubcontext(final String name) throws NamingException {
    this.destroySubcontext(new CompositeName(Objects.requireNonNull(name)));
  }

  @Override
  public Context createSubcontext(final Name name) throws NamingException {
    Objects.requireNonNull(name);
    if (name.isEmpty()) {
      throw new InvalidNameException("name.isEmpty()");
    }
    final Name compoundName = toCompoundName(name);
    assert compoundName != null;
    final int size = compoundName.size();
    assert size > 0;
    final K mapKey = this.extractKey(compoundName);
    if (mapKey == null ||
        (mapKey instanceof String && ((String)mapKey).isEmpty()) ||
        (mapKey instanceof Name && ((Name)mapKey).isEmpty())) {
      throw new InvalidNameException(name.toString());
    }
    if (this.containsKey(mapKey)) {
      throw new NameAlreadyBoundException(name.toString());
    }
    final Object value = this.get(mapKey, compoundName);
    final Context returnValue;
    if (size == 1) {
      returnValue = this.newContext();
      if (returnValue == null) {
        throw (NamingException)new NamingException().initCause(new IllegalStateException("newContext() == null"));
      }
      final Object old = this.put(mapKey, returnValue);
      assert old == null;
    } else if (value instanceof Context) {
      assert size > 1;
      returnValue = ((Context)value).createSubcontext(compoundName.getSuffix(1));
    } else {
      throw new NotContextException(mapKey + " in " + name.toString());
    }
    return returnValue;
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
  public NameParser getNameParser(final Name name) throws NamingException {
    // The word "NameParser" shows up only twice, total, in what
    // passes for the JNDI specification
    // (https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/spec/jndi/).
    // There is no indication what the name parameter is used for in
    // this method.
    //
    // The javadoc says "Retrieves the parser associated with the
    // named context."  This suggests that the supplied Name must
    // (ultimately) name a Context, but what if the supplied Name is a
    // CompositeName, as it will be when getNameParser(String) is
    // called?
    //
    // The tutorial
    // (https://docs.oracle.com/javase/jndi/tutorial/provider/basics/parser.html)
    // also implies that the name supplied here must be a compound
    // name, and an example does a lookup(Name) on it to verify it
    // exists.  But this is circular: part of what lookup(Name) must
    // do is parse components of what might be a CompositeName, and to
    // do that it needs a name parser.
    //
    // All of this craziness suggests that this method is used to get
    // the NameParser affiliated with a Context implementation stored
    // in THIS Context implementation under the supplied compound
    // name.  Recall that the supplied compound name might be (and
    // usually is) empty, which means this very context itself.
    //
    // If we are handed a CompositeName, it's hard to know what to do,
    // since name parsing is so very fundamental to separating out the
    // concepts of composite and compound names.  For example, if we
    // were to try to use the toCompoundName(Name) method here, we
    // would get an infinite loop, because it uses a NameParser to
    // parse the CompositeName components.
    final NameParser returnValue;
    if (name == null || name.isEmpty()) {
      returnValue = this.nameParser;
    } else {
      final Object potentialSubcontext = this.lookup(name);
      if (potentialSubcontext instanceof Context) {
        returnValue = ((Context)potentialSubcontext).getNameParser(EMPTY_NAME);
      } else {
        throw new NotContextException(name.toString());
      }
    }
    return returnValue;
  }
  
  @Override
  public final NameParser getNameParser(final String name) throws NamingException {
    if (name == null || name.isEmpty()) {
      return this.getNameParser(EMPTY_NAME);
    } else {
      return this.getNameParser(new CompositeName(name));
    }
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
  public void close() throws NamingException {
    final NamingEnumeration<Binding> contents = this.listBindings(EMPTY_NAME);
    if (contents != null) {
      while (contents.hasMore()) {
        final Binding binding = contents.next();
        if (binding != null) {
          final Object value = binding.getObject();
          if (value instanceof Context) {
            ((Context)value).close();
          }
        }
      }
    }
  }

  @Override
  public String getNameInNamespace() throws NamingException {
    return this.prefix.toString();
  }

}
