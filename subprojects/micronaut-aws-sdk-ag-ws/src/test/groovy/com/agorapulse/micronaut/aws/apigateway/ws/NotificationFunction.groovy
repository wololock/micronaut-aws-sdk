/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2022 Agorapulse.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.agorapulse.micronaut.aws.apigateway.ws

import com.amazonaws.AmazonClientException
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import groovy.transform.Field

import javax.inject.Inject

@Inject @Field MessageSender sender                                                     // <1>

void notify(SNSEvent event) {                                                           // <2>
    event.records.each {
        try {
            sender.send(it.SNS.subject, "[SNS] $it.SNS.message")                        // <3>
        } catch (AmazonClientException ignored) {
            // can be gone                                                              // <4>
        }
    }
}
