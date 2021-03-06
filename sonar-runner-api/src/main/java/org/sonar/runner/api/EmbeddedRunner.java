/*
 * Sonar Runner - API
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.runner.api;

import org.sonar.runner.impl.BatchLauncher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of {@link Runner} that is executed in the same JVM. The application can inject
 * some extensions into Sonar IoC container (see {@link #addExtensions(Object...)}. It can be
 * used for example in the Maven Sonar plugin to register Maven components like MavenProject
 * or MavenPluginExecutor.
 * @since 2.2
 */
public class EmbeddedRunner extends Runner<EmbeddedRunner> {

  private final BatchLauncher batchLauncher;
  private final List<Object> extensions = new ArrayList<Object>();

  EmbeddedRunner(BatchLauncher bl) {
    this.batchLauncher = bl;
  }

  /**
   * Create a new instance.
   */
  public static EmbeddedRunner create() {
    return new EmbeddedRunner(new BatchLauncher());
  }

  /**
   * Sonar is executed in an almost fully isolated classloader. The unmasked packages
   * define the classes of the client application that are visible from Sonar classloader. They
   * relate to the extensions provided by {@link #setUnmaskedPackages(String...)}.
   */
  public EmbeddedRunner setUnmaskedPackages(String... packages) {
    return setProperty("sonarRunner.unmaskedPackages", Utils.join(packages, ","));
  }

  public EmbeddedRunner addExtensions(Object... objects) {
    extensions.addAll(Arrays.asList(objects));
    return this;
  }

  List<Object> extensions() {
    return extensions;
  }

  @Override
  protected void doExecute() {
    batchLauncher.execute(properties(), extensions);
  }
}
