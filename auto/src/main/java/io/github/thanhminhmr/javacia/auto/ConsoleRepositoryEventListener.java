/*
 * Copyright (C) 2020-2021 Mai Thanh Minh (a.k.a. thanhminhmr or mrmathami)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.thanhminhmr.javacia.auto;

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