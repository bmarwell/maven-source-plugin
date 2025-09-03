/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * This goal bundles all the sources into a jar archive. This goal functions the same as the jar goal but does not fork
 * the build and is suitable for attaching to the build lifecycle.
 *
 * @author pgier
 * @since 2.1
 */
@Mojo(name = "jar-no-fork", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class SourceJarNoForkMojo extends AbstractSourceJarMojo {
    /**
     * @since 2.2
     */
    @Parameter(property = "maven.source.classifier", defaultValue = "sources")
    protected String classifier;

    /**
     * {@inheritDoc}
     */
    protected List<String> getSources(MavenProject p) {
        List<String> compilerConfigurationPaths = new ArrayList<>();

        for (Plugin plugin : p.getModel().getBuild().getPlugins()) {
            if (!plugin.getArtifactId().equals("maven-compiler-plugin") || !plugin.getGroupId().equals("org.apache.maven.plugins")) {
                continue;
            }

            // TODO: detect execution configurations and default configuration whether it includes additional source roots.
            Object compilerConfig = plugin.getConfiguration();
            compilerConfigurationPaths.addAll(readPathsFromCompilerConfig(compilerConfig));

            for (PluginExecution execution : plugin.getExecutions()) {
                Object executionConfiguration = execution.getConfiguration();
                compilerConfigurationPaths.addAll(readPathsFromCompilerConfig(executionConfiguration));
            }

            break;
        }

        return Stream.concat(
            compilerConfigurationPaths.stream(),
            p.getCompileSourceRoots().stream()
        ).collect(Collectors.toList());
    }

    private Collection<String> readPathsFromCompilerConfig(Object compilerConfig) {
        if (compilerConfig == null) {
            return Collections.emptyList();
        }

        if (!(compilerConfig instanceof Xpp3Dom)) {
            return Collections.emptyList();
        }

        Xpp3Dom configuration = (Xpp3Dom) compilerConfig;
        Xpp3Dom compileSourceRoots = configuration.getChild("compileSourceRoots");

        if  (compileSourceRoots == null) {
            return Collections.emptyList();
        }

        Xpp3Dom[] compileSourceRootsChildren = compileSourceRoots.getChildren();

        List<String> compileSourcePaths = new ArrayList<>();

        for (Xpp3Dom child : compileSourceRootsChildren) {
            compileSourcePaths.add(child.getValue());
        }

        return compileSourcePaths;
    }

    /**
     * {@inheritDoc}
     */
    protected List<Resource> getResources(MavenProject p) {
        if (excludeResources) {
            return Collections.emptyList();
        }

        return p.getResources();
    }

    /**
     * {@inheritDoc}
     */
    protected String getClassifier() {
        return classifier;
    }
}
