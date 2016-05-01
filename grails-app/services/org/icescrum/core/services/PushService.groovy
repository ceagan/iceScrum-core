/*
 * Copyright (c) 2015 Kagilum SAS.
 *
 * This file is part of iceScrum.
 *
 * iceScrum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * iceScrum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with iceScrum.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Authors:
 *
 * Vincent Barrier (vbarrier@kagilum.com)
 * Nicolas Noullet (nnoullet@kagilum.com)
 */

package org.icescrum.core.services

import grails.converters.JSON
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceFactory
import org.atmosphere.cpr.Broadcaster
import org.atmosphere.cpr.HeaderConfig
import org.icescrum.atmosphere.IceScrumAtmosphereEventListener
import org.icescrum.atmosphere.IceScrumBroadcaster
import org.icescrum.core.domain.User
import org.icescrum.core.event.IceScrumEventType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder

@Transactional
class PushService {

    def atmosphereMeteor

    void broadcastToProductUsers(IceScrumEventType eventType, object, long productId) {
        def channel = '/stream/app/product-' + productId
        Broadcaster broadcaster = atmosphereMeteor.broadcasterFactory?.lookup(IceScrumBroadcaster.class, channel)
        if (broadcaster) {
            // toString() required to eagerly generate the String (lazy raise an error because no session in atmosphere thread)
            def message = ([eventType: eventType.name(), object: object] as JSON).toString()
            log.debug("broadcast to everybody on channel " + channel)
            broadcaster.broadcast(message)
        }
    }

    void broadcastToSingleUser(IceScrumEventType eventType, object, User user) {
        def channel = '/stream/app/*'
        Broadcaster broadcaster = atmosphereMeteor.broadcasterFactory?.lookup(IceScrumBroadcaster.class, channel)
        if (broadcaster) {
            Set<AtmosphereResource> resources = broadcaster.atmosphereResources?.findAll { AtmosphereResource resource ->
                resource.request?.getAttribute(IceScrumAtmosphereEventListener.USER_CONTEXT)?.username == user.username
            }
            if (resources) {
                log.debug('broadcast to ' + resources*.uuid().join(', ') + ' on channel ' + channel)
                broadcaster.broadcast(([eventType: eventType.name(), object: object] as JSON).toString(), resources)
            }
        }
    }

    void broadcastToUsers(IceScrumEventType eventType, object, Collection<User> user) {
        def usernames = user*.username
        AtmosphereResourceFactory atmosphereResourceFactory = atmosphereMeteor.framework?.atmosphereFactory()
        if (atmosphereResourceFactory) {
            def resources = atmosphereResourceFactory.findAll().findAll { AtmosphereResource resource ->
                resource.request?.getAttribute(IceScrumAtmosphereEventListener.USER_CONTEXT)?.username in usernames
            }
            log.debug('broadcast to ' + resources*.uuid().join(', ') + ' on one of their broadcasters')
            resources.each { AtmosphereResource resource ->
                Broadcaster broadcaster = resource.broadcasters().first()
                broadcaster.broadcast(([eventType: eventType.name(), object: object] as JSON).toString(), resource)
            }
        }
    }
}