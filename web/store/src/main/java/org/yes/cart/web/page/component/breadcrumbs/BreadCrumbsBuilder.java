/*
 * Copyright 2009 Igor Azarnyi, Denys Pavlov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.yes.cart.web.page.component.breadcrumbs;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

/**
 * Bread crumbs builder produce category and
 * attributive filtered navigation breadcrumbs based on
 * web query string and context.
 * <p/>
 * <p/>
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 2011-May-17
 * Time: 9:50:51 AM
 */
public interface BreadCrumbsBuilder {



    /**
     * We have 2 kinds of breadcrumbs:
     * 1. category path, for example electronics -> phones -> ip phones
     * 2. attributive filters, for example ip phones [price range, brands, weight, ect]
     *
     * @param categoryId            current category id
     * @param pageParameters        current query string
     * @param allowedAttributeNames allowed attribute names for filtering including price, brand, search...
     * @param shopCategoryIds       all categoryIds, that belong to shop
     * @param namePrefixProvider    name prifix provider for price, brand, search..

     * @return list of crumbs
     */
    List<Crumb> getBreadCrumbs(
            long categoryId,
            PageParameters pageParameters,
            List<String> allowedAttributeNames,
            List<Long> shopCategoryIds,
            CrumbNamePrefixProvider namePrefixProvider);




}
