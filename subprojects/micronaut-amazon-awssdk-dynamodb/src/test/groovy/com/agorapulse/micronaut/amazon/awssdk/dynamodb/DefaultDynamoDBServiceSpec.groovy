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
package com.agorapulse.micronaut.amazon.awssdk.dynamodb

import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Query
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Scan
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Service
import com.agorapulse.micronaut.amazon.awssdk.dynamodb.annotation.Update
import io.micronaut.context.ApplicationContext
import io.reactivex.Flowable
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.Instant
import java.time.temporal.ChronoUnit

// tag::builders-import[]
import static com.agorapulse.micronaut.amazon.awssdk.dynamodb.groovy.GroovyBuilders.*   // <1>
// end::builders-import[]
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB

@SuppressWarnings([
    'AbcMetric',
    'MethodCount',
    'MethodSize',
    'DuplicateListLiteral',
    'DuplicateNumberLiteral',
    'DuplicateStringLiteral',
])
/**
 * Specification for testing DefaultDynamoDBService using entity with range key.
 */
// tag::testcontainers-header[]
@Stepwise
@Testcontainers                                                                         // <1>
class DefaultDynamoDBServiceSpec extends Specification {

// end::testcontainers-header[]

    private static final Instant REFERENCE_DATE = Instant.ofEpochMilli(1358487600000)

    UnknownMethodsService unknownMethodsService
    Playbook playbook

    // tag::testcontainers-setup[]
    @AutoCleanup ApplicationContext context                                             // <2>

    @Shared LocalStackContainer localstack = new LocalStackContainer()                  // <3>
        .withServices(DYNAMODB)

    DynamoDBItemDBService service
    DynamoDbService<DynamoDBEntity> dbs

