/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.integration;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.junit.Assert;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;

public class SemanticTextFieldIT extends PyTorchModelRestTestCase {

    private static final String BASE_64_ENCODED_MODEL = "UEsDBAAACAgAAAAAAAAAAAAAAAAAA"
        + "AAAAAAUAA4Ac2ltcGxlbW9kZWwvZGF0YS5wa2xGQgoAWlpaWlpaWlpaWoACY19fdG9yY2hfXwpUaW55VG"
        + "V4dEV4cGFuc2lvbgpxACmBfShYCAAAAHRyYWluaW5ncQGJWBYAAABfaXNfZnVsbF9iYWNrd2FyZF9ob29"
        + "rcQJOdWJxAy5QSwcIITmbsFgAAABYAAAAUEsDBBQACAgIAAAAAAAAAAAAAAAAAAAAAAAdAB0Ac2ltcGxl"
        + "bW9kZWwvY29kZS9fX3RvcmNoX18ucHlGQhkAWlpaWlpaWlpaWlpaWlpaWlpaWlpaWlpaWoWRT4+cMAzF7"
        + "/spfASJomF3e0Ga3nrrn8vcELIyxAzRhAQlpjvbT19DWDrdquqBA/bvPT87nVUxwsm41xPd+PNtUi4a77"
        + "KvXs+W8voBAHFSQY3EFCIiHKFp1+p57vs/ShyUccZdoIaz93aBTMR+thbPqru+qKBx8P4q/e8TyxRlmwVc"
        + "tJp66H1YmCyS7WsZwD50A2L5V7pCBADGTTOj0bGGE7noQyqzv5JDfp0o9fZRCWqP37yjhE4+mqX5X3AdF"
        + "ZHGM/2TzOHDpy1IvQWR+OWo3KwsRiKdpcqg4pBFDtm+QJ7nqwIPckrlnGfFJG0uNhOl38Sjut3pCqg26Qu"
        + "Zy8BR9In7ScHHrKkKMW0TIucFrGQXCMpdaDO05O6DpOiy8e4kr0Ed/2YKOIhplW8gPr4ntygrd9ixpx3j9"
        + "UZZVRagl2c6+imWUzBjuf5m+Ch7afphuvvW+r/0dsfn+2N9MZGb9+/SFtCYdhd83CMYp+mGy0LiKNs8y/e"
        + "UuEA8B/d2z4dfUEsHCFSE3IaCAQAAIAMAAFBLAwQUAAgICAAAAAAAAAAAAAAAAAAAAAAAJwApAHNpbXBsZ"
        + "W1vZGVsL2NvZGUvX190b3JjaF9fLnB5LmRlYnVnX3BrbEZCJQBaWlpaWlpaWlpaWlpaWlpaWlpaWlpaWlp"
        + "aWlpaWlpaWlpaWlpahZHLbtNAFIZtp03rSVIuLRKXjdk5ojitKJsiFq24lem0KKSqpRIZt55gE9/GM+lNL"
        + "Fgx4i1Ys2aHhIBXgAVICNggHgNm6rqJN2BZGv36/v/MOWeea/Z5RVHurLfRUsfZXOnccx522itrd53O0vL"
        + "qbaKYtsAKUe1pcege7hm9JNtzM8+kOOzNApIX0A3xBXE6YE7g0UWjg2OaZAJXbKvALOnj2GEHKc496ykLkt"
        + "gNt3Jz17hprCUxFqExe7YIpQkNpO1/kfHhPUdtUAdH2/gfmeYiIFW7IkM6IBP2wrDNbMe3Mjf2ksiK3Hjg"
        + "hg7F2DN9l/omZZl5Mmez2QRk0q4WUUB0+1oh9nDwxGdUXJdXPMRZQs352eGaRPV9s2lcMeZFGWBfKJJiw0Y"
        + "gbCMLBaRmXyy4flx6a667Fch55q05QOq2Jg2ANOyZwplhNsjiohVApo7aa21QnNGW5+4GXv8gxK1beBeHSR"
        + "rhmLXWVh+0aBhErZ7bx1ejxMOhlR6QU4ycNqGyk8/yNGCWkwY7/RCD7UEQek4QszCgDJAzZtfErA0VqHBy9"
        + "ugQP9pUfUmgCjVYgWNwHFbhBJyEOgSwBuuwARWZmoI6J9PwLfzEocpRpPrT8DP8wqHG0b4UX+E3DiscvRgl"
        + "XIoi81KKPwioHI5x9EooNKWiy0KOc/T6WF4SssrRuzJ9L2VNRXUhJzj6UKYfS4W/q/5wuh/l4M9R9qsU+y2"
        + "dpoo2hJzkaEET8r6KRONicnRdK9EbUi6raFVIwNGjsrlbpk6ZPi7TbS3fv3LyNjPiEKzG0aG0tvNb6xw90/"
        + "whe6ONjnJcUxobHDUqQ8bIOW79BVBLBwhfSmPKdAIAAE4EAABQSwMEAAAICAAAAAAAAAAAAAAAAAAAAAAAA"
        + "BkABQBzaW1wbGVtb2RlbC9jb25zdGFudHMucGtsRkIBAFqAAikuUEsHCG0vCVcEAAAABAAAAFBLAwQAAAgI"
        + "AAAAAAAAAAAAAAAAAAAAAAAAEwA7AHNpbXBsZW1vZGVsL3ZlcnNpb25GQjcAWlpaWlpaWlpaWlpaWlpaWlp"
        + "aWlpaWlpaWlpaWlpaWlpaWlpaWlpaWlpaWlpaWlpaWlpaWlpaWjMKUEsHCNGeZ1UCAAAAAgAAAFBLAQIAAA"
        + "AACAgAAAAAAAAhOZuwWAAAAFgAAAAUAAAAAAAAAAAAAAAAAAAAAABzaW1wbGVtb2RlbC9kYXRhLnBrbFBLA"
        + "QIAABQACAgIAAAAAABUhNyGggEAACADAAAdAAAAAAAAAAAAAAAAAKgAAABzaW1wbGVtb2RlbC9jb2RlL19f"
        + "dG9yY2hfXy5weVBLAQIAABQACAgIAAAAAABfSmPKdAIAAE4EAAAnAAAAAAAAAAAAAAAAAJICAABzaW1wbGVt"
        + "b2RlbC9jb2RlL19fdG9yY2hfXy5weS5kZWJ1Z19wa2xQSwECAAAAAAgIAAAAAAAAbS8JVwQAAAAEAAAAGQAA"
        + "AAAAAAAAAAAAAACEBQAAc2ltcGxlbW9kZWwvY29uc3RhbnRzLnBrbFBLAQIAAAAACAgAAAAAAADRnmdVAgAA"
        + "AAIAAAATAAAAAAAAAAAAAAAAANQFAABzaW1wbGVtb2RlbC92ZXJzaW9uUEsGBiwAAAAAAAAAHgMtAAAAAAAA"
        + "AAAABQAAAAAAAAAFAAAAAAAAAGoBAAAAAAAAUgYAAAAAAABQSwYHAAAAALwHAAAAAAAAAQAAAFBLBQYAAAAABQAFAGoBAABSBgAAAAA=";

