/*
 * Copyright (C) 2021 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.event;

import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventTask;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;

final class CustomHandlerAdapter<F> {

  final String name;
  private final Function<F, BiFunction<Object, Object, EventTask>> handlerBuilder;
  final Predicate<Method> filter;
  final BiConsumer<Method, List<String>> validator;
  private final LambdaType<F> functionType;
  private final MethodHandles.Lookup methodHandlesLookup;

  @SuppressWarnings("unchecked")
  CustomHandlerAdapter(
      final String name,
      final Predicate<Method> filter,
      final BiConsumer<Method, List<String>> validator,
      final TypeToken<F> invokeFunctionType,
      final Function<F, BiFunction<Object, Object, EventTask>> handlerBuilder,
      final MethodHandles.Lookup methodHandlesLookup) {
    this.name = name;
    this.filter = filter;
    this.validator = validator;
    this.functionType = (LambdaType<F>) LambdaType.of(invokeFunctionType.getRawType());
    this.handlerBuilder = handlerBuilder;
    this.methodHandlesLookup = methodHandlesLookup;
  }

  UntargetedEventHandler buildUntargetedHandler(final Method method)
      throws IllegalAccessException {
    final MethodHandle methodHandle = methodHandlesLookup.unreflect(method);
    final MethodHandles.Lookup defineLookup = MethodHandles.privateLookupIn(
        method.getDeclaringClass(), methodHandlesLookup);
    final LambdaType<F> lambdaType = functionType.defineClassesWith(defineLookup);
    final F invokeFunction = LambdaFactory.create(lambdaType, methodHandle);
    final BiFunction<Object, Object, EventTask> handlerFunction =
        handlerBuilder.apply(invokeFunction);
    return targetInstance -> new EventHandler() {

      @Override
      public void execute(Object event) {
        throw new UnsupportedOperationException();
      }

      @Override
      public @Nullable EventTask executeAsync(Object event) {
        return handlerFunction.apply(targetInstance, event);
      }
    };
  }
}