/*
 The MIT License

 Copyright (c) 2010-2016 Paul R. Holser, Jr.

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package net.jqwik;

import static java.util.Arrays.asList;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.junit.gen5.commons.util.ReflectionUtils;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

class PropertyVerifier {
	private final Method method;
	private final Object[] args;
	private final Consumer<Void> onSuccess;
	private final Consumer<TestAbortedException> onAssumptionViolated;
	private final Consumer<AssertionError> onFailure;
	private final Class<?> testClass;

	PropertyVerifier(Class<?> clazz, Method method, Object[] args, Consumer<Void> onSuccess,
			Consumer<TestAbortedException> onAssumptionViolated, Consumer<AssertionError> onFailure) {

		this.testClass = clazz;
		this.method = method;
		this.args = args;
		this.onSuccess = onSuccess;
		this.onAssumptionViolated = onAssumptionViolated;
		this.onFailure = onFailure;
	}

	void verify() throws Throwable {
		try {
			Object instance = null;
			if (!ReflectionUtils.isStatic(method))
				instance = ReflectionUtils.newInstance(testClass);
			Object result = ReflectionUtils.invokeMethod(method, instance, args);
			Class<?> returnType = method.getReturnType();
			if (returnType.equals(Boolean.class) || returnType.equals(boolean.class)) {
				if ((boolean) result)
					onSuccess.accept(null);
				else {
					String methodDescription = testClass.getName() + "#" + method.getName();
					String message = String.format("Method %s returned false", methodDescription);
					onFailure.accept(new AssertionFailedError(message));
				}
			}
			else {
				onSuccess.accept(null);
			}
		}
		catch (TestAbortedException e) {
			onAssumptionViolated.accept(e);
		}
		catch (AssertionError e) {
			onFailure.accept(e);
		}
		catch (Throwable e) {
			reportErrorWithArguments(e);
		}
	}

	private void reportErrorWithArguments(Throwable e) {
		throw new AssertionFailedError(
			String.format("Unexpected error in property %s with args %s", method.getName(), asList(args)), e);
	}
}
