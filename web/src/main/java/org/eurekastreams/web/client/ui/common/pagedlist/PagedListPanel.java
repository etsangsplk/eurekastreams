/*
 * Copyright (c) 2010 Lockheed Martin Corporation
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
package org.eurekastreams.web.client.ui.common.pagedlist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eurekastreams.server.action.request.PageableRequest;
import org.eurekastreams.server.domain.PagedSet;
import org.eurekastreams.web.client.events.Observer;
import org.eurekastreams.web.client.events.PagerUpdatedEvent;
import org.eurekastreams.web.client.events.SwitchToFilterOnPagedFilterPanelEvent;
import org.eurekastreams.web.client.events.UpdateHistoryEvent;
import org.eurekastreams.web.client.events.UpdatedHistoryParametersEvent;
import org.eurekastreams.web.client.history.CreateUrlRequest;
import org.eurekastreams.web.client.model.Fetchable;
import org.eurekastreams.web.client.ui.Session;
import org.eurekastreams.web.client.ui.common.Pager;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * This is a fairly complex control. Basically, it supports a series of "filters" (which can be though of as data sets.
 * These's data sets can be sorted and/or paged. Add sets by feeding it Fetchable models and a renderer for the
 * individual items, and this control should take care of all the logic.
 * 
 */
public class PagedListPanel extends FlowPanel
{
    /**
     * List id (so we can have >1 on a page).
     */
    private String listId = "list";
    /**
     * The renderers keyed by filter.
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, ItemRenderer> renderers = new HashMap<String, ItemRenderer>();
    /**
     * Requests keyed by filter.
     */
    private HashMap<String, HashMap<String, PageableRequest>> requests = // \n
    new HashMap<String, HashMap<String, PageableRequest>>();

    /**
     * Links keyed by filter.
     */
    private HashMap<String, Anchor> filterLinks = new HashMap<String, Anchor>();
    /**
     * Fetchers keyed by filter.
     */
    @SuppressWarnings("unchecked")
    private HashMap<String, Fetchable> fetchers = new HashMap<String, Fetchable>();
    /**
     * Sorters keyed by filter.
     */
    private HashMap<String, FlowPanel> sortPanels = new HashMap<String, FlowPanel>();
    /**
     * Sort Links keyed by filter and sort.
     */
    private HashMap<String, HashMap<String, Anchor>> sortLinks = new HashMap<String, HashMap<String, Anchor>>();

    /**
     * Collection of filters that have been loaded and the available sorts for each filter.
     */
    private HashMap<String, List<String>> loadedFilters = new HashMap<String, List<String>>();

    /**
     * Filter to go to.
     */
    private String jumpToFilter;

    /**
     * Sort to go to.
     */
    private String jumpToSort;

    /**
     * Pager item start index to go to.
     */
    private Integer jumpToStart;

    /**
     * Pager item end index to go to.
     */
    private Integer jumpToEnd;

    /**
     * Flag indicating processing of initial panel view.
     */
    private boolean processingInitial = false;

    /**
     * Flag indicating processing of a browser back button click.
     */
    private boolean processingBack = false;

    /**
     * Flag indicating processing of initial panel view initiated from a browser back button click.
     */
    private boolean processingDefaultOnBack = false;

    /**
     * Contains the items.
     */
    private FlowPanel renderContainer = new FlowPanel();
    /**
     * Contains the filter switchers.
     */
    private FlowPanel filterContainer = new FlowPanel();
    /**
     * Contains the sort switchers.
     */
    private FlowPanel sortContainer = new FlowPanel();

    /**
     * Waiting spinner.
     */
    FlowPanel waitSpinner = new FlowPanel();

    /**
     * Whether or not this has been initiated with a filter. The first filter will initiate it.
     */
    private boolean initialized = false;

    /**
     * Start index.
     */
    private Integer startIndex = null;
    /**
     * End index.
     */
    private Integer endIndex = null;

    /**
     * The current filter.
     */
    private String currentFilter = "";
    /**
     * The current sort.
     */
    private String currentSortKey = "";

    /**
     * The bottom pager. Pass in true to show the buttons.
     */
    private Pager bottomPager;

    /**
     * Used to lay out the page; default is two columns.
     */
    private PagedListRenderer pageRenderer = new TwoColumnPagedListRenderer();

