package io.github.zforgo.arquillian.junit5;

import org.jboss.arquillian.test.spi.TestRunnerAdaptor;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JUnitJupiterTestClassLifecycleManager extends ArquillianTestClassLifecycleManager {

    private static final Logger LOG = Logger.getLogger(JUnitJupiterTestClassLifecycleManager.class.getName());
    private static final String NAMESPACE_KEY = "arquillianNamespace";
    private static final String ADAPTOR_KEY = "testRunnerAdaptor";
    private static final String INTERCEPTED_TEMPLATE_NAMESPACE_KEY = "interceptedTestTemplates";
    private static final String RESULT_NAMESPACE_KEY = "results";

    private final ExtensionContext.Store store;
    private final ExtensionContext.Store templateStore;
    private final ExtensionContext.Store resultStore;


    JUnitJupiterTestClassLifecycleManager(ExtensionContext context) {
        LOG.finest(() -> String.format("JUnitJupiterTestClassLifecycleManager(context=%s)", context));
        store = context.getStore(ExtensionContext.Namespace.create(NAMESPACE_KEY));
        templateStore = context
            .getStore(ExtensionContext.Namespace.create(NAMESPACE_KEY, INTERCEPTED_TEMPLATE_NAMESPACE_KEY));
        resultStore = context.getStore(ExtensionContext.Namespace.create(NAMESPACE_KEY, RESULT_NAMESPACE_KEY));
    }


    @Override
    protected void setAdaptor(TestRunnerAdaptor testRunnerAdaptor) {
        LOG.finest(() -> String.format("setAdaptor(testRunnerAdaptor=%s)", testRunnerAdaptor));
        store.put(ADAPTOR_KEY, testRunnerAdaptor);
    }


    @Override
    protected TestRunnerAdaptor getAdaptor() {
        return store.get(ADAPTOR_KEY, TestRunnerAdaptor.class);
    }


    boolean isRegisteredTemplate(final Method method) {
        LOG.finest(() -> String.format("isRegisteredTemplate(method=%s)", method));
        final boolean isRegistered = templateStore.getOrDefault(method.toGenericString(), boolean.class, false);
        if (!isRegistered) {
            templateStore.put(method.toGenericString(), true);
        }
        return isRegistered;
    }


    void storeResult(String uniqueId, Throwable throwable) {
        LOG.log(Level.FINEST, String.format("storeResult(uniqueId=%s, throwable)", uniqueId), throwable);
        // TODO: find source and unwrap it where it is thrown, not here.
        if (throwable instanceof InvocationTargetException) {
            resultStore.put(uniqueId, throwable.getCause());
        } else {
            resultStore.put(uniqueId, throwable);
        }
    }


    Optional<Throwable> getResult(String uniqueId) {
        LOG.finest(() -> String.format("getResult(uniqueId=%s)", uniqueId));
        return Optional.ofNullable(resultStore.getOrDefault(uniqueId, Throwable.class, null));
    }
}