    void setup() {
        DynamoDbClient client = DynamoDbClient                                          // <4>
            .builder()
            .endpointOverride(localstack.getEndpointOverride(DYNAMODB))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.accessKey, localstack.secretKey
            )))
            .region(Region.of(localstack.region))
            .build()

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient
            .builder()
            .dynamoDbClient(client)
            .build()

        context = ApplicationContext.builder().build()
        context.registerSingleton(DynamoDbClient, client)                               // <5>
        context.registerSingleton(DynamoDbEnhancedClient, enhancedClient)               // <6>
        context.start()

        service = context.getBean(DynamoDBItemDBService)                                // <7>
        dbs = context.getBean(DynamoDBServiceProvider).findOrCreate(DynamoDBEntity)     // <8>
        // end::testcontainers-setup[]
        unknownMethodsService = context.getBean(UnknownMethodsService)
        playbook = context.getBean(Playbook)
    }

    void 'unsupported methods throws meaningful messages'() {
        when:
        unknownMethodsService.doSomething()
        then:
        UnsupportedOperationException e1 = thrown(UnsupportedOperationException)
        e1.message == 'Cannot implement method public abstract void com.agorapulse.micronaut.amazon.awssdk.dynamodb.UnknownMethodsService.doSomething()'

        when:
        unknownMethodsService.save()
        then:
        UnsupportedOperationException e2 = thrown(UnsupportedOperationException)
        e2.message == 'Method expects 1 parameter - item, iterable of items or array of items'

        when:
        unknownMethodsService.delete('1', '1', '1')
        then:
        UnsupportedOperationException e3 = thrown(UnsupportedOperationException)
        e3.message == 'Method expects at most 2 parameters - partition key and sort key, an item or items'

        when:
        unknownMethodsService.get('1', '1', '1')
        then:
        UnsupportedOperationException e4 = thrown(UnsupportedOperationException)
        e4.message == 'Method expects at most 2 parameters - partition key and sort key or sort keys'

        when:
        unknownMethodsService.findAll('1', '1')
        then:
        UnsupportedOperationException e5 = thrown(UnsupportedOperationException)
        e5.message == 'Method needs to have at least one argument annotated with @PartitionKey or with called \'partition\''
    }

    @SuppressWarnings([
        'AbcMetric',
        'UnnecessaryObjectReferences',
        'UnnecessaryBooleanExpression',
        'DuplicateStringLiteral',
        'DuplicateNumberLiteral',
    ])
    void 'service introduction works'() {
        when:
            // tag::save-entity[]
            service.save(new DynamoDBEntity(                                                        // <3>
                parentId: '1',
                id: '1',
                rangeIndex: 'foo',
                number: 1,
                date: Date.from(REFERENCE_DATE)
            ))
            // end::save-entity[]
            service.save(new DynamoDBEntity(parentId: '1', id: '2', rangeIndex: 'bar', number: 2, date: Date.from(REFERENCE_DATE.plus(1, ChronoUnit.DAYS))))
            service.saveAll([
                new DynamoDBEntity(parentId: '2', id: '1', rangeIndex: 'foo',  number: 3, date: Date.from(REFERENCE_DATE.minus(5, ChronoUnit.DAYS))),
                new DynamoDBEntity(parentId: '2', id: '2', rangeIndex: 'foo', number: 4, date: Date.from(REFERENCE_DATE.minus(2, ChronoUnit.DAYS)))])

            service.saveAll(
                new DynamoDBEntity(parentId: '3', id: '1', rangeIndex: 'foo', number: 5, date: Date.from(REFERENCE_DATE.plus(7, ChronoUnit.DAYS))),
                new DynamoDBEntity(parentId: '3', id: '2', rangeIndex: 'bar', number: 6, date: Date.from(REFERENCE_DATE.plus(14, ChronoUnit.DAYS)))
            )
            service.saveAll(Flowable.fromArray(
                new DynamoDBEntity(parentId: '4', id: '1', rangeIndex: 'boo', number: 5, date: Date.from(REFERENCE_DATE.plus(7, ChronoUnit.DAYS))),
                new DynamoDBEntity(parentId: '4', id: '2', rangeIndex: 'far', number: 6, date: Date.from(REFERENCE_DATE.plus(14, ChronoUnit.DAYS)))
            ))
        then:
            playbook.verifyAndForget(
                'PRE_PERSIST:1:1:foo:1',
                'PRE_PERSIST:1:1:foo:1',
                'POST_PERSIST:1:1:foo:1',
                'PRE_PERSIST:1:2:bar:2',
                'POST_PERSIST:1:2:bar:2',
                'PRE_PERSIST:2:1:foo:3',
                'PRE_PERSIST:2:2:foo:4',
                'POST_PERSIST:2:1:foo:3',
                'POST_PERSIST:2:2:foo:4',
                'PRE_PERSIST:3:1:foo:5',
                'PRE_PERSIST:3:2:bar:6',
                'POST_PERSIST:3:1:foo:5',
                'POST_PERSIST:3:2:bar:6',
                'PRE_PERSIST:4:1:boo:5',
                'PRE_PERSIST:4:2:far:6',
                'POST_PERSIST:4:1:boo:5',
                'POST_PERSIST:4:2:far:6'
            )

        when:
            // tag::load-entity[]
            service.get('1', '1')                                                                   // <4>
            // end::load-entity[]
        then:
            playbook.verifyAndForget(
                'POST_LOAD:1:1:foo:1'
            )

        when:
            service.load('1', '1')
        then:
            playbook.verifyAndForget(
                'POST_LOAD:1:1:foo:1'
            )

        when:
            service.getAll('1', ['2', '1'] as LinkedHashSet).size() == 2
        then:
            playbook.verifyAndForget(
                'POST_LOAD:1:2:bar:2',
                'POST_LOAD:1:1:foo:1'
            )

        when:
            service.loadAll('1', ['2', '1']).size() == 2
        then:
            playbook.verifyAndForget(
                'POST_LOAD:1:2:bar:2',
                'POST_LOAD:1:1:foo:1'
            )

        when:
            service.getAll('1', '2', '1').size() == 2
        then:
            playbook.verifyAndForget(
                'POST_LOAD:1:2:bar:2',
                'POST_LOAD:1:1:foo:1'
            )

        when:
            service.loadAll('1', '3', '4').size() == 0
        then:
            playbook.verifyAndForget()

        expect:
            service.save(new DynamoDBEntity(parentId: '1001', id: '1', rangeIndex: 'foo', number: 7, date: Date.from(REFERENCE_DATE)))
            service.save(new DynamoDBEntity(parentId: '1001', id: '2', rangeIndex: 'bar', number: 8, date: Date.from(REFERENCE_DATE.plus(1, ChronoUnit.DAYS))))
            service.saveAll([
                new DynamoDBEntity(parentId: '1002', id: '1', rangeIndex: 'foo',  number: 9, date: Date.from(REFERENCE_DATE.minus(5, ChronoUnit.DAYS))),
                new DynamoDBEntity(parentId: '1002', id: '2', rangeIndex: 'foo', number: 10, date: Date.from(REFERENCE_DATE.minus(2, ChronoUnit.DAYS))),
            ])
            service.saveAll(
                new DynamoDBEntity(parentId: '1003', id: '1', rangeIndex: 'foo', number: 11, date: Date.from(REFERENCE_DATE.plus(7, ChronoUnit.DAYS))),
                new DynamoDBEntity(parentId: '1003', id: '2', rangeIndex: 'bar', number: 12, date: Date.from(REFERENCE_DATE.plus(14, ChronoUnit.DAYS)))
            )

        when:
            playbook.forget()
            service.count('1') == 2
            service.count('1', '1') == 1
            service.countByRangeIndex('1', 'bar') == 1
            service.countByRangeNotContains('1', 'a') == 1
            service.countByRangeOfType('1', String) == 2
            service.countByRangeOfType('1', Number) == 0
            service.countByRangeNotExists('1') == 0
            service.countByRangeIsNull('1') == 0
            service.countByDates('1', Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(2, ChronoUnit.DAYS))) == 2
            service.countByDates('3', Date.from(REFERENCE_DATE.plus(9, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))) == 1

        then:
            playbook.verifyAndForget()

        when:
            service.query('1').count().blockingGet() == 2
        then:
            playbook.verifyAndForget(
                'POST_LOAD:1:1:foo:1',
                'POST_LOAD:1:2:bar:2'
            )

        expect:
            service.query('1', '1').count().blockingGet() == 1
            service.queryByRangeIndex('1', 'bar').count().blockingGet() == 1
            service.queryByRangeIndex('1', 'bar').blockingSingle().parentId == null // projection
            service.queryByRangeIndex('1', 'bar').blockingSingle().rangeIndex == 'bar' // projection
            !service.queryByRangeIndex('1', 'bar').blockingSingle().number

            service.queryByDates(
                '1',
                Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)),
                Date.from(REFERENCE_DATE.plus(2, ChronoUnit.DAYS))
            ).count().blockingGet() == 2

            service.queryByDatesWithLimit(
                '1',
                Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)),
                Date.from(REFERENCE_DATE.plus(2, ChronoUnit.DAYS)),
                1
            ).count().blockingGet() == 1

            service.queryByDates(
                '3',
                Date.from(REFERENCE_DATE.plus(9, ChronoUnit.DAYS)),
                Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))
            ).count().blockingGet() == 1

            service.queryByDates(
                '3',
                Date.from(REFERENCE_DATE.plus(9, ChronoUnit.DAYS)),
                Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))
            ).blockingFirst().number

            service.queryByPrefix('1', 'b').toList().blockingGet().size() == 1
            service.queryBySizeAndContains('1', 3, 'a').toList().blockingGet().size() == 1
            service.queryBySizeLe('1', 3).toList().blockingGet().size() == 2
            service.queryBySizeLt('1', 3).toList().blockingGet().size() == 0
            service.queryBySizeGe('1', 3).toList().blockingGet().size() == 2
            service.queryBySizeGt('1', 3).toList().blockingGet().size() == 0
            service.queryBySizeNe('1', 3).toList().blockingGet().size() == 0
            service.queryByLe('1', '1').toList().blockingGet().size() == 1
            service.queryByLt('1', '1').toList().blockingGet().size() == 0
            service.queryByGe('1', '1').toList().blockingGet().size() == 2
            service.queryByGt('1', '1').toList().blockingGet().size() == 1
            service.queryByBetween('1', '0', '5').toList().blockingGet().size() == 2
            service.queryByIdPrefix('1', '1').toList().blockingGet().size() == 1
            service.queryByNe('1', 'foo').toList().blockingGet().size() == 1
            service.queryInList('1', 'foo', 'bar').toList().blockingGet().size() == 2

            dbs.countUsingQuery {
                partitionKey '1'
                index DynamoDBEntity.DATE_INDEX
                range { between Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(2, ChronoUnit.DAYS)) }
            } == 2
            dbs.query {
                partitionKey '1'
                index DynamoDBEntity.DATE_INDEX
                range { between Date.from(REFERENCE_DATE.minus(1, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(2, ChronoUnit.DAYS)) }
            }.toList().blockingGet().size() == 2

        when:
            playbook.forget()
            service.scanAllByRangeIndex('bar').count().blockingGet() == 4
        then:
            playbook.verifyAndForget(
                'POST_LOAD:null:null:bar:0',
                'POST_LOAD:null:null:bar:0',
                'POST_LOAD:null:null:bar:0',
                'POST_LOAD:null:null:bar:0'
            )

        expect:
            service.scanAllByRangeIndex('foo').count().blockingGet() == 8
            service.countAllByRangeIndexNotEqual('bar') == 10
            service.countComplex('bar') == 4
            service.countBetween('a', 'z') == 14

            dbs.countUsingScan {
                index DynamoDBEntity.DATE_INDEX
                filter {
                    ne DynamoDBEntity.RANGE_INDEX, 'bar'
                }
            } == 10

            dbs.scan {
                filter {
                    eq DynamoDBEntity.RANGE_INDEX, 'foo'
                }
                only {
                    rangeIndex
                }
            }.toList().blockingGet().size() == 8

        when:
            List<DynamoDBEntity> scannedPage = service.scanAllByRangeIndexWithLimit('bar', 2, null)
        then:
            scannedPage.size() == 2

        when:
            List<DynamoDBEntity> nextPage = service.scanAllByRangeIndexWithLimit('bar', 2, scannedPage.last())
        then:
            nextPage.size() == 2
            scannedPage != nextPage

        when:
            playbook.forget()
            service.increment('1001', '1')
        then:
            playbook.verifyAndForget(
                'PRE_UPDATE:1001:1:null:0',
                'POST_UPDATE:null:null:null:8'
            )

        and:
            service.increment('1001', '1')
            service.increment('1001', '1')
            service.decrement('1001', '1') == 9
            service.get('1001', '1').number == 9
            service.minus3AndReturnOriginal('1001', '1') == 9
            service.get('1001', '1').number == 6
            service.minus5AndReturnAllOld('1001', '1') == 6
            service.get('1001', '1').number == 1
            service.minus7AndReturnAllNew('1001', '1') == -6
            service.get('1001', '1').number == -6
            service.minus13AndIgnore('1001', '1') || true
            service.get('1001', '1').number == -19
            service.resetAll('1001')
            service.get('1001', '1').number == 0

            service.setWhereNumberIsZero('1001', 123)
            service.get('1001', '1').number == 123

        and:
            dbs.update {
                partitionKey '1001'
                sortKey '1'
                add 'number', 13
                returns allNew
            }.number == 136

            dbs.updateAll(dbs.findAll('1001', '1')) {
                add 'number', 1
                returns none
            }

        when:
            playbook.forget()
            service.delete(service.get('1001', '1'))
        then:
            playbook.verifyAndForget(
                'POST_LOAD:1001:1:foo:137',
                'PRE_REMOVE:1001:1:foo:137',
                'POST_REMOVE:1001:1:foo:137'
            )
            service.count('1001', '1') == 0

        when:
            playbook.forget()
            service.delete('1003', '1')
        then:
            playbook.verifyAndForget(
                'PRE_REMOVE:1003:1:null:0',
                'POST_REMOVE:1003:1:null:0'
            )
            service.count('1003', '1') == 0

        when:
            playbook.forget()
            service.deleteByRangeIndex('1001', 'bar') == 1
        then:
            playbook.verifyAndForget(
                'POST_LOAD:1001:2:bar:0',
                'PRE_REMOVE:1001:2:bar:0',
                'POST_REMOVE:1001:2:bar:0'
            )
            service.countByRangeIndex('1001', 'bar') == 0

        and:
            service.deleteByDates('1002',  Date.from(REFERENCE_DATE.minus(20, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))) == 2
            service.countByDates('1002', Date.from(REFERENCE_DATE.minus(20, ChronoUnit.DAYS)), Date.from(REFERENCE_DATE.plus(20, ChronoUnit.DAYS))) == 0
        when:
            playbook.forget()
            service.deleteAll(nextPage)
        then:
            playbook.verifyAndForget(
                'PRE_REMOVE:3:2:bar:6',
                'PRE_REMOVE:1001:2:bar:8',
                'POST_REMOVE:3:2:bar:6',
                'POST_REMOVE:1001:2:bar:8',
            )
        and:
            service.deleteAllByRangeIndexNotEqual('bar')
    }

    void 'count many items'() {
        when:
            String parentKey = '2001'
            service.saveAll((1..101).collect { new DynamoDBEntity(parentId: parentKey, id: "$it") })
        then:
            service.count(parentKey) == 101
    }

}

