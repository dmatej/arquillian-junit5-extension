package io.github.zforgo.arquillian.junit5;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.zforgo.arquillian.junit5.extension.RunModeEvent;
import org.jboss.arquillian.test.spi.LifecycleMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ExceptionUtils;

public class ArquillianExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback,
    InvocationInterceptor, TestExecutionExceptionHandler {

    private static final Logger LOG = Logger.getLogger(ArquillianExtension.class.getName());
	private static final String CHAIN_EXCEPTION_MESSAGE_PREFIX = "Chain of InvocationInterceptors never called invocation";
	public static final String RUNNING_INSIDE_ARQUILLIAN = "insideArquillian";

    private static final Predicate<ExtensionContext> isInsideArquillian = (context -> Boolean
        .parseBoolean(context.getConfigurationParameter(RUNNING_INSIDE_ARQUILLIAN).orElse("false")));

    private JUnitJupiterTestClassLifecycleManager lifecycleManager;

	private JUnitJupiterTestClassLifecycleManager getManager(ExtensionContext context) {
		if (lifecycleManager == null) {
			lifecycleManager = new JUnitJupiterTestClassLifecycleManager(context);
		}
		return lifecycleManager;
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
	    LOG.fine(() -> String.format("beforeAll(context=%s)", context));
		getManager(context).beforeTestClassPhase(context.getRequiredTestClass());
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
        LOG.fine(() -> String.format("afterAll(context=%s)", context));
		getManager(context).afterTestClassPhase(context.getRequiredTestClass());
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
	    LOG.fine(() -> String.format("beforeEach(context=%s)", context));
		getManager(context).getAdaptor().before(
				context.getRequiredTestInstance(),
				context.getRequiredTestMethod(),
				LifecycleMethodExecutor.NO_OP);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
        LOG.fine(() -> String.format("afterEach(context=%s)", context));
		getManager(context).getAdaptor().after(
				context.getRequiredTestInstance(),
				context.getRequiredTestMethod(),
				LifecycleMethodExecutor.NO_OP);
	}


    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        LOG.fine(
            () -> String.format("interceptTestTemplateMethod(invocation=%s, invocationContext=%s, extensionContext=%s)",
                invocation, invocationContext, extensionContext));
	    if (isInsideArquillian.test(extensionContext)) {
			// run inside arquillian
			invocation.proceed();
		} else {
            RunModeEvent runModeEvent = new RunModeEvent( //
                extensionContext.getRequiredTestInstance(), extensionContext.getRequiredTestMethod());
			getManager(extensionContext).getAdaptor().fireCustomLifecycle(runModeEvent);
			if (runModeEvent.isRunAsClient()) {
				// Run as client
				outsideArquillianInvocation(invocationContext, extensionContext);
			} else {
				// Run as container (but only once)
				if (!getManager(extensionContext).isRegisteredTemplate(invocationContext.getExecutable())) {
					outsideArquillianInvocation(invocationContext, extensionContext);
				}
				// Otherwise get result
                getManager(extensionContext).getResult(extensionContext.getUniqueId())
                    .ifPresent(ExceptionUtils::throwAsUncheckedException);
			}
		}
	}

	@Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {
        LOG.fine(() -> String.format("interceptTestMethod(invocation=%s, invocationContext=%s, extensionContext=%s)",
            invocation, invocationContext, extensionContext));
		if (isInsideArquillian.test(extensionContext)) {
			invocation.proceed();
		} else {
			outsideArquillianInvocation(invocationContext, extensionContext);
            getManager(extensionContext).getResult(extensionContext.getUniqueId())
                .ifPresent(ExceptionUtils::throwAsUncheckedException);
		}
	}


    private void outsideArquillianInvocation(ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext) throws Throwable {
        LOG.fine(() -> String.format("interceptInvocation(invocationContext=%s, extensionContext=%s)",
            invocationContext, extensionContext));
		ArquillianTestMethodExecutor executor = new ArquillianTestMethodExecutor(invocationContext, extensionContext);
        TestResult result = getManager(extensionContext).getAdaptor().test(executor);
		populateResults(result, extensionContext);
	}

	private void populateResults(TestResult result, ExtensionContext context) {
	    LOG.fine(() -> String.format("populateResults(result=%s, context=%s)", result, context));
		if (result.getThrowable() != null) {
			if (result.getThrowable() instanceof IdentifiedTestException) {
				((IdentifiedTestException) result.getThrowable()).getCollectedExceptions()
						.forEach((id, throwable) -> getManager(context).storeResult(id, throwable));
			} else {
				getManager(context).storeResult(context.getUniqueId(), result.getThrowable());
			}
		}
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
	    LOG.log(Level.FINE, String.format("handleTestExecutionException(context=%s, throwable)", context), throwable);
		if (throwable instanceof JUnitException && throwable.getMessage().startsWith(CHAIN_EXCEPTION_MESSAGE_PREFIX)) {
			return;
		}
		throw throwable;
	}
}
