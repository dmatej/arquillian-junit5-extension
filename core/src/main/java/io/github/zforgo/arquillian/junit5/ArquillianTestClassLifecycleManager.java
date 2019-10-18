package io.github.zforgo.arquillian.junit5;

import java.util.logging.Logger;

import org.jboss.arquillian.test.spi.LifecycleMethodExecutor;
import org.jboss.arquillian.test.spi.TestRunnerAdaptor;

//TODO move to common
public abstract class ArquillianTestClassLifecycleManager extends AdaptorManager {
    private static final Logger LOG = Logger.getLogger(ArquillianTestClassLifecycleManager.class.getName());

    @Override
    protected void handleSuiteLevelFailure(Throwable initializationException) {
        LOG.finest(() -> String.format("handleSuiteLevelFailure(initializationException=%s)", initializationException));
        throw new RuntimeException(
            "Arquillian initialization has already been attempted, but failed. See previous exceptions for cause",
            initializationException);
    }

    @Override
    protected void handleBeforeSuiteFailure(Exception e) throws Exception {
        LOG.finest(() -> String.format("handleBeforeSuiteFailure(e=%s)", e));
        State.runnerFinished();
        if (State.isLastRunner()) {
            State.clean();
        }
        throw e;
    }


    void beforeTestClassPhase(Class<?> testClass) throws Exception {
        LOG.finest(() -> String.format("beforeTestClassPhase(testClass=%s)", testClass));
        State.runnerStarted();
        initializeAdaptor();

        // initialization ok, run children
        if (State.hasTestAdaptor()) {
            setAdaptor(State.getTestAdaptor());
        }

        getAdaptor().beforeClass(testClass, LifecycleMethodExecutor.NO_OP);
    }

    void afterTestClassPhase(Class<?> testClass) throws Exception {
        TestRunnerAdaptor adaptor = getAdaptor();
        if (adaptor != null) {
            adaptor.afterClass(testClass, LifecycleMethodExecutor.NO_OP);
            shutdown(adaptor);
        }
    }
}
