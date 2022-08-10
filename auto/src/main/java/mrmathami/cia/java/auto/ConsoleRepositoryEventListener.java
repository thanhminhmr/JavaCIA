package mrmathami.cia.java.auto;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleRepositoryEventListener extends AbstractRepositoryListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleRepositoryEventListener.class);


	@Override
	public void artifactInstalled(RepositoryEvent event) {
		LOGGER.info("artifact {} installed to file {}", event.getArtifact(), event.getFile());
	}

	@Override
	public void artifactInstalling(RepositoryEvent event) {
		LOGGER.info("installing artifact {} to file {}", event.getArtifact(), event.getFile());
	}

	@Override
	public void artifactResolved(RepositoryEvent event) {
		LOGGER.info("artifact {} resolved from repository {}", event.getArtifact(), event.getRepository());
	}

	@Override
	public void artifactDownloading(RepositoryEvent event) {
		LOGGER.info("downloading artifact {} from repository {}", event.getArtifact(), event.getRepository());
	}

	@Override
	public void artifactDownloaded(RepositoryEvent event) {
		LOGGER.info("downloaded artifact {} from repository {}", event.getArtifact(), event.getRepository());
	}

	@Override
	public void artifactResolving(RepositoryEvent event) {
		LOGGER.info("resolving artifact {}", event.getArtifact());
	}
}