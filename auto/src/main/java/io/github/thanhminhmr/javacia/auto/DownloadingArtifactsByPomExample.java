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

import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class DownloadingArtifactsByPomExample {
	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadingArtifactsByPomExample.class);

	private static final Path LOCAL_REPO_DIR = Path.of("./target/local-repository").toAbsolutePath();
	private static final @NotNull RemoteRepository CENTRAL_REPOSITORY =
			new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();

	public static void main(String[] args) throws Exception {
		final Path projectPom = Path.of("pom.xml").toRealPath();

		LOGGER.info("loading this sample project's Maven descriptor from {}", projectPom);
		LOGGER.info("local Maven repository set to {}", LOCAL_REPO_DIR);

		final RepositorySystem repositorySystem = getRepositorySystem();
		final RepositorySystemSession repositorySystemSession
				= getRepositorySystemSession(repositorySystem, LOCAL_REPO_DIR);
		final Model model = buildProjectModelFromPom(projectPom);

		LOGGER.info("Maven model resolved: {}, parsing its dependencies..", model);

		final MavenProject mavenProject = new MavenProject(model);
		final List<ArtifactResult> artifactResults
				= resolveDependencies(mavenProject, repositorySystemSession, repositorySystem);
		for (final ArtifactResult artifactResult : artifactResults) {
			final Artifact resultArtifact = artifactResult.getArtifact();
			LOGGER.info("artifact {} resolved to {}", resultArtifact, resultArtifact.getFile());
		}
	}

	private static @NotNull List<ArtifactResult> resolveDependencies(@NotNull MavenProject project,
			@NotNull RepositorySystemSession session, @NotNull RepositorySystem repoSystem)
			throws DependencyCollectionException, DependencyResolutionException {
		final CollectRequest collectRequest = new CollectRequest()
				.setRootArtifact(RepositoryUtils.toArtifact(project.getArtifact()))
				.setRepositories(project.getRemoteProjectRepositories())
				.addRepository(CENTRAL_REPOSITORY);

		final ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();
		for (final Dependency dependency : project.getDependencies()) {
			collectRequest.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
		}

		final DependencyManagement dependencyManagement = project.getDependencyManagement();
		if (dependencyManagement != null) {
			for (final Dependency dependency : dependencyManagement.getDependencies()) {
				collectRequest.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
			}
		}

		final CollectResult collectResult = repoSystem.collectDependencies(session, collectRequest);

		final DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null)
				.setRoot(collectResult.getRoot());

		final DependencyResult dependencyResult = repoSystem.resolveDependencies(session, dependencyRequest);

		return dependencyResult.getArtifactResults();
	}

	private static @NotNull RepositorySystem getRepositorySystem() {
		return MavenRepositorySystemUtils.newServiceLocator()
				.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class)
				.addService(TransporterFactory.class, FileTransporterFactory.class)
				.addService(TransporterFactory.class, HttpTransporterFactory.class)
				.getService(RepositorySystem.class);
	}

	private static @NotNull RepositorySystemSession getRepositorySystemSession(
			@NotNull RepositorySystem system, @NotNull Path localRepository) {
		final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		final LocalRepositoryManager repositoryManager
				= system.newLocalRepositoryManager(session, new LocalRepository(localRepository.toFile()));
		session.setLocalRepositoryManager(repositoryManager);
		session.setRepositoryListener(new ConsoleRepositoryEventListener());
		return session;
	}

	private static @NotNull Model buildProjectModelFromPom(@NotNull Path projectPom) throws ModelBuildingException {
		final ModelBuildingRequest request = new DefaultModelBuildingRequest().setPomFile(projectPom.toFile());
		final ModelBuildingResult modelBuildingResult = new DefaultModelBuilderFactory().newInstance().build(request);
		return modelBuildingResult.getEffectiveModel();
	}
}