/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gemstone.gemfire.distributed;

import com.gemstone.gemfire.test.junit.categories.IntegrationTest;

import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

import static org.assertj.core.api.Assertions.assertThat;

@Category(IntegrationTest.class)
public class MemberNameGeneratorJUnitTest {

  MemberNameGenerator memberNameGenerator;

  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  @Rule
  public final TestName testName = new TestName();

  @Before
  public void setup() {
    memberNameGenerator = new GemsMemberNameGenerator();

    mockContext = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
      setThreadingPolicy(new Synchroniser());
    }};
  }

  @After
  public void tearDown() {
    mockContext.assertIsSatisfied();
    mockContext = null;
  }

  @Test
  public void testGenerate() throws Exception {
    String name = memberNameGenerator.generate();
    assertThat(name)
            .isNotNull()
            .isNotEmpty();
  }

  @Test
  public void testRandomNameGeneratation() throws Exception {
    assertThat(memberNameGenerator.generate()).isNotEqualTo(memberNameGenerator.generate());
  }

  private Mockery mockContext;


//
//  @Test
//  public void testExecutingCommandsOnAServer() throws Exception {
//    final String locatorString = "localHost[" + locatorPort + "]";
//
//    CommandStringBuilder csb = new CommandStringBuilder(CliStrings.START_SERVER);
////      csb.addOption(CliStrings.START_SERVER__NAME, serverName);
//    csb.addOption(CliStrings.START_SERVER__LOCATORS, locatorString);
//    csb.addOption(CliStrings.START_SERVER__SERVER_PORT, Integer.toString(port));
//    CommandResult cmdResult = executeCommand(gfsh, csb.getCommandString());
//
//
//    assertThat(memberNameGenerator.generate()).isNotEqualTo(memberNameGenerator.generate());
//  }

//  @Test
//  public void test1000RandomNamesGeneratation() throws Exception {
//    for (int i=0; i < 1000; i++)
//      assertThat(memberNameGenerator.generate()).isNotEqualTo(memberNameGenerator.generate());
//  }

}