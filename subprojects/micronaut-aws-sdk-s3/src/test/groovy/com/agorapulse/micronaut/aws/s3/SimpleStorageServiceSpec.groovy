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
package com.agorapulse.micronaut.aws.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import io.micronaut.context.ApplicationContext
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.TempDir

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3

/**
 * Tests for SimpleStorageService based on Testcontainers.
 */
@SuppressWarnings('NoJavaUtilDate')
// tag::header[]
@Stepwise
@Testcontainers                                                                         // <1>
class SimpleStorageServiceSpec extends Specification {

// end::header[]

    private static final String KEY = 'foo/bar.baz'
    private static final String UPLOAD_KEY = 'foo/foo.two'
    private static final String MY_BUCKET = 'testbucket'
    private static final String SAMPLE_CONTENT = 'hello world!'
    private static final Date TOMORROW = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)

    // tag::setup[]
    @AutoCleanup ApplicationContext context                                             // <2>

    @Shared LocalStackContainer localstack = new LocalStackContainer()                  // <3>
        .withServices(S3)

    @TempDir File tmp

    AmazonS3 amazonS3
    SimpleStorageService service

    void setup() {
        amazonS3 = AmazonS3Client                                                       // <4>
            .builder()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
            .withCredentials(localstack.defaultCredentialsProvider)
            .build()

        context = ApplicationContext
            .builder('aws.s3.bucket': MY_BUCKET)                                        // <5>
            .build()
        context.registerSingleton(AmazonS3, amazonS3)                                   // <6>
        context.start()

        service = context.getBean(SimpleStorageService)                                 // <7>
    }
    // end::setup[]

    void 'new bucket'() {
        when:
            service.createBucket(MY_BUCKET)
        then:
            service.listBucketNames().contains(MY_BUCKET)
    }

    void 'upload file'() {
        when:
            File file = new File(tmp, 'foo.txt')
            file.createNewFile()
            file.text = SAMPLE_CONTENT

            service.storeFile('bar/foo.txt', file)
        then:
            service.exists('bar/foo.txt')
    }

    void 'upload content'() {
        expect:
            service.listObjectSummaries('foo').count().blockingGet() == 0
        when:
            service.storeInputStream(
                KEY,
                new ByteArrayInputStream(SAMPLE_CONTENT.bytes),
                new ObjectMetadata(
                    contentLength: SAMPLE_CONTENT.size(),
                    contentType: 'text/plain',
                    contentDisposition: 'bar.baz'
                )
            )
        then:
            service.listObjectSummaries('foo').blockingFirst().key == KEY
    }

    void 'generate presigned URL'() {
        when:
            String url = service.generatePresignedUrl(KEY, TOMORROW)
        then:
            url
            new URL(url).text == SAMPLE_CONTENT
    }

    void 'download file'() {
        when:
            File file = new File(tmp, 'bar.baz')

            service.getFile(KEY, file)
        then:
            file.exists()
            file.text == SAMPLE_CONTENT
    }

    void 'generate upload URL'() {
        when:
            String uploadUrl = service.generateUploadUrl(UPLOAD_KEY, TOMORROW)

            HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection()
            connection.doOutput = true
            connection.requestMethod = 'PUT'
            connection.setRequestProperty('User-Agent', 'Groovy')

            connection.outputStream.withWriter { Writer w ->
                w.write('Hello')
            }

        then:
            connection.responseCode == 200
            service.exists(UPLOAD_KEY)
    }

    void 'delete file'() {
        when:
            service.deleteFile(KEY)
            service.deleteFile(UPLOAD_KEY)
            service.deleteFile('bar/foo.txt')
        then:
            !service.exists(KEY)
            !service.exists(UPLOAD_KEY)
            !service.exists('bar/foo.txt')
    }

    void 'delete bucket'() {
        when:
            service.deleteBucket()
        then:
            !service.listBucketNames().contains(MY_BUCKET)
    }

}