    private static final long RAW_MODEL_SIZE = Base64.getDecoder().decode(BASE_64_ENCODED_MODEL).length;
    public static final List<String> VOCABULARY = List.of(
        "these",
        "are",
        "my",
        "words",
        "the",
        "washing",
        "machine",
        "is",
        "leaking",
        "octopus",
        "comforter",
        "smells"
    );

    public void testSemanticTextInference() throws IOException {
        String modelId = "semantic-text-model";

        createTextExpansionModel(modelId);
        putModelDefinition(modelId, BASE_64_ENCODED_MODEL, RAW_MODEL_SIZE);
        putVocabulary(
            VOCABULARY,
            modelId
        );
        startDeployment(modelId);

        String indexName = modelId + "-index";
        createSemanticTextIndex(indexName);

        int numDocs = ESTestCase.randomIntBetween(1, 10);
        bulkIndexDocs(indexName, numDocs);

        for (int i = 0; i < numDocs; i++) {
            Request getRequest = new Request("GET", "/" + indexName + "/_doc/" + i);
            Response response = ESRestTestCase.client().performRequest(getRequest);
            Assert.assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        }
    }

    private void createTextExpansionModel(String modelId) throws IOException {
        Request request = new Request("PUT", "/_ml/trained_models/" + modelId);
        request.setJsonEntity("""
            {
               "description": "a text expansion model",
               "model_type": "pytorch",
               "inference_config": {
                 "text_expansion": {
                   "tokenization": {
                     "bert": {
                       "with_special_tokens": false
                     }
                   }
                 }
               }
             }""");
        ESRestTestCase.client().performRequest(request);
    }

    private void createSemanticTextIndex(String indexName) throws IOException {
        Request createIndex = new Request("PUT", "/" + indexName);
        createIndex.setJsonEntity("""
              {
                "mappings": {
                  "properties": {
                    "text_field": {
                      "type": "text"
                    },
                    "inference_field": {
                        "type": "semantic_text",
                        "model_id": "semantic-text-model"
                    }
                  }
                }
              }""");
        var response = ESRestTestCase.client().performRequest(createIndex);
        assertOkWithErrorMessage(response);
    }

    private void bulkIndexDocs(String indexName, int numDocs) throws IOException {

        StringBuilder bulkBuilder = new StringBuilder();

        for (int i = 0; i < numDocs; i++) {
            String createAction = "{\"create\": {\"_index\": \"" + indexName + "\" \"_id\":\"" + i + "\"}}\n";
            bulkBuilder.append(createAction);
            bulkBuilder.append("{\"text_field\": \"").append(ESTestCase.randomAlphaOfLengthBetween(1, 100)).append("\",");

            bulkBuilder.append("{\"inference_field\": \"");
            bulkBuilder.append(String.join(" ", ESTestCase.randomSubsetOf(ESTestCase.randomIntBetween(1, 10), VOCABULARY)));
            bulkBuilder.append("\"");

            bulkBuilder.append("}}\n");
        }

        Request bulkRequest = new Request("POST", "/_bulk");

        bulkRequest.setJsonEntity(bulkBuilder.toString());
        bulkRequest.addParameter("refresh", "true");
        var bulkResponse = ESRestTestCase.client().performRequest(bulkRequest);
        assertOkWithErrorMessage(bulkResponse);
    }

}
