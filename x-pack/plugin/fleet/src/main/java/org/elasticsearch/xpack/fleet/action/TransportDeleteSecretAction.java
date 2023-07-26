/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.fleet.action;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.client.internal.OriginSettingClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.transport.TransportService;

import static org.elasticsearch.xpack.core.ClientHelper.FLEET_ORIGIN;

public class TransportDeleteSecretAction extends HandledTransportAction<DeleteSecretRequest, DeleteSecretResponse> {

    private final Client client;

    @Inject
    public TransportDeleteSecretAction(TransportService transportService, ActionFilters actionFilters, Client client) {
        super(DeleteSecretAction.NAME, transportService, actionFilters, DeleteSecretRequest::new);
        this.client = new OriginSettingClient(client, FLEET_ORIGIN);
    }

    @Override
    protected void doExecute(Task task, DeleteSecretRequest request, ActionListener<DeleteSecretResponse> listener) {
        client.prepareDelete(".fleet-secrets", request.id())
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
            .execute(
                ActionListener.wrap(
                    deleteResponse -> listener.onResponse(new DeleteSecretResponse(deleteResponse.getResult() == Result.DELETED)),
                    e -> handleFailure(e, listener)
                )
            ); // TODO: check impl and failure handling
    }

    private void handleFailure(Exception e, ActionListener<DeleteSecretResponse> listener) {
        Throwable cause = ExceptionsHelper.unwrapCause(e);
        if (cause instanceof IndexNotFoundException) {
            listener.onResponse(new DeleteSecretResponse(false));
        } else {
            listener.onFailure(e);
        }
    }
}
