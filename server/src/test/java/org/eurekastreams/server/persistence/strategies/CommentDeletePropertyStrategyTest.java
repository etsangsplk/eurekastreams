/*
 * Copyright (c) 2009-2010 Lockheed Martin Corporation
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
package org.eurekastreams.server.persistence.strategies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eurekastreams.server.domain.EntityType;
import org.eurekastreams.server.domain.stream.ActivityDTO;
import org.eurekastreams.server.domain.stream.StreamEntityDTO;
import org.eurekastreams.server.persistence.mappers.DomainMapper;
import org.eurekastreams.server.persistence.mappers.GetAllPersonIdsWhoHaveGroupCoordinatorAccess;
import org.eurekastreams.server.persistence.mappers.stream.GetDomainGroupsByShortNames;
import org.eurekastreams.server.search.modelview.CommentDTO;
import org.eurekastreams.server.search.modelview.PersonModelView;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for CommentDeletePropertyStrategy.
 * 
 */
@SuppressWarnings("serial")
public class CommentDeletePropertyStrategyTest
{
    /**
     * Mocking context.
     */
    private final JUnit4Mockery context = new JUnit4Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /**
     * Mapper to get a person's id by their account id.
     */
    private DomainMapper<String, Long> getPersonIdByAccountIdMapper = context.mock(DomainMapper.class,
            "getPersonIdByAccountIdMapper");

    /**
     * DAO for looking up group by short name.
     */
    private GetDomainGroupsByShortNames groupByShortNameDAO = context.mock(GetDomainGroupsByShortNames.class);

    /**
     * mapper to get the system admin ids.
     */
    private DomainMapper<Serializable, List<Long>> adminIdsMapper = context.mock(DomainMapper.class, "adminIdsMapper");

    /**
     * StreamEntityDTO.
     */
    private StreamEntityDTO activityDestinationStream = context.mock(StreamEntityDTO.class);

    /**
     * PersonModelView representing destination stream.
     */
    private PersonModelView activityDestinationPersonModelView = context.mock(PersonModelView.class);

    /**
     * ActivityDTO mock.
     */
    private ActivityDTO parentActivity = context.mock(ActivityDTO.class);

    /**
     * user id.
     */
    private final long userPersonId = 99L;

    /**
     * group id.
     */
    private final long groupId = 38982L;

    /**
     * user acctounId.
     */
    private String userAcctId = "smithers";

    /**
     * CommentDTO mock.
     */
    private CommentDTO comment = context.mock(CommentDTO.class);

    /**
     * List of commentDTO.
     */
    private ArrayList<CommentDTO> comments = new ArrayList<CommentDTO>()
    {
        {
            add(comment);
        }
    };

    /**
     * Group short name.
     */
    private String groupShortName = "groupShortName";

    /**
     * Mapper to check if the user has coordinator access to a group.
     */
    private GetAllPersonIdsWhoHaveGroupCoordinatorAccess groupAccessMapper = context
            .mock(GetAllPersonIdsWhoHaveGroupCoordinatorAccess.class);

    /**
     * System under test.
     */
    private CommentDeletePropertyStrategy sut;

    /**
     * Setup.
     */
    @Before
    public void setup()
    {
        sut = new CommentDeletePropertyStrategy(getPersonIdByAccountIdMapper, groupByShortNameDAO, groupAccessMapper,
                adminIdsMapper);
    }

    /**
     * Test execute with user who can delete parent activity.
     */
    @Test
    public void testExecuteUserCanDeleteParent()
    {
        context.checking(new Expectations()
        {
            {
                oneOf(parentActivity).isDeletable();
                will(returnValue(true));

                oneOf(comment).setDeletable(true);
            }
        });

        sut.execute(userAcctId, parentActivity, comments);
        context.assertIsSatisfied();
    }

    /**
     * Test execute with user as stream owner.
     */
    @Test
    public void testExecuteUserIsStreamOwner()
    {
        context.checking(new Expectations()
        {
            {
                allowing(parentActivity).getDestinationStream();
                will(returnValue(activityDestinationStream));

                oneOf(activityDestinationStream).getType();
                will(returnValue(EntityType.PERSON));

                oneOf(activityDestinationStream).getUniqueIdentifier();
                will(returnValue(userAcctId));

                oneOf(parentActivity).isDeletable();
                will(returnValue(false));

                oneOf(comment).setDeletable(true);
            }
        });

        sut.execute(userAcctId, parentActivity, comments);
        context.assertIsSatisfied();
    }

