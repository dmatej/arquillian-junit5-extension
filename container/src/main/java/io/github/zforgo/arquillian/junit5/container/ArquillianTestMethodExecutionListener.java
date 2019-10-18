package io.github.zforgo.arquillian.junit5.container;

import io.github.zforgo.arquillian.junit5.IdentifiedTestException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.jboss.arquillian.test.spi.TestResult;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.opentest4j.TestAbortedException;

class ArquillianTestMethodExecutionListener implements TestExecutionListener {

    private static final Logger LOG = Logger.getLogger(ArquillianTestMethodExecutionListener.class.getName());
	private final Map<String, Throwable> exceptions = new HashMap<>();

	@Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		exceptions.put(testIdentifier.getUniqueId(), new TestAbortedException(reason));
	}

	@Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        LOG.finest(() -> String.format("executionFinished(testIdentifier=%s, testExecutionResult=%s)", testIdentifier,
            testExecutionResult));
		TestExecutionResult.Status status = testExecutionResult.getStatus();
		LOG.finest(() -> String.format("status=%s", status));
		if (!testIdentifier.isTest()) {
			return;
		}
		switch (status) {
			case FAILED:
				exceptions.put(
						testIdentifier.getUniqueId(),
						testExecutionResult.getThrowable().orElseGet(() -> new Exception("Failed"))
				);
				break;
			case ABORTED:
				exceptions.put(
						testIdentifier.getUniqueId(),
						testExecutionResult.getThrowable().orElseGet(() -> new TestAbortedException("Aborted"))
				);
				break;
		}
	}

	public TestResult getTestResult() {
	    LOG.finest("getTestResult()");
		return exceptions.isEmpty() ? TestResult.passed() : TestResult.failed(new IdentifiedTestException(exceptions));
	}
}