@SuppressWarnings([
    'AbcMetric',
    'MethodCount',
    'MethodSize',
    'DuplicateNumberLiteral',
    'DuplicateStringLiteral',
])
// tag::service-all[]
// tag::service-header[]
@Service(DynamoDBEntity)                                                                // <2>
interface DynamoDBItemDBService {

// end::service-header[]

    DynamoDBEntity get(String hash, String rangeKey)
    DynamoDBEntity load(String hash, String rangeKey)
    List<DynamoDBEntity> getAll(String hash, Iterable<String> rangeKeys)
    List<DynamoDBEntity> getAll(String hash, String... rangeKeys)
    List<DynamoDBEntity> loadAll(String hash, List<String> rangeKeys)
    List<DynamoDBEntity> loadAll(String hash, String... rangeKeys)

    DynamoDBEntity save(DynamoDBEntity entity)
    List<DynamoDBEntity> saveAll(DynamoDBEntity... entities)
    List<DynamoDBEntity> saveAll(Iterable<DynamoDBEntity> entities)
    List<DynamoDBEntity> saveAll(Flowable<DynamoDBEntity> entities)

    int count(String hashKey)
    int count(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            index DynamoDBEntity.RANGE_INDEX
            range {
                eq rangeKey
            }
        }
    })
    int countByRangeIndex(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            index DynamoDBEntity.DATE_INDEX
            range { between after, before }
        }
    })
    int countByDates(String hashKey, Date after, Date before)

    Flowable<DynamoDBEntity> query(String hashKey)
    Flowable<DynamoDBEntity> query(String hashKey, String rangeKey)

    // tag::sample-queries[]
    @Query({                                                                            // <3>
        query(DynamoDBEntity) {
            partitionKey hashKey                                                        // <4>
            index DynamoDBEntity.RANGE_INDEX
            range {
                eq rangeKey                                                             // <5>
            }
            only {                                                                      // <6>
                rangeIndex                                                              // <7>
            }
        }
    })
    Flowable<DynamoDBEntity> queryByRangeIndex(String hashKey, String rangeKey)         // <8>
    // end::sample-queries[]

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            index DynamoDBEntity.DATE_INDEX
            range { between after, before }
        }
    })
    Flowable<DynamoDBEntity> queryByDates(String hashKey, Date after, Date before)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            index DynamoDBEntity.RANGE_INDEX
            range { beginsWith prefix }
        }
    })
    Flowable<DynamoDBEntity> queryByPrefix(String hashKey, String prefix)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter { inList DynamoDBEntity.RANGE_INDEX, values }
        }
    })
    Flowable<DynamoDBEntity> queryInList(String hashKey, String... values)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                and {
                    contains DynamoDBEntity.RANGE_INDEX, substring
                    sizeEq DynamoDBEntity.RANGE_INDEX, size
                }
            }
        }
    })
    Flowable<DynamoDBEntity> queryBySizeAndContains(String hashKey, int size, String substring)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                isNull DynamoDBEntity.RANGE_INDEX
            }
        }
    })
    int countByRangeIsNull(String hashKey)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                notExists DynamoDBEntity.RANGE_INDEX
            }
        }
    })
    int countByRangeNotExists(String hashKey)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                notContains DynamoDBEntity.RANGE_INDEX, value
            }
        }
    })
    int countByRangeNotContains(String hashKey, String value)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                typeOf DynamoDBEntity.RANGE_INDEX, type
            }
        }
    })
    int countByRangeOfType(String hashKey, Class<?> type)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                and {
                    sizeNe DynamoDBEntity.RANGE_INDEX, size
                }
            }
        }
    })
    Flowable<DynamoDBEntity> queryBySizeNe(String hashKey, int size)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                and {
                    sizeLe DynamoDBEntity.RANGE_INDEX, size
                }
            }
        }
    })
    Flowable<DynamoDBEntity> queryBySizeLe(String hashKey, int size)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                and {
                    sizeLt DynamoDBEntity.RANGE_INDEX, size
                }
            }
        }
    })
    Flowable<DynamoDBEntity> queryBySizeLt(String hashKey, int size)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                and {
                    sizeGe DynamoDBEntity.RANGE_INDEX, size
                }
            }
        }
    })
    Flowable<DynamoDBEntity> queryBySizeGe(String hashKey, int size)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                and {
                    sizeGt DynamoDBEntity.RANGE_INDEX, size
                }
            }
        }
    })
    Flowable<DynamoDBEntity> queryBySizeGt(String hashKey, int size)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            filter {
                group {
                    ne DynamoDBEntity.RANGE_INDEX, value
                }
            }
        }
    })
    Flowable<DynamoDBEntity> queryByNe(String hashKey, String value)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            range {
                le value
            }
        }
    })
    Flowable<DynamoDBEntity> queryByLe(String hashKey, String value)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            range {
                lt value
            }
        }
    })
    Flowable<DynamoDBEntity> queryByLt(String hashKey, String value)

    @Query({
        query(DynamoDBEntity) {
            order asc
            partitionKey hashKey
            range {
                ge value
            }
        }
    })
    Flowable<DynamoDBEntity> queryByGe(String hashKey, String value)

    @Query({
        query(DynamoDBEntity) {
            order desc
            partitionKey hashKey
            range {
                gt value
            }
        }
    })
    Flowable<DynamoDBEntity> queryByGt(String hashKey, String value)

    @Query({
        query(DynamoDBEntity) {
            inconsistent read
            partitionKey hashKey
            range {
                between lo, hi
            }
        }
    })
    Flowable<DynamoDBEntity> queryByBetween(String hashKey, String lo, String hi)

    @Query({
        query(DynamoDBEntity) {
            consistent read
            partitionKey hashKey
            range {
                    beginsWith prefix
            }
        }
    })
    Flowable<DynamoDBEntity> queryByIdPrefix(String hashKey, String prefix)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            index DynamoDBEntity.DATE_INDEX
            range { between after, before }
            limit max
        }
    })
    Flowable<DynamoDBEntity> queryByDatesWithLimit(String hashKey, Date after, Date before, int max)

    void delete(DynamoDBEntity entity)
    void delete(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            index DynamoDBEntity.RANGE_INDEX
            range {
                eq rangeKey
            }
        }
    })
    int deleteByRangeIndex(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
            index DynamoDBEntity.DATE_INDEX
            range { between after, before }
        }
    })
    int deleteByDates(String hashKey, Date after, Date before)

    void deleteAll(Collection<DynamoDBEntity> entities)

    // tag::sample-update[]
    @Update({                                                                           // <3>
        update(DynamoDBEntity) {
            partitionKey hashKey                                                        // <4>
            sortKey rangeKey                                                            // <5>
            add 'number', 1                                                             // <6>
            returnUpdatedNew { number }                                                 // <7>
        }
    })
    Number increment(String hashKey, String rangeKey)                                   // <8>
    // end::sample-update[]

    @Update({
        update(DynamoDBEntity) {
            partitionKey hashKey
            sortKey rangeKey
            add 'number', -1
            returnUpdatedNew { number }
        }
    })
    Number decrement(String hashKey, String rangeKey)

    @Update({
        update(DynamoDBEntity) {
            partitionKey hashKey
            sortKey rangeKey
            add 'number', -3
            returnUpdatedOld { number }
        }
    })
    Number minus3AndReturnOriginal(String hashKey, String rangeKey)

    @Update({
        update(DynamoDBEntity) {
            partitionKey hashKey
            sortKey rangeKey
            add 'number', -5
            returnAllOld { number }
        }
    })
    Number minus5AndReturnAllOld(String hashKey, String rangeKey)

    @Update({
        update(DynamoDBEntity) {
            partitionKey hashKey
            sortKey rangeKey
            add 'number', -7
            returnAllNew { number }
        }
    })
    Number minus7AndReturnAllNew(String hashKey, String rangeKey)

    @Update({
        update(DynamoDBEntity) {
            partitionKey hashKey
            sortKey rangeKey
            add 'number', -13
            returnNone()
        }
    })
    void minus13AndIgnore(String hashKey, String rangeKey)

    @Query({
        query(DynamoDBEntity) {
            partitionKey hashKey
        }
    })
    @Update({
        update(DynamoDBEntity) {
            put 'number', 0
        }
    })
    int resetAll(String hashKey)

    @Scan({
        scan(DynamoDBEntity) {
            filter {
                eq 'number', 0
            }
        }
    })
    @Update({
        update(DynamoDBEntity) {
            put 'number', value
        }
    })
    int setWhereNumberIsZero(String hashKey, int value)

    // tag::sample-scan[]
    @Scan({                                                                             // <3>
        scan(DynamoDBEntity) {
            filter {
                eq DynamoDBEntity.RANGE_INDEX, foo                                      // <4>
            }
            only {
                rangeIndex
            }
        }
    })
    Flowable<DynamoDBEntity> scanAllByRangeIndex(String foo)                            // <5>
    // end::sample-scan[]

    @Scan({
        scan(DynamoDBEntity) {
            filter {
                eq DynamoDBEntity.RANGE_INDEX, foo
            }
            inconsistent read
            page 1
            limit max
            lastEvaluatedKey lastInPreviousList
        }
    })
    List<DynamoDBEntity> scanAllByRangeIndexWithLimit(String foo, int max, DynamoDBEntity lastInPreviousList)

    @Scan({
        scan(DynamoDBEntity) {
            index DynamoDBEntity.DATE_INDEX
            filter {
                ne DynamoDBEntity.RANGE_INDEX, foo
            }
            consistent read
        }
    })
    int countAllByRangeIndexNotEqual(String foo)

    @Scan({
        scan(DynamoDBEntity) {
            filter {
                or {
                    group {
                        le DynamoDBEntity.RANGE_INDEX, foo
                        ge DynamoDBEntity.RANGE_INDEX, foo
                    }
                    group {
                        gt DynamoDBEntity.RANGE_INDEX, foo
                        lt DynamoDBEntity.RANGE_INDEX, foo
                    }
                }
            }
        }
    })
    int countComplex(String foo)

    @Scan({
        scan(DynamoDBEntity) {
            filter {
                between DynamoDBEntity.RANGE_INDEX, lo, hi
            }
            configure {
                addAttributeToProject DynamoDBEntity.RANGE_INDEX
            }
        }
    })
    int countBetween(String lo, String hi)

    @Scan({
        scan(DynamoDBEntity) {
            filter {
                and {
                    ne DynamoDBEntity.RANGE_INDEX, foo
                }
            }
            configure {
                attributesToProject 'parentId', 'id'
            }
        }
    })
    int deleteAllByRangeIndexNotEqual(String foo)

// tag::service-footer[]

}
// end::service-footer[]

@Service(DynamoDBEntity)                                                                // <2>
interface UnknownMethodsService {

    void doSomething()
    void save()
    void delete(String parentId, String id, String somethingElse)
    void get(String parentId, String id, String somethingElse)
    void findAll(String somethingElse, String anotherArgument)

}

// end::service-all[]