    /**
     * Navigation panel.
     */
    private FlowPanel navPanel;

    /**
     * Default constructor.
     * 
     * @param inListId
     *            the list id.
     */
    public PagedListPanel(final String inListId)
    {
        listId = inListId;
        bottomPager = new Pager("filteredPager" + listId, true);

        waitSpinner.addStyleName("wait-spinner");

        this.addStyleName("connection-master");
        filterContainer.add(new Label("View:"));

        navPanel = new FlowPanel();
        navPanel.addStyleName("navpanel");
        navPanel.add(filterContainer);
        navPanel.add(sortContainer);
        this.add(navPanel);

        filterContainer.addStyleName("options");
        filterContainer.addStyleName("views");
        sortContainer.addStyleName("options");
        bottomPager.addStyleName("bottom-pager");

        this.add(waitSpinner);
        this.add(renderContainer);

        Session.getInstance().getEventBus().addObserver(PagerUpdatedEvent.class, new Observer<PagerUpdatedEvent>()
        {
            public void update(final PagerUpdatedEvent event)
            {
                if (event.getPager().getPagerId().equals("filteredPager" + listId)
                        && event.getPager().getStartIndex() != startIndex)
                {
                    startIndex = event.getPager().getStartIndex();
                    endIndex = event.getPager().getEndIndex();
                    reload();
                }
            }
        });

        Session.getInstance().getEventBus().addObserver(SwitchToFilterOnPagedFilterPanelEvent.class,
                new Observer<SwitchToFilterOnPagedFilterPanelEvent>()
                {
                    public void update(final SwitchToFilterOnPagedFilterPanelEvent event)
                    {
                        if (event.getListId().equals(listId))
                        {
                            currentFilter = event.getFilterName();
                            currentSortKey = event.getSortKey();
                            startIndex = null;

                            // TODO - put pager history handling into Pager class

                            // reset the pager settings if the view or sort has changed
                            if (hasSortOrFilterChanged())
                            {
                                jumpToStart = null;
                                jumpToEnd = null;
                            }

                            if (jumpToStart == null)
                            {
                                bottomPager.reset();
                            }
                            else
                            {
                                bottomPager.setStartIndex(jumpToStart);
                                bottomPager.setEndIndex(jumpToEnd);
                                Session.getInstance().getEventBus().notifyObservers(new PagerUpdatedEvent(bottomPager));
                            }

                            if (!processingDefaultOnBack && (!processingInitial || processingBack)
                                    && hasSortOrFilterChanged())
                            {
                                HashMap<String, String> params = new HashMap<String, String>();
                                params.put("name", currentFilter);
                                params.put("sort", currentSortKey);
                                params.put("listId", listId);

                                if (jumpToStart == null)
                                {
                                    params.put("startIndex", "0");
                                }
                                if (jumpToEnd == null)
                                {
                                    params.put("endIndex", "9");
                                }

                                Session.getInstance().getEventBus().notifyObservers(
                                        new UpdateHistoryEvent(new CreateUrlRequest(params, false)));
                            }
                            processingInitial = false;
                            processingDefaultOnBack = false;
                        }
                    }
                });

        Session.getInstance().getEventBus().addObserver(UpdatedHistoryParametersEvent.class,
                new Observer<UpdatedHistoryParametersEvent>()
                {
                    public void update(final UpdatedHistoryParametersEvent event)
                    {
                        processingDefaultOnBack = false;
                        String thisListId = event.getParameters().get("listId");

                        // handle returning back to the default view (no history tokens)
                        if (processingBack && !thisListId.equals(listId))
                        {
                            String defaultFilter = (String) loadedFilters.keySet().toArray()[0];
                            String defaultSort = loadedFilters.get(defaultFilter).get(0);
                            processingDefaultOnBack = true;
                            Session.getInstance().getEventBus().notifyObservers(
                                    new SwitchToFilterOnPagedFilterPanelEvent(listId, defaultFilter, defaultSort));
                        }
                        else if (thisListId.equals(listId))
                        {
                            jumpToFilter = event.getParameters().get("name");
                            jumpToSort = event.getParameters().get("sort");
                            if (jumpToSort == null)
                            {
                                jumpToSort = "";
                            }

                            jumpToStart = event.getParameters().get("startIndex") == null ? null : new Integer(event
                                    .getParameters().get("startIndex"));
                            jumpToEnd = event.getParameters().get("endIndex") == null ? null : new Integer(event
                                    .getParameters().get("endIndex"));

                            if (initialized)
                            {
                                Session.getInstance().getEventBus().notifyObservers(
                                        new SwitchToFilterOnPagedFilterPanelEvent(listId, jumpToFilter, jumpToSort));
                                processingBack = true;
                            }
                        }
                    }
                }, true);

        this.add(bottomPager);

        FlowPanel clear = new FlowPanel();
        clear.addStyleName("clear");
        this.add(clear);
    }

