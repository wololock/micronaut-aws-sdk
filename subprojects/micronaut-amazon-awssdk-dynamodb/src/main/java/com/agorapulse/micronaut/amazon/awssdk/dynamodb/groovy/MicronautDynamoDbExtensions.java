/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2020 Agorapulse.
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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb.groovy;

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.*;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import space.jasan.support.groovy.closure.ConsumerWithDelegate;
import space.jasan.support.groovy.closure.FunctionWithDelegate;

public class MicronautDynamoDbExtensions {

    /**
     * One or more filter conditions in disjunction.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    public static <T> FilterConditionCollector<T> group(
        FilterConditionCollector<T> self,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>")
            Closure<FilterConditionCollector<T>> conditions
    ) {
        return self.group(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more filter conditions in disjunction.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    public static <T> FilterConditionCollector<T> or(
        FilterConditionCollector<T> self,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>")
            Closure<FilterConditionCollector<T>> conditions
    ) {
        return self.or(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more filter conditions in conjunction.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    public <T> FilterConditionCollector<T> and(
        FilterConditionCollector<T> self,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>")
            Closure<FilterConditionCollector<T>> conditions
    ) {
        return self.and(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more range key conditions.
     * @param conditions closure to build the conditions
     * @return self
     */
    public static <T> QueryBuilder<T> range(
        QueryBuilder<T> self,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.KeyConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.KeyConditionCollector<T>")
            Closure<KeyConditionCollector<T>> conditions
    ) {
        return self.range(ConsumerWithDelegate.create(conditions));
    }

    /**
     * One or more range key filter conditions.
     *
     * These conditions are resolved on the result set before returning the values and therefore they don't require an existing index
     * but they consume more resources as all the result set must be traversed.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    public static <T> QueryBuilder<T> filter(
        QueryBuilder<T> self,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>")
            Closure<FilterConditionCollector<T>> conditions
    ) {
        return self.filter(ConsumerWithDelegate.create(conditions));
    }

    /**
     * Configures the native query expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer closure to configure the native query expression
     * @return self
     */
    public static <T> QueryBuilder<T> configure(
        QueryBuilder<T> self,
        @DelegatesTo(type = "software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest.Builder")
            Closure<Object> configurer
    ) {
        return self.configure(ConsumerWithDelegate.create(configurer));
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param collector closure to collect the property paths
     * @return self
     */
    public static <T> QueryBuilder<T> only(
        QueryBuilder<T> self,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_ONLY)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> collector
    ) {
        return self.only(PathCollector.collectPaths(collector).getPropertyPaths());
    }

    /**
     * One or more filter conditions.
     *
     * @param conditions closure to build the conditions
     * @return self
     */
    public static <T> ScanBuilder<T> filter(
        ScanBuilder<T> self,
        @DelegatesTo(type = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "com.agorapulse.micronaut.amazon.awssdk.dynamodb.builder.FilterConditionCollector<T>")
            Closure<FilterConditionCollector<T>> conditions
    ) {
        return self.filter(ConsumerWithDelegate.create(conditions));
    }

    /**
     * Configures the native scan expression.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer closure to configure the native scan expression
     * @return self
     */
    public static <T> ScanBuilder<T> configure(
        ScanBuilder<T> self,
        @DelegatesTo(type = "software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest.Builder")
            Closure<Object> configurer
    ) {
        return self.configure(ConsumerWithDelegate.create(configurer));
    }

    /**
     * Limits which properties of the returned entities will be populated.
     * @param collector closure to collect the property paths
     * @return self
     */
    public static <T> ScanBuilder<T> only(
        ScanBuilder<T> self,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_ONLY)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> collector
    ) {
        return self.only(PathCollector.collectPaths(collector).getPropertyPaths());
    }

    /**
     * Declares a return value of the update operation.
     * @param returnValue whether none (default), old or new, all or updated attributes should be returned
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    public static <T> UpdateBuilder<T> returns(
        UpdateBuilder<T> self,
        ReturnValue returnValue,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return self.returns(returnValue, FunctionWithDelegate.create(mapper));
    }

    /**
     * Declares that the update operation will return all previous values.
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    public static <T> UpdateBuilder<T> returnAllOld(
        UpdateBuilder<T> self,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return self.returns(ReturnValue.ALL_OLD, FunctionWithDelegate.create(mapper));
    }

    /**
     * Declares that the update operation will only return updated previous values.
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    public static <T> UpdateBuilder<T> returnUpdatedOld(
        UpdateBuilder<T> self,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return self.returns(ReturnValue.UPDATED_OLD, FunctionWithDelegate.create(mapper));
    }

    /**
     * Declares that the update operation will return all new values.
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    public static <T> UpdateBuilder<T> returnAllNew(
        UpdateBuilder<T> self,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return self.returns(ReturnValue.ALL_NEW, FunctionWithDelegate.create(mapper));
    }

    /**
     * Declares that the update operation will only return updated new values.
     * @param mapper closure to map the returned entity to another value (e.g. value of the particular attribute)
     * @return self
     */
    public static <T> UpdateBuilder<T> returnUpdatedNew(
        UpdateBuilder<T> self,
        @DelegatesTo(type = "T", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "T")
            Closure<Object> mapper
    ) {
        return self.returns(ReturnValue.UPDATED_NEW, FunctionWithDelegate.create(mapper));
    }

    /**
     * Configures the native update request.
     *
     * This method is an extension point which allows to configure properties which are not provides by this builder.
     *
     * @param configurer closure to configure the native update request
     * @return self
     */
    public static <T> UpdateBuilder<T> configure(
        UpdateBuilder<T> self,
        @DelegatesTo(type = "software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest.Builder", strategy = Closure.DELEGATE_FIRST)
        @ClosureParams(value = FromString.class, options = "software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest.Builder")
            Closure<Object> configurer
    ) {
        return self.configure(ConsumerWithDelegate.create(configurer));
    }

}