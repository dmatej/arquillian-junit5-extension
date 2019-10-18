package io.github.zforgo.arquillian.junit5;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

final class ArquillianTestMethodExecutor implements TestMethodExecutor {

    private final ReflectiveInvocationContext<Method> invocationContext;
    private final ExtensionContext extensionContext;


    ArquillianTestMethodExecutor(ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) {
        this.invocationContext = invocationContext;
        this.extensionContext = extensionContext;
    }


    @Override
    public String getMethodName() {
        return extensionContext.getRequiredTestMethod().getName();
    }


    @Override
    public Method getMethod() {
        return extensionContext.getRequiredTestMethod();
    }


    @Override
    public Object getInstance() {
        return extensionContext.getRequiredTestInstance();
    }


    @Override
    public void invoke(Object... parameters) throws InvocationTargetException, IllegalAccessException {
        getMethod().invoke(getInstance(), invocationContext.getArguments().toArray());
    }
}