    /**
     * Helper method to isolate complex logic determining if filter or sort has been updated.
     * 
     * @return true if either sort or filter history tokens have been changed.
     */
    private boolean hasSortOrFilterChanged()
    {
        String nameFromUrl = Session.getInstance().getParameterValue("name");
        String sortFromUrl = Session.getInstance().getParameterValue("sort");
        return (nameFromUrl != "undefined" && !nameFromUrl.equals(currentFilter))
                || (currentSortKey != "" && !sortFromUrl.equals(currentSortKey));
    }

    /**
     * Constructor.
     * 
     * @param inListId
     *            the list id.
     * @param inPageRenderer
     *            page layout renderer.
     */
    public PagedListPanel(final String inListId, final PagedListRenderer inPageRenderer)
    {
        this(inListId);
        pageRenderer = inPageRenderer;
    }

    /**
     * Reload the panel to a default filter.
     * 
     * @param filter
     *            The default filter to use after reset.
     */
    public void reload(final String filter)
    {
        currentFilter = filter;
        reload();
    }

    /**
     * Reload the panel.
     */
    public void reload()
    {

        for (Anchor filterLink : filterLinks.values())
        {
            filterLink.removeStyleName("active");
        }

        if (sortLinks.get(currentFilter) != null)
        {
            for (Anchor sortLink : sortLinks.get(currentFilter).values())
            {
                sortLink.removeStyleName("active");
            }

            if (sortLinks.get(currentFilter).get(currentSortKey) != null)
            {
                sortLinks.get(currentFilter).get(currentSortKey).addStyleName("active");
            }
        }

        if (sortPanels.get(currentFilter) != null)
        {
            sortContainer.clear();
            sortContainer.add(sortPanels.get(currentFilter));
        }
        else
        {
            sortContainer.clear();
        }

        filterLinks.get(currentFilter).addStyleName("active");

        refreshData();
    }

    /**
     * Causes the data for the current filter and sort to be refreshed (via fetching from the model).
     */
    @SuppressWarnings("unchecked")
    public void refreshData()
    {
        waitSpinner.setVisible(true);
        PageableRequest request = requests.get(currentFilter).get(currentSortKey);
        request.setStartIndex(startIndex);
        request.setEndIndex(endIndex);
        renderContainer.addStyleName("hidden");
        fetchers.get(currentFilter).fetch(request, false);
    }

    /**
     * Add a filter w/o a sort.
     * 
     * @param name
     *            name of the filter.
     * @param fetchable
     *            the fetchable model.
     * @param renderer
     *            the renderer.
     * @param request
     *            the request.
     */
    @SuppressWarnings("unchecked")
    public void addSet(final String name, final Fetchable fetchable, final ItemRenderer renderer,
            final PageableRequest request)
    {
        addSet(name, fetchable, renderer, request, "");
    }