    /**
     * Test execute with user as destination stream group Coordinator.
     */
    @Test
    public void testExecuteUserActivityGroupCoordinatorRole()
    {
        context.checking(new Expectations()
        {
            {
                allowing(parentActivity).getDestinationStream();
                will(returnValue(activityDestinationStream));

                allowing(activityDestinationStream).getType();
                will(returnValue(EntityType.GROUP));

                oneOf(getPersonIdByAccountIdMapper).execute(userAcctId);
                will(returnValue(userPersonId));

                oneOf(activityDestinationStream).getUniqueIdentifier();
                will(returnValue(groupShortName));

                oneOf(groupByShortNameDAO).fetchId(groupShortName);
                will(returnValue(groupId));

                oneOf(parentActivity).isDeletable();
                will(returnValue(false));

                oneOf(groupAccessMapper).hasGroupCoordinatorAccessRecursively(userPersonId, groupId);
                will(returnValue(true));

                oneOf(comment).setDeletable(true);
            }
        });

        sut.execute(userAcctId, parentActivity, comments);
        context.assertIsSatisfied();
    }

    /**
     * Test execute with user as comment author.
     */
    @Test
    public void testExecuteUserIsCommentAuthorOnly()
    {
        final List<Long> adminIds = new ArrayList<Long>();

        context.checking(new Expectations()
        {
            {
                allowing(parentActivity).getDestinationStream();
                will(returnValue(activityDestinationStream));

                allowing(activityDestinationStream).getType();
                will(returnValue(EntityType.PERSON));

                allowing(activityDestinationStream).getUniqueIdentifier();
                will(returnValue("notSmithers"));

                oneOf(adminIdsMapper).execute(null);
                will(returnValue(adminIds));

                // user is the comment author
                allowing(getPersonIdByAccountIdMapper).execute(userAcctId);
                will(returnValue(userPersonId));

                oneOf(comment).getAuthorId();
                will(returnValue(userPersonId));

                oneOf(parentActivity).isDeletable();
                will(returnValue(false));

                oneOf(comment).setDeletable(true);
            }
        });

        sut.execute(userAcctId, parentActivity, comments);
        context.assertIsSatisfied();
    }

    /**
     * Test execute with user as system admin.
     */
    @Test
    public void testExecuteUserIsParentOrgCoord()
    {
        final List<Long> admins = new ArrayList<Long>();
        admins.add(userPersonId);
        context.checking(new Expectations()
        {
            {
                allowing(parentActivity).getDestinationStream();
                will(returnValue(activityDestinationStream));

                allowing(activityDestinationStream).getType();
                will(returnValue(EntityType.PERSON));

                allowing(activityDestinationStream).getUniqueIdentifier();
                will(returnValue("notSmithers"));

                oneOf(adminIdsMapper).execute(null);
                will(returnValue(admins));

                allowing(getPersonIdByAccountIdMapper).execute(userAcctId);
                will(returnValue(userPersonId));

                oneOf(parentActivity).isDeletable();
                will(returnValue(false));

                oneOf(comment).setDeletable(true);
            }
        });

        sut.execute(userAcctId, parentActivity, comments);
        context.assertIsSatisfied();
    }

    /**
     * Test execute with user as non of the above.
     */
    @Test
    public void testExecuteUserIsNobody()
    {
        final List<Long> admins = new ArrayList<Long>();
        context.checking(new Expectations()
        {
            {
                allowing(parentActivity).getDestinationStream();
                will(returnValue(activityDestinationStream));

                allowing(activityDestinationStream).getType();
                will(returnValue(EntityType.PERSON));

                allowing(activityDestinationStream).getUniqueIdentifier();
                will(returnValue("notSmithers"));

                oneOf(adminIdsMapper).execute(null);
                will(returnValue(admins));

                // user is not the comment author
                allowing(getPersonIdByAccountIdMapper).execute(userAcctId);
                will(returnValue(userPersonId));

                oneOf(comment).getAuthorId();
                will(returnValue(1L));

                oneOf(parentActivity).isDeletable();
                will(returnValue(false));

                oneOf(comment).setDeletable(false);
            }
        });

        sut.execute(userAcctId, parentActivity, comments);
        context.assertIsSatisfied();
    }

    /**
     * Test execute with user is null.
     */
    @Test
    public void testExecuteUserIsNull()
    {
        context.checking(new Expectations()
        {
            {
                oneOf(comment).setDeletable(false);
            }
        });

        sut.execute(null, parentActivity, comments);
        context.assertIsSatisfied();
    }
}
