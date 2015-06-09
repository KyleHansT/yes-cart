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

package org.yes.cart.service.domain;

import org.yes.cart.service.order.OrderException;

import java.util.Map;

/**
 * User: denispavlov
 * Date: 04/06/2015
 * Time: 13:45
 */
public interface CustomerOrderTransitionService {

    /**
     * Fire order transition in a separate transaction.
     *
     * @param event event name
     * @param orderNumber order number
     * @param deliveryNumber delivery number (only for delivery events)
     * @param params additional parameters
     *
     * @return handled flag
     *
     * @throws org.yes.cart.service.order.OrderException in case of transition failures
     */
    boolean transitionOrder(String event, String orderNumber, String deliveryNumber, Map params) throws OrderException;

}