    /**
     * Add a filter w/o a sort.
     * 
     * @param name
     *            name of the filter.
     * @param fetchable
     *            the fetchable model.
     * @param renderer
     *            the renderer.
     * @param request
     *            the request.
     * @param sortKey
     *            the sort key.
     */
    @SuppressWarnings("unchecked")
    public void addSet(final String name, final Fetchable fetchable, final ItemRenderer renderer,
            final PageableRequest request, final String sortKey)
    {
        if (requests.get(name) == null)
        {
            requests.put(name, new HashMap<String, PageableRequest>());

            renderers.put(name, renderer);
            fetchers.put(name, fetchable);

            Anchor filterLink = new Anchor(name);
            filterLink.addStyleName("connection-filter-button");

            filterLink.addClickHandler(new ClickHandler()
            {
                public void onClick(final ClickEvent event)
                {
                    Session.getInstance().getEventBus().notifyObservers(
                            new SwitchToFilterOnPagedFilterPanelEvent(listId, name, sortKey));
                }
            });

            filterLinks.put(name, filterLink);
            filterContainer.add(filterLink);
            sortLinks.put(name, new HashMap<String, Anchor>());
        }

        requests.get(name).put(sortKey, request);

        if (!sortKey.equals(""))
        {
            if (sortPanels.get(name) == null)
            {
                FlowPanel sortPanel = new FlowPanel();
                sortPanel.add(new Label("Sort: "));
                sortPanels.put(name, sortPanel);
            }

            Anchor sortLink = new Anchor(sortKey);
            sortLink.addStyleName("connection-filter-button");
            sortLink.addClickHandler(new ClickHandler()
            {
                public void onClick(final ClickEvent event)
                {
                    Session.getInstance().getEventBus().notifyObservers(
                            new SwitchToFilterOnPagedFilterPanelEvent(listId, name, sortKey));
                }
            });
            sortPanels.get(name).add(sortLink);
            sortLinks.get(name).put(sortKey, sortLink);
        }

        if (loadedFilters.containsKey(name))
        {
            loadedFilters.get(name).add(sortKey);
        }
        else
        {
            List sorts = new ArrayList();
            sorts.add(sortKey);
            loadedFilters.put(name, sorts);
        }

        // default case with no history detection
        if (!initialized && jumpToFilter == null)
        {
            initialized = true;
            processingInitial = true;
            Session.getInstance().getEventBus().notifyObservers(
                    new SwitchToFilterOnPagedFilterPanelEvent(listId, name, sortKey));
        }
        // case where back button was just pressed (history listener has already set jumpToFilter/jumpToSort)
        else if (!initialized && loadedFilters.containsKey(jumpToFilter)
                && loadedFilters.get(jumpToFilter).contains(jumpToSort))
        {
            initialized = true;
            processingBack = true;
            Session.getInstance().getEventBus().notifyObservers(
                    new SwitchToFilterOnPagedFilterPanelEvent(listId, jumpToFilter, jumpToSort));
        }
    }

    /**
     * Updates the request for a given set that has no sort key.
     * 
     * @param name
     *            The name of the set to update.
     * @param request
     *            The updated request.
     */
    public void updateSetRequest(final String name, final PageableRequest request)
    {
        updateSetRequest(name, request, "");
    }

    /**
     * Updates the request for a given set.
     * 
     * @param name
     *            The name of the set to update.
     * @param request
     *            The updated request.
     * @param sortKey
     *            The sort key of the set to update.
     */
    public void updateSetRequest(final String name, final PageableRequest request, final String sortKey)
    {
        if (requests.get(name) != null)
        {
            requests.get(name).remove(sortKey);
            requests.get(name).put(sortKey, request);
        }
    }

    /**
     * Render the panel.
     * 
     * @param <T>
     *            the type of item.
     * @param items
     *            the items.
     * @param noItemsMessage
     *            the message to display when nothing is there.
     */
    @SuppressWarnings("unchecked")
    public <T extends Serializable> void render(final PagedSet<T> items, final String noItemsMessage)
    {
        ItemRenderer render = renderers.get(currentFilter);
        renderContainer.clear();

        if (items.getTotal() > 0)
        {
            this.removeStyleName("empty-list");
        }
        else
        {

            this.addStyleName("empty-list");
        }
        pageRenderer.render(renderContainer, render, items, noItemsMessage);
        renderContainer.removeStyleName("hidden");

        bottomPager.setTotal(items.getTotal());
        waitSpinner.setVisible(false);
    }

    /**
     * @return The current filter being displayed.
     */
    public String getCurrentFilter()
    {
        return currentFilter;
    }

    /**
     * Sets the text displayed on the filter link.
     * 
     * @param name
     *            The name of the filter.
     * @param title
     *            The text to display.
     */
    public void setFilterTitle(final String name, final String title)
    {
        filterLinks.get(name).setText(title);
    }

}
