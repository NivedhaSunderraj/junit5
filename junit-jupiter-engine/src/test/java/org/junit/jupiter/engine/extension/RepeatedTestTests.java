/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.ExecutionEventConditions.container;
import static org.junit.platform.testkit.ExecutionEventConditions.displayName;
import static org.junit.platform.testkit.ExecutionEventConditions.event;
import static org.junit.platform.testkit.ExecutionEventConditions.finishedWithFailure;
import static org.junit.platform.testkit.TestExecutionResultConditions.message;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.testkit.ExecutionRecorder;
import org.junit.platform.testkit.ExecutionsResult;

/**
 * Integration tests for {@link RepeatedTest @RepeatedTest} and supporting
 * infrastructure.
 *
 * @since 5.0
 */
class RepeatedTestTests {

	private static int fortyTwo = 0;

	@BeforeEach
	@AfterEach
	void beforeAndAfterEach(TestInfo testInfo, RepetitionInfo repetitionInfo) {
		if (testInfo.getTestMethod().get().getName().equals("repeatedOnce")) {
			assertThat(repetitionInfo.getCurrentRepetition()).isEqualTo(1);
			assertThat(repetitionInfo.getTotalRepetitions()).isEqualTo(1);
		}
		else if (testInfo.getTestMethod().get().getName().equals("repeatedFortyTwoTimes")) {
			assertThat(repetitionInfo.getCurrentRepetition()).isBetween(1, 42);
			assertThat(repetitionInfo.getTotalRepetitions()).isEqualTo(42);
		}
	}

	@AfterAll
	static void afterAll() {
		assertEquals(42, fortyTwo);
	}

	@RepeatedTest(1)
	void repeatedOnce(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("repetition 1 of 1");
	}

	@RepeatedTest(42)
	void repeatedFortyTwoTimes(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).matches("repetition \\d+ of 42");
		fortyTwo++;
	}

	@RepeatedTest(1)
	@DisplayName("Repeat!")
	void customDisplayName(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("repetition 1 of 1");
	}

	@RepeatedTest(1)
	@DisplayName("   \t ")
	void customDisplayNameWithBlankName(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("repetition 1 of 1");
	}

	@RepeatedTest(value = 1, name = "{displayName}")
	@DisplayName("Repeat!")
	void customDisplayNameWithPatternIncludingDisplayName(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("Repeat!");
	}

	@RepeatedTest(value = 1, name = "#{currentRepetition}")
	@DisplayName("Repeat!")
	void customDisplayNameWithPatternIncludingCurrentRepetition(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("#1");
	}

	@RepeatedTest(value = 1, name = "Repetition #{currentRepetition} for {displayName}")
	@DisplayName("Repeat!")
	void customDisplayNameWithPatternIncludingDisplayNameAndCurrentRepetition(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("Repetition #1 for Repeat!");
	}

	@RepeatedTest(value = 1, name = RepeatedTest.LONG_DISPLAY_NAME)
	@DisplayName("Repeat!")
	void customDisplayNameWithPredefinedLongPattern(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("Repeat! :: repetition 1 of 1");
	}

	@RepeatedTest(value = 1, name = "{displayName} {currentRepetition}/{totalRepetitions}")
	@DisplayName("Repeat!")
	void customDisplayNameWithPatternIncludingDisplayNameCurrentRepetitionAndTotalRepetitions(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("Repeat! 1/1");
	}

	@RepeatedTest(value = 1, name = "Repetition #{currentRepetition} for {displayName}")
	void defaultDisplayNameWithPatternIncludingDisplayNameAndCurrentRepetition(TestInfo testInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo(
			"Repetition #1 for defaultDisplayNameWithPatternIncludingDisplayNameAndCurrentRepetition(TestInfo)");
	}

	@RepeatedTest(value = 5, name = "{displayName}")
	void injectRepetitionInfo(TestInfo testInfo, RepetitionInfo repetitionInfo) {
		assertThat(testInfo.getDisplayName()).isEqualTo("injectRepetitionInfo(TestInfo, RepetitionInfo)");
		assertThat(repetitionInfo.getCurrentRepetition()).isBetween(1, 5);
		assertThat(repetitionInfo.getTotalRepetitions()).isEqualTo(5);
	}

	@RepeatedTest(1)
	void failsContainerOnEmptyPattern() {
		ExecutionsResult executionsResult = execute(selectMethod(TestCase.class, "testWithEmptyPattern"));
		assertThat(executionsResult.getExecutionEvents()) //
				.haveExactly(1, event(container(), displayName("testWithEmptyPattern()"), //
					finishedWithFailure(message(value -> value.contains("must be declared with a non-empty name")))));
	}

	@RepeatedTest(1)
	void failsContainerOnBlankPattern() {
		ExecutionsResult executionsResult = execute(selectMethod(TestCase.class, "testWithBlankPattern"));
		assertThat(executionsResult.getExecutionEvents()) //
				.haveExactly(1, event(container(), displayName("testWithBlankPattern()"), //
					finishedWithFailure(message(value -> value.contains("must be declared with a non-empty name")))));
	}

	@RepeatedTest(1)
	void failsContainerOnNegativeRepeatCount() {
		ExecutionsResult executionsResult = execute(selectMethod(TestCase.class, "negativeRepeatCount"));
		assertThat(executionsResult.getExecutionEvents()) //
				.haveExactly(1, event(container(), displayName("negativeRepeatCount()"), //
					finishedWithFailure(message(value -> value.contains("must be declared with a positive 'value'")))));
	}

	@RepeatedTest(1)
	void failsContainerOnZeroRepeatCount() {
		ExecutionsResult executionsResult = execute(selectMethod(TestCase.class, "zeroRepeatCount"));
		assertThat(executionsResult.getExecutionEvents()) //
				.haveExactly(1, event(container(), displayName("zeroRepeatCount()"), //
					finishedWithFailure(message(value -> value.contains("must be declared with a positive 'value'")))));
	}

	private ExecutionsResult execute(DiscoverySelector... selectors) {
		return ExecutionRecorder.execute(new JupiterTestEngine(), request().selectors(selectors).build());
	}

	static class TestCase {

		@RepeatedTest(value = 1, name = "")
		void testWithEmptyPattern() {
		}

		@RepeatedTest(value = 1, name = " \t  ")
		void testWithBlankPattern() {
		}

		@RepeatedTest(-99)
		void negativeRepeatCount() {
		}

		@RepeatedTest(0)
		void zeroRepeatCount() {
		}
	}
}
