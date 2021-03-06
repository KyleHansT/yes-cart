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

package org.yes.cart.web.application;

import org.apache.commons.lang.ObjectUtils;
import org.apache.wicket.IRequestCycleProvider;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.https.HttpsConfig;
import org.apache.wicket.protocol.https.HttpsMapper;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.cycle.RequestCycleContext;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.ClassProvider;
import org.apache.wicket.util.file.IResourceFinder;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.yes.cart.service.misc.LanguageService;
import org.yes.cart.util.ShopCodeContext;
import org.yes.cart.web.theme.WicketPagesMounter;
import org.yes.cart.web.theme.WicketResourceMounter;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 *
 * Main web application.
 *
 * In case if we yes-cart running without apache http server
 *
 *  1. Tomcat is responsible to offload ssl certificate
 *
 *  Main approach to work with https behind proxy is following:
 *
 *  1. Apache http server responsible to offload ssl
 *  2. Tomcat accept only ajp unsecured protocol.
 *
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 7/10/11
 * Time: 9:02 AM
 */
public class StorefrontApplication
        extends AuthenticatedWebApplication
        implements
        IResourceFinder,
        IRequestCycleProvider {

    private SpringComponentInjector springComponentInjector;

    private ClassProvider loginPageProvider;
    private ClassProvider homePageProvider;

    /**
     * Lazy getter of spring injector.
     *
     * @return spring inject support
     */
    public SpringComponentInjector getSpringComponentInjector() {
        if (springComponentInjector == null) {
            this.springComponentInjector = new SpringComponentInjector(this);
        }
        return springComponentInjector;
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    public Class<Page> getHomePage() {
        return homePageProvider.get();
    }



    /**
     * {@inheritDoc}
     */
    protected void init() {

        enableResourceAccess();

        super.init();

        // dynamic shop markup support via specific resource finder
        getResourceSettings().setResourceFinder(this);
        getResourceSettings().setResourceStreamLocator(new ResourceStreamLocator(this));

        setRequestCycleProvider(this);

        // wicket-groovy dynamic pages support
        //getApplicationSettings().setClassResolver(new GroovyClassResolver(this));

        configureMarkupSettings();

        getComponentInstantiationListeners().add(getSpringComponentInjector());

        getRequestCycleListeners().add(new StorefrontRequestCycleListener());

        mountPages();
        mountResources();

        if ("true".equalsIgnoreCase(getInitParameter("secureMode"))) {

            final HttpsConfig httpsConfig = new HttpsConfig(
                    Integer.valueOf((String) ObjectUtils.defaultIfNull(getInitParameter("unsecurePort"), "8080")),
                    Integer.valueOf((String) ObjectUtils.defaultIfNull(getInitParameter("securePort"), "8443"))
            );

            final HttpsMapper httpsMapper = new HttpsMapper(getRootRequestMapper(), httpsConfig);

            setRootRequestMapper(httpsMapper);

        }

    }

    /** {@inheritDoc} */
    @Override
    public Session newSession(Request request, Response response) {

        final ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        final LanguageService languageService = ctx.getBean(
                "languageService",
                LanguageService.class);

        return super.newSession(
                new StorefrontRequestDecorator(request, languageService.getSupportedLanguages(ShopCodeContext.getShopCode())),
                response);

    }

    /** {@inheritDoc} */
    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return StorefrontWebSession.class;
    }


    /**
     * {@inheritDoc}
     */
    protected Class<? extends WebPage> getSignInPageClass() {
        return loginPageProvider.get();
    }

    /**
     * Enable resources for application.
     */
    private void enableResourceAccess() {
        final ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        final WicketResourceMounter mounter = ctx.getBean(
                "wicketResourceMounter",
                WicketResourceMounter.class);

        mounter.enableResourceAccess(this);

    }

    /**
     * Configure markup settings
     */
    private void configureMarkupSettings() {

        // EhCache cache for markup
        final ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        final MultiMarkupFactory multiMarkupFactory = ctx.getBean(
                "wicketMultiMarkupFactory",
                MultiMarkupFactory.class);
        getMarkupSettings().setMarkupFactory(multiMarkupFactory);

        // Plain POJO with Concurrent hash map cache
        // getMarkupSettings().setMarkupFactory(new MultiMarkupFactory());

        getMarkupSettings().setCompressWhitespace(true);
        getMarkupSettings().setStripWicketTags(true); // true remove wicket:tags in development mode
        getMarkupSettings().setDefaultMarkupEncoding("UTF-8");
        getMarkupSettings().setDefaultBeforeDisabledLink("<a>");
        getMarkupSettings().setDefaultAfterDisabledLink("</a>");
        //getMarkupSettings().setAutomaticLinking(false);
    }

    /**
     * Mount resources to particular paths.
     */
    private void mountResources() {
        final ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        final WicketResourceMounter mounter = ctx.getBean(
                "wicketResourceMounter",
                WicketResourceMounter.class);

        mounter.mountResources(this);

    }

    /**
     * Mount pages to particular paths.
     */
    private void mountPages() {
        final ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        final WicketPagesMounter mounter = ctx.getBean(
                "wicketPagesMounter",
                WicketPagesMounter.class);

        mounter.mountPages(this);

        loginPageProvider = mounter.getLoginPageProvider();
        homePageProvider = mounter.getHomePageProvider();

        if (loginPageProvider == null) {
            ShopCodeContext.getLog(this).error("No login page class was mounted");
        }
        if (homePageProvider == null) {
            ShopCodeContext.getLog(this).error("No home page class was mounted");
        }

    }

    /**
     * {@inheritDoc}
     */
    public IResourceStream find(final Class<?> aClass, final String s) {
        return configureMultiWebApplicationPath().find(aClass, s);
    }

    /**
     * {@inheritDoc}
     */
    public RequestCycle get(final RequestCycleContext context) {
        return new RequestCycle(context);
    }


    /**
     * Get existing or create new {@link MultiWebApplicationPath} for new request
     *
     * @return instance of {@link MultiWebApplicationPath}
     */
    private MultiWebApplicationPath configureMultiWebApplicationPath() {

        HttpServletRequest rawRequest = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
        final Object resolver = rawRequest.getAttribute("YC_APP_MULTIWEBAPP_RESOLVER");
        if (resolver == null) {

            MultiWebApplicationPath multiWebApplicationPath = new MultiWebApplicationPath(getServletContext());

            final List<String> themesChain = ApplicationDirector.getCurrentThemeChain();
            for (final String theme : themesChain) {
                multiWebApplicationPath.add(theme + "/markup");  // shop specific markup folder
            }

            rawRequest.setAttribute("YC_APP_MULTIWEBAPP_RESOLVER", multiWebApplicationPath);
            return multiWebApplicationPath;
        }
        return (MultiWebApplicationPath) resolver;
    }

}
