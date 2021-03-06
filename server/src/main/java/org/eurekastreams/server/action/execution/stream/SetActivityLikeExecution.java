/*
 * Copyright (c) 2010-2011 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.server.action.execution.stream;

import java.io.Serializable;

import org.eurekastreams.commons.actions.TaskHandlerExecutionStrategy;
import org.eurekastreams.commons.actions.context.PrincipalActionContext;
import org.eurekastreams.commons.actions.context.TaskHandlerActionContext;
import org.eurekastreams.commons.exceptions.ExecutionException;
import org.eurekastreams.commons.server.UserActionRequest;
import org.eurekastreams.server.action.request.notification.ActivityNotificationsRequest;
import org.eurekastreams.server.action.request.notification.CreateNotificationsRequest;
import org.eurekastreams.server.action.request.notification.CreateNotificationsRequest.RequestType;
import org.eurekastreams.server.action.request.stream.SetActivityLikeRequest;
import org.eurekastreams.server.action.request.stream.SetActivityLikeRequest.LikeActionType;
import org.eurekastreams.server.domain.stream.Activity;
import org.eurekastreams.server.domain.stream.LikedActivity;
import org.eurekastreams.server.persistence.mappers.DeleteLikedActivity;
import org.eurekastreams.server.persistence.mappers.FindByIdMapper;
import org.eurekastreams.server.persistence.mappers.IndexEntity;
import org.eurekastreams.server.persistence.mappers.InsertLikedActivity;
import org.eurekastreams.server.persistence.mappers.requests.FindByIdRequest;

/**
 * Action to add or remove like on activity for current user.
 */
public class SetActivityLikeExecution implements TaskHandlerExecutionStrategy<PrincipalActionContext>
{
    /**
     * Mapper for adding like.
     */
    private final InsertLikedActivity insertLikedActivity;

    /**
     * Mapper for removing like.
     */
    private final DeleteLikedActivity deleteLikedActivity;

    /**
     * The entity indexer.
     */
    private final IndexEntity<Activity> indexEntity;

    /**
     * Find Activity by ID mapper.
     */
    private final FindByIdMapper<Activity> activityEntityMapper;

    /**
     * Constructor.
     *
     * @param inInsertLikedActivity
     *            Mapper for liking an activity.
     * @param inDeleteLikedActivity
     *            Mapper for unliking an activity.
     * @param inIndexEntity
     *            the activity indexer.
     * @param inActivityEntityMapper
     *            activity entity mapper, used for indexing.
     */
    public SetActivityLikeExecution(final InsertLikedActivity inInsertLikedActivity,
            final DeleteLikedActivity inDeleteLikedActivity, final IndexEntity<Activity> inIndexEntity,
            final FindByIdMapper<Activity> inActivityEntityMapper)
    {
        insertLikedActivity = inInsertLikedActivity;
        deleteLikedActivity = inDeleteLikedActivity;
        indexEntity = inIndexEntity;
        activityEntityMapper = inActivityEntityMapper;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Serializable execute(final TaskHandlerActionContext<PrincipalActionContext> inActionContext)
            throws ExecutionException
    {
        SetActivityLikeRequest request = (SetActivityLikeRequest) inActionContext.getActionContext().getParams();
        final Long userId = inActionContext.getActionContext().getPrincipal().getId();
        LikedActivity likeActivityData = new LikedActivity(userId, request.getActivityId());

        if (request.getLikeActionType() == LikeActionType.ADD_LIKE)
        {
            insertLikedActivity.execute(likeActivityData);

            inActionContext.getUserActionRequests().add(
                    new UserActionRequest("loadLikedActivityIdsByUserId", null, userId));

            CreateNotificationsRequest notificationRequest = new ActivityNotificationsRequest(RequestType.LIKE,
                    userId, 0L, request.getActivityId());
            inActionContext.getUserActionRequests().add(
                    new UserActionRequest(CreateNotificationsRequest.ACTION_NAME, null, notificationRequest));
        }
        else
        {
            deleteLikedActivity.execute(likeActivityData);
        }

        indexEntity.execute(activityEntityMapper.execute(new FindByIdRequest("Activity", request.getActivityId())));

        return Boolean.TRUE;
    }
}
