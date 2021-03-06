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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;
import org.sonar.runner.impl.JarExtractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ForkedRunnerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_create() {
    ForkedRunner runner = ForkedRunner.create();
    assertThat(runner).isNotNull().isInstanceOf(ForkedRunner.class);
  }

  @Test
  public void should_print_to_standard_outputs_by_default() throws IOException {
    JarExtractor jarExtractor = mock(JarExtractor.class);
    final File jar = temp.newFile();
    when(jarExtractor.extract("sonar-runner-impl")).thenReturn(jar);

    CommandExecutor commandExecutor = mock(CommandExecutor.class);
    ForkedRunner runner = new ForkedRunner(jarExtractor, commandExecutor);
    runner.execute();

    verify(commandExecutor).execute(any(Command.class), argThat(new StdConsumerMatcher(System.out)), argThat(new StdConsumerMatcher(System.err)), anyLong());
  }

  static class StdConsumerMatcher extends ArgumentMatcher<StreamConsumer> {
    PrintStream output;

    StdConsumerMatcher(PrintStream output) {
      this.output = output;
    }

    public boolean matches(Object o) {
      return ((PrintStreamConsumer) o).output == output;
    }
  }

  @Test
  public void test_java_command() throws IOException {
    JarExtractor jarExtractor = mock(JarExtractor.class);
    final File jar = temp.newFile();
    when(jarExtractor.extract("sonar-runner-impl")).thenReturn(jar);

    CommandExecutor commandExecutor = mock(CommandExecutor.class);

    ForkedRunner runner = new ForkedRunner(jarExtractor, commandExecutor);
    runner.setJavaExecutable("java");
    runner.setProperty("sonar.dynamicAnalysis", "false");
    runner.setProperty("sonar.login", "admin");
    runner.addJvmArguments("-Xmx512m");
    runner.addJvmEnvVariables(System.getenv());
    runner.setJvmEnvVariable("SONAR_HOME", "/path/to/sonar");
    runner.setStdOut(mock(StreamConsumer.class));
    runner.setStdErr(mock(StreamConsumer.class));

    assertThat(runner.jvmArguments()).contains("-Xmx512m");
    runner.execute();

    verify(commandExecutor).execute(argThat(new ArgumentMatcher<Command>() {
      public boolean matches(Object o) {
        Command command = (Command) o;
        assertThat(command.toStrings()).hasSize(6);
        assertThat(command.toStrings()[0]).isEqualTo("java");
        assertThat(command.toStrings()[1]).isEqualTo("-Xmx512m");
        assertThat(command.toStrings()[2]).isEqualTo("-cp");
        assertThat(command.toStrings()[3]).isEqualTo(jar.getAbsolutePath());
        assertThat(command.toStrings()[4]).isEqualTo("org.sonar.runner.impl.BatchLauncherMain");

        // env variables
        assertThat(command.envVariables().size()).isGreaterThan(1);
        assertThat(command.envVariables().get("SONAR_HOME")).isEqualTo("/path/to/sonar");

        // the properties
        String propsPath = command.toStrings()[5];
        assertThat(propsPath).endsWith(".properties");
        Properties properties = new Properties();
        try {
          properties.load(new FileInputStream(propsPath));
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
        assertThat(properties.size()).isGreaterThan(2);
        assertThat(properties.getProperty("sonar.dynamicAnalysis")).isEqualTo("false");
        assertThat(properties.getProperty("sonar.login")).isEqualTo("admin");
        assertThat(properties.getProperty("-Xmx512m")).isNull();
        assertThat(properties.getProperty("SONAR_HOME")).isNull();
        // default values
        assertThat(properties.getProperty("sonar.task")).isEqualTo("scan");
        assertThat(properties.getProperty("sonar.host.url")).isEqualTo("http://localhost:9000");
        return true;
      }
    }), any(PrintStreamConsumer.class), any(PrintStreamConsumer.class), anyLong());

  }
}
