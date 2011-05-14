package org.yes.cart.service.locator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yes.cart.service.locator.InstantiationStrategy;
import org.yes.cart.service.locator.ServiceLocator;

import java.text.MessageFormat;
import java.util.Map;

/**
 * Service locator use particular strategy, that depends from protocol in service url, to
 * instantiate serice. At thi moment tree strategies available - web service , jnp an spring local.
 *
 * User: Igor Azarny iazarny@yahoo.com
 * Date: 09-May-2011
 * Time: 14:12:54
 */
public class ServiceLocatorImpl implements ServiceLocator {


    private static final Logger LOG = LoggerFactory.getLogger(ServiceLocatorImpl.class);

    private final Map<String, InstantiationStrategy> protocolStrategyMap;


    /**
     * Construct the service locator.
     *
     * @param protocolStrategyMap strategy  map to instanciate service.
     */
    public ServiceLocatorImpl(Map<String, InstantiationStrategy> protocolStrategyMap) {
        this.protocolStrategyMap = protocolStrategyMap;
    }


    /**
     * Get {@link InstantiationStrategy} by given service url.
     *
     * @param serviceUrl given service url

     * @return {@link InstantiationStrategy} to create particular service instance.
     */
    InstantiationStrategy getInstantiationStrategy(final String serviceUrl) {
        final String strategyKey = getStrategyKey(serviceUrl);
        final InstantiationStrategy instantiationStrategy = protocolStrategyMap.get(strategyKey);
        if (instantiationStrategy == null) {
            throw new RuntimeException(
                    MessageFormat.format(
                            "Instantiation strategy can not be found for key {0} from url {1}",
                            strategyKey,
                            serviceUrl
                    )
            );
        }
        return instantiationStrategy;
    }

    /**
     * Get protocol from url. Possible values - http,https,jnp.
     * Null will be returned for spring.
     *
     * @param url given url
     * @return protocol
     */
    String getStrategyKey(final String url) {
        if (url.indexOf(':') > -1) {
            return url.substring(0, url.indexOf(':'));
        }
        return null;
    }


    /** {@inheritDoc} */
    public <T> T getServiceInstance(final String serviceUrl,
                                    final Class<T> iface,
                                    final String loginName,
                                    final String password) {

        if(LOG.isDebugEnabled()) {
            LOG.debug("Get " + serviceUrl + " as " + iface.getName());
        }

        try {
            return getInstantiationStrategy(serviceUrl).getInstance(serviceUrl, iface, loginName, password);
        } catch (Exception e) {
            throw new RuntimeException(
                    MessageFormat.format
                            ("Can not create {0} instance. Given interface is {1}. See root cause for more detail",
                                    serviceUrl, iface.getName()), e);
        }
    }

}