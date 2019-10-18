package io.github.zforgo.arquillian.junit5.container;

import java.util.logging.Logger;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class JUnitJupiterContainerExtension implements LoadableExtension {
    private static final Logger LOG = Logger.getLogger(JUnitJupiterContainerExtension.class.getName());

	@Override
	public void register(ExtensionBuilder builder) {
	    LOG.config(() -> String.format("register(builder=%s)", builder));
		builder
				.service(AuxiliaryArchiveAppender.class, JUnitJupiterDeploymentAppender.class)
				.observer(RunModeEventHandler.class)
		;
	}
}
