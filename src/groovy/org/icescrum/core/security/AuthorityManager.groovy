/*
 * Copyright (c) 2015 Kagilum SAS
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
 * Vincent Barrier (vbarrier@kagilum.com)
 * Stéphane Maldini (stephane.maldini@icescrum.com)
 */

package org.icescrum.core.security

import org.icescrum.core.domain.User
import org.icescrum.core.domain.preferences.UserPreferences
import org.icescrum.core.domain.security.Authority
import org.icescrum.core.domain.security.UserAuthority

class AuthorityManager {

    static public makeAdmin = { user ->
        UserAuthority.create(user, Authority.findByAuthority(Authority.ROLE_ADMIN), false)
        UserAuthority.create(user, Authority.findByAuthority(Authority.ROLE_PERMISSION), true)
    }

    static public makeUnAdmin = { user ->
        UserAuthority.remove(user, Authority.findByAuthority(Authority.ROLE_ADMIN), false)
        UserAuthority.remove(user, Authority.findByAuthority(Authority.ROLE_PERMISSION), true)
    }

    static public initSecurity = { def grailsApplication ->
        def ctx = grailsApplication.mainContext
        def securityService = ctx.securityService
        ctx.webExpressionHandler?.securityService = securityService
        ctx.expressionHandler?.securityService = securityService
        def springSecurityService = ctx.springSecurityService
        if (Authority.count() == 0) {
            new Authority(authority: Authority.ROLE_ADMIN).save()
            new Authority(authority: Authority.ROLE_PERMISSION).save()
            if (grailsApplication.config.icescrum.createDefaultAdmin) {
                def admin = new User(username: 'admin',
                        email: 'admin@icescrum.com',
                        enabled: true,
                        firstName: "--",
                        lastName: "Admin",
                        password: springSecurityService.encodePassword('adminadmin!'),
                        preferences: new UserPreferences(language: "en")
                ).save(flush:true)
                makeAdmin(admin)
            }
        }
    }
}
