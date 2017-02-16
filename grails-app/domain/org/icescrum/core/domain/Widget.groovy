package org.icescrum.core.domain

import grails.converters.JSON
import org.icescrum.core.domain.preferences.UserPreferences

class Widget implements Serializable {

    static final long serialVersionUID = 813639045722976126L

    int position
    boolean onRight = false

    String settingsData
    String widgetDefinitionId
    String type
    Long typeId

    static belongsTo = [userPreferences: UserPreferences]

    static constraints = {
        settingsData nullable: true
        type nullable: true
        typeId nullable: true
    }

    static mapping = {
        cache true
        settingsData type: 'text'
        table 'is_up_widgets'
        userPreferences index: 'up_wdi_index'
        widgetDefinitionId index: 'up_wdi_index'
    }

    def beforeInsert() {
        position = !position ? Widget.countByOnRight(onRight) + 1 : position
    }

    static transients = ["settings"]

    public void setSettings(Map settings) {
        settingsData = settings ? settings as JSON : null
    }

    public Map getSettings() {
        settingsData ? JSON.parse(settingsData) as Map : [:]
    }

    def xml = { builder ->
        builder.widget() {
            builder.onRight(this.onRight)
            builder.position(this.position)
            builder.widgetDefinitionId(this.widgetDefinitionId)
            builder.settingsData { builder.mkp.yieldUnescaped("<![CDATA[${this.settingsData ?: ''}]]>") }
            exportDomainsPlugins(builder)
        }
    }
}
