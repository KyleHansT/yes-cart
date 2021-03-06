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

package org.yes.cart.web.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.task.TaskExecutor;
import org.yes.cart.constants.AttributeNamesKeys;
import org.yes.cart.domain.entity.AttrValueShop;
import org.yes.cart.domain.entity.RegisteredPerson;
import org.yes.cart.domain.entity.Shop;
import org.yes.cart.domain.message.RegistrationMessage;
import org.yes.cart.domain.message.consumer.CustomerRegistrationMessageListener;
import org.yes.cart.domain.message.impl.RegistrationMessageImpl;
import org.yes.cart.service.domain.HashHelper;
import org.yes.cart.service.domain.MailService;
import org.yes.cart.service.domain.PassPhrazeGenerator;
import org.yes.cart.service.domain.aspect.impl.BaseNotificationAspect;
import org.yes.cart.service.mail.MailComposer;
import org.yes.cart.service.theme.ThemeService;
import org.yes.cart.shoppingcart.ShoppingCart;
import org.yes.cart.util.ShopCodeContext;
import org.yes.cart.web.application.ApplicationDirector;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Aspect responsible for send email in case of person registration.
 * <p/>
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 10/23/11
 * Time: 11:57 AM
 */
@Aspect
public class RegistrationAspect extends BaseNotificationAspect {

    private final HashHelper hashHelper;

    private final PassPhrazeGenerator phrazeGenerator;

    private final MailService mailService;

    private final MailComposer mailComposer;

    private final ThemeService themeService;


    /**
     * Construct customer aspect.
     *
     * @param taskExecutor    {@link org.springframework.core.task.TaskExecutor} to execute task.
     * @param phrazeGenerator {@link org.yes.cart.service.domain.PassPhrazeGenerator}
     * @param hashHelper      {@link org.yes.cart.service.domain.HashHelper}
     * @param themeService    theme service
     */
    public RegistrationAspect(
            final TaskExecutor taskExecutor,
            final PassPhrazeGenerator phrazeGenerator,
            final HashHelper hashHelper,
            final MailService mailService,
            final MailComposer mailComposer,
            final ThemeService themeService) {
        super(taskExecutor);

        this.hashHelper = hashHelper;
        this.phrazeGenerator = phrazeGenerator;
        this.mailService = mailService;
        this.mailComposer = mailComposer;


        this.themeService = themeService;
    }

    /**
     * Perform notification about person registration.
     *
     * @param pjp join point
     * @param newPerson in case if new person was created.
     * @return inherited return
     * @throws Throwable in case it was in underlying method
     */
    protected Object notifyInternal(final ProceedingJoinPoint pjp, final boolean newPerson) throws Throwable {
        final Object[] args = pjp.getArgs();

        final RegisteredPerson registeredPerson = (RegisteredPerson) args[0];
        final Shop shop = (Shop) args[1];


        final String generatedPassword;
        if (newPerson && registeredPerson.getPassword() != null) {
            generatedPassword = registeredPerson.getPassword();
        } else {
            generatedPassword = phrazeGenerator.getNextPassPhrase();
        }
        final String passwordHash = hashHelper.getHash(generatedPassword);
        registeredPerson.setPassword(passwordHash);

        final RegistrationMessage registrationMessage = new RegistrationMessageImpl();
        registrationMessage.setEmail(registeredPerson.getEmail());
        registrationMessage.setFirstname(registeredPerson.getFirstname());
        registrationMessage.setLastname(registeredPerson.getLastname());
        registrationMessage.setPassword(generatedPassword);
        final ShoppingCart cart = ApplicationDirector.getShoppingCart();
        if (cart != null) {
            registrationMessage.setLocale(cart.getCurrentLocale());
        }


        registrationMessage.setMailTemplatePathChain(themeService.getMailTemplateChainByShopId(shop.getShopId()));

        registrationMessage.setTemplateName(newPerson ? "customer-registered" : "customer-change-password");

        final AttrValueShop attrVal = shop.getAttributeByCode(AttributeNamesKeys.Shop.SHOP_ADMIN_EMAIL);
        String fromEmail = null;
        if (attrVal != null) {
            fromEmail = attrVal.getVal();
        }
        registrationMessage.setShopMailFrom(fromEmail);

        registrationMessage.setShopId(shop.getShopId());
        registrationMessage.setShopCode(shop.getCode());
        registrationMessage.setShopName(shop.getName());
        registrationMessage.setShopUrl(transformShopUrls(shop));

        sendNotification(registrationMessage);

        ShopCodeContext.getLog(this).info("Person message was send to queue {}", registrationMessage);

        return pjp.proceed();
    }

    /**
     * {@inheritDoc}
     */
    public Runnable getTask(Serializable serializableMessage) {
        return new CustomerRegistrationMessageListener(
                mailService,
                mailComposer,
                serializableMessage
        );
    }

    private Set<String> transformShopUrls(final Shop shop) {
        final Set<String> rez = new HashSet<String>();
        rez.add(shop.getDefaultShopUrl());
        return rez;
    }


    /**
     * Handle customer creation.
     *
     * @param pjp {@link org.aspectj.lang.ProceedingJoinPoint}
     * @return Object
     * @throws Throwable in case of target method errors
     */
    @Around("execution(* org.yes.cart.service.domain.impl.CustomerServiceImpl.create(..))")
    public Object doCreateCustomer(final ProceedingJoinPoint pjp) throws Throwable {
        return notifyInternal(pjp, true);
    }

    /**
     * Handle reset password operation.
     *
     * @param pjp {@link ProceedingJoinPoint}
     * @return Object
     * @throws Throwable in case of target method errors
     */
    @Around("execution(* org.yes.cart.service.domain.impl.CustomerServiceImpl.resetPassword(..))")
    public Object doResetPassword(final ProceedingJoinPoint pjp) throws Throwable {
        return notifyInternal(pjp, false);
    }


}
