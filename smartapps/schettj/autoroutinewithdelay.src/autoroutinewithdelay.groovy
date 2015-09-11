/**
 *  AutoRoutineWithDelay
 *
 *  Copyright 2015 John schettino
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
		name: "AutoRoutineWithDelay",
		namespace: "schettj",
		author: "John schettino",
		description: "Run a routine when a door is unlocked with a specific code. Optionally, after a delay, if no one is present, run a different routine. So, for example, unlock with code 1 -> run I'm Back! (disarms SHM) but if after 2 minutes presence hasn't changed -> run Goodbye! (arms SHM, alarms fire) If you set up an emergency 911 unlock code then entering that could trigger Alert! Routine -> Silent alarm/notifications/SMS but SMH off.",
		category: "Safety & Security",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "selectDoorActions")
}

def selectDoorActions() {
	dynamicPage(name: "selectDoorActions", title: "Select Routine to run on unlock", install: true, uninstall: true) {

		// get the available actions
		def actions = location.helloHome?.getPhrases()*.label
		if (actions) {
			// sort them alphabetically
			actions.sort()
			section("When this Lock unlocked with this code") {
				input "lock", "capability.lock", title: "Lock", multiple: false
				input "code", "number", title: "Code"
				// use the actions as the options for an enum input
				input "routine", "enum", title: "Routine to run", options: actions
			}
			// optional stuff
			section("Optionally run second routine if no presence after delay") {
				input "doDelay", "bool", title: "Run second routine after delay", required: false
				input "delay", "number", title: "Minutes to delay", required: settings.doDelay
				input "routine2", "enum", title: "Routine to run", options: actions, required: settings.doDelay
				input "presdevs", "capability.presenceSensor", title: "If none of these present", multiple:true, required: settings.doDelay
			}
		}
	}
}

import groovy.json.JsonSlurper


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// subscribe to lock
	subscribe(lock, "lock", checkUnlockedWithCode)

}

def unlockTimeout() {
	// check presence for all presence devices, if non present, fire routine2 
	// TBD
}

def checkUnlockedWithCode(evt) {
	def ourCode = false;
	if (("unlocked" == evt.value)) {
		// code pulled from 
		// https://github.com/SANdood/Home-on-Code-Unlock-Too/blob/master/Home%20on%20Code%20Unlock%20Too.groovy
		def isManual = false

		// have to check text for "with code N"

		log.debug evt.descriptionText

		// get digit(s)
		def digitstring = evt.descriptionText.find(/(\d+)/)
		def ourcode = false;

		if (digitstring == null) {
			isManual = true
		}
		else {
			ourcode = (digitstring == settings.code.toString())
			log.debug ourcode.toString() + " " + digitstring + " " + settings.code
		}

		if (ourcode) {
			// run the routine 
			location.helloHome?.execute(settings.routine)
			if (settings.doDelay) {
				runIn(settings.delay == null ? 120 : settings.delay * 60, unlockTimeout)
			}
		}
	}
}
