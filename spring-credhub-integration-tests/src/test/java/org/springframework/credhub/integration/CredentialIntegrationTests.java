/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.credhub.integration;

import org.junit.Before;
import org.junit.Test;
import org.springframework.credhub.core.CredHubException;
import org.springframework.credhub.core.credential.CredHubCredentialOperations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.CredentialSummary;
import org.springframework.credhub.support.CredentialType;
import org.springframework.credhub.support.SimpleCredentialName;
import org.springframework.credhub.support.WriteMode;
import org.springframework.credhub.support.password.PasswordParameters;
import org.springframework.credhub.support.user.UserCredential;
import org.springframework.credhub.support.user.UserParametersRequest;
import org.springframework.credhub.support.value.ValueCredential;
import org.springframework.credhub.support.value.ValueCredentialRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class CredentialIntegrationTests extends CredHubIntegrationTests {
	private static final SimpleCredentialName CREDENTIAL_NAME =
			new SimpleCredentialName("spring-credhub", "integration-test", "test-credential");
	private static final String CREDENTIAL_VALUE = "test-value";

	private CredHubCredentialOperations credentials;

	@Before
	public void setUp() {
		credentials = operations.credentials();

		try {
			credentials.deleteByName(CREDENTIAL_NAME);
		} catch (CredHubException e) {
			// ignore failing deletes on cleanup
		}
	}

	@Test
	public void writeCredential() {
		CredentialDetails<ValueCredential> written = credentials.write(ValueCredentialRequest.builder()
				.name(CREDENTIAL_NAME)
				.value(CREDENTIAL_VALUE)
				.build());
		assertThat(written.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(written.getValue().getValue()).isEqualTo(CREDENTIAL_VALUE);
		assertThat(written.getCredentialType()).isEqualTo(CredentialType.VALUE);
		assertThat(written.getId()).isNotNull();

		CredentialDetails<ValueCredential> byId = credentials.getById(written.getId(), ValueCredential.class);
		assertThat(byId.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(byId.getValue().getValue()).isEqualTo(CREDENTIAL_VALUE);
		assertThat(byId.getCredentialType()).isEqualTo(CredentialType.VALUE);

		CredentialDetails<ValueCredential> byName = credentials.getByName(CREDENTIAL_NAME, ValueCredential.class);
		assertThat(byName.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(byName.getValue().getValue()).isEqualTo(CREDENTIAL_VALUE);
		assertThat(byName.getCredentialType()).isEqualTo(CredentialType.VALUE);

		List<CredentialSummary> foundByName = credentials.findByName(new SimpleCredentialName("/test"));
		assertThat(foundByName).hasSize(1);
		assertThat(foundByName).extracting("name").extracting("name").containsExactly(CREDENTIAL_NAME.getName());

		List<CredentialSummary> foundByPath = credentials.findByPath("/spring-credhub/integration-test");
		assertThat(foundByPath).hasSize(1);
		assertThat(foundByPath).extracting("name").extracting("name").containsExactly(CREDENTIAL_NAME.getName());

		credentials.deleteByName(CREDENTIAL_NAME);

		List<CredentialSummary> afterDelete = credentials.findByName(CREDENTIAL_NAME);
		assertThat(afterDelete).hasSize(0);
	}

	@Test
	public void overwriteCredentialV2() {
		assumeTrue(serverApiIsV2());

		CredentialDetails<ValueCredential> written = credentials.write(ValueCredentialRequest.builder()
				.name(CREDENTIAL_NAME)
				.value(CREDENTIAL_VALUE)
				.build());
		assertThat(written.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(written.getValue().getValue()).isEqualTo(CREDENTIAL_VALUE);
		assertThat(written.getCredentialType()).isEqualTo(CredentialType.VALUE);
		assertThat(written.getId()).isNotNull();

		CredentialDetails<ValueCredential> overwritten = credentials.write(ValueCredentialRequest.builder()
				.name(CREDENTIAL_NAME)
				.value("new-value")
				.build());
		assertThat(overwritten.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(overwritten.getValue().getValue()).isEqualTo("new-value");
	}

	@Test
	public void overwriteCredentialV1() {
		assumeTrue(serverApiIsV1());

		CredentialDetails<ValueCredential> written = credentials.write(ValueCredentialRequest.builder()
				.name(CREDENTIAL_NAME)
				.value(CREDENTIAL_VALUE)
				.mode(WriteMode.OVERWRITE)
				.build());
		assertThat(written.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(written.getValue().getValue()).isEqualTo(CREDENTIAL_VALUE);
		assertThat(written.getCredentialType()).isEqualTo(CredentialType.VALUE);
		assertThat(written.getId()).isNotNull();

		CredentialDetails<ValueCredential> overwritten = credentials.write(ValueCredentialRequest.builder()
				.name(CREDENTIAL_NAME)
				.value("new-value")
				.build());
		assertThat(overwritten.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(overwritten.getValue().getValue()).isEqualTo("new-value");
	}

	@Test
	public void generateCredential() {
		CredentialDetails<UserCredential> generated = credentials.generate(UserParametersRequest.builder()
				.name(CREDENTIAL_NAME)
				.username("test-user")
				.parameters(PasswordParameters.builder()
						.length(12)
						.excludeLower(false)
						.excludeUpper(false)
						.excludeNumber(false)
						.includeSpecial(true)
						.build())
				.build());
		assertThat(generated.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(generated.getCredentialType()).isEqualTo(CredentialType.USER);
		assertThat(generated.getValue().getUsername()).isEqualTo("test-user");
		assertThat(generated.getValue().getPassword()).matches("^[a-zA-Z0-9\\p{Punct}]{12}$");
		assertThat(generated.getValue().getPasswordHash()).isNotNull();

		CredentialDetails<UserCredential> retrieved = credentials.getById(generated.getId(), UserCredential.class);
		assertThat(retrieved.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());

		CredentialDetails<UserCredential> regenerated = credentials.regenerate(CREDENTIAL_NAME, UserCredential.class);
		assertThat(regenerated.getName().getName()).isEqualTo(CREDENTIAL_NAME.getName());
		assertThat(regenerated.getValue().getUsername()).isEqualTo("test-user");
		assertThat(regenerated.getValue().getPassword()).matches("^[a-zA-Z0-9\\p{Punct}]{12}$");
		assertThat(regenerated.getValue().getPassword()).isNotEqualTo(generated.getValue().getPassword());
		assertThat(regenerated.getValue().getPasswordHash()).isNotEqualTo(generated.getValue().getPasswordHash());

		credentials.deleteByName(CREDENTIAL_NAME);

		List<CredentialSummary> afterDelete = credentials.findByName(CREDENTIAL_NAME);
		assertThat(afterDelete).hasSize(0);
	}
